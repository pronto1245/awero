package app.awero.core.sync

import android.content.Context
import app.awero.core.storage.AweroDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class OfflineSyncCoordinator(private val context: Context) {
    suspend fun runOnce(nowMillis: Long = System.currentTimeMillis()) = syncMutex.withLock {
        withContext(Dispatchers.IO) {
        val configuration = SyncApiConfiguration.from(context)
        if (!configuration.isEnabled) return@withContext

        val sessionStore = AnonymousAuthSessionStore(context)
        val auth = AnonymousAuthCoordinator(context)
        auth.refreshIfNeeded(nowMillis)
        val session = sessionStore.loadSession()
            ?.takeIf { it.expiresAtEpochMillis > nowMillis }
            ?: return@withContext
        val queue = SyncQueueStore(context, AweroDatabase.get(context.applicationContext))
        val database = AweroDatabase.get(context.applicationContext)
        val conflictsStore = SyncConflictStore(database)
        val operations = queue.due(nowMillis)
        if (operations.isEmpty()) return@withContext

        try {
            val response = try {
                SyncApiClient(configuration).sync(session.accessToken, operations)
            } catch (error: SyncApiException) {
                if (error.statusCode != 401) throw error
                sessionStore.clearSession()
                auth.refreshIfNeeded(nowMillis)
                val refreshed = sessionStore.loadSession()
                    ?.takeIf { it.expiresAtEpochMillis > nowMillis }
                    ?: throw error
                SyncApiClient(configuration).sync(refreshed.accessToken, operations)
            }
            val accepted = response.acceptedIds
            val conflictsById = response.conflicts.associateBy { it.id }
            operations.forEach { operation ->
                when {
                    operation.id in accepted -> queue.acknowledge(operation.id)
                    conflictsById[operation.id] != null -> conflictsStore.record(operation, conflictsById.getValue(operation.id))
                    else -> queue.retry(operation.id, operation.attempts + 1)
                }
            }
        } catch (_: Exception) {
            operations.forEach { operation -> queue.retry(operation.id, operation.attempts + 1) }
        }
        }
    }

    private companion object {
        val syncMutex = Mutex()
    }
}
