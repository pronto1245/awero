package app.awero.core.sync

import android.content.Context
import app.awero.core.alarm.AlarmStore
import app.awero.core.analytics.AnalyticsQueueStore
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
        val analyticsQueue = AnalyticsQueueStore(context)
        val analyticsEvents = analyticsQueue.pending()
        if (analyticsEvents.isNotEmpty()) {
            runCatching {
                SyncApiClient(configuration).sendAnalytics(session.accessToken, analyticsEvents)
            }.onSuccess { response ->
                val completed = response.acceptedIds + response.rejected.map { it.id }
                completed.forEach { analyticsQueue.acknowledge(it) }
            }.onFailure { error ->
                if (error is SyncApiException && error.statusCode >= 500) {
                    SupportDiagnosticsApiClient(context, configuration).reportSyncServerFailure(
                        error.statusCode, session.accessToken, sessionStore.installationId()
                    )
                }
            }
        }
        val operations = queue.due(nowMillis)
        if (operations.isEmpty()) {
            sessionStore.loadSession()?.accessToken?.let { token ->
                pullServerState(token, configuration, database)
                WakeSessionSyncClient(context, configuration).uploadRecent(token, nowMillis)
            }
            return@withContext
        }

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
            val rejectionsById = response.rejected.associateBy { it.id }
            operations.forEach { operation ->
                when {
                    operation.id in accepted -> queue.acknowledge(operation.id)
                    conflictsById[operation.id] != null -> conflictsStore.record(operation, conflictsById.getValue(operation.id))
                    rejectionsById[operation.id] != null -> {
                        val rejected = rejectionsById.getValue(operation.id)
                        conflictsStore.record(
                            operation,
                            SyncConflict(rejected.id, rejected.code, null, null)
                        )
                        queue.acknowledge(operation.id)
                    }
                    else -> queue.retry(operation.id, operation.attempts + 1)
                }
            }
            sessionStore.loadSession()?.accessToken?.let { token ->
                pullServerState(token, configuration, database)
                WakeSessionSyncClient(context, configuration).uploadRecent(token, nowMillis)
            }
        } catch (error: Exception) {
            if (error is SyncApiException && error.statusCode >= 500) {
                sessionStore.loadSession()?.let { activeSession ->
                    SupportDiagnosticsApiClient(context, configuration).reportSyncServerFailure(
                        error.statusCode, activeSession.accessToken, sessionStore.installationId()
                    )
                }
            }
            operations.forEach { operation -> queue.retry(operation.id, operation.attempts + 1) }
        }
        }
    }

    private suspend fun pullServerState(
        bearerToken: String,
        configuration: SyncApiConfiguration,
        database: AweroDatabase
    ) {
        val conflicts = database.syncConflicts().all().mapTo(mutableSetOf()) { it.entityId }
        val snapshots = runCatching { SyncApiClient(configuration).fetchServerAlarms(bearerToken) }.getOrNull() ?: return
        val store = AlarmStore(context)
        for (snapshot in snapshots) {
            val alarm = snapshot.alarm
            if (alarm.id in conflicts) continue
            val local = database.alarms().get(alarm.id)
            if (snapshot.status == "DELETED") {
                if (local != null && local.version <= alarm.version) runCatching { store.delete(alarm.id) }
            } else if (local == null || local.version < alarm.version) {
                runCatching { store.save(alarm) }
            }
        }
    }

    private companion object {
        val syncMutex = Mutex()
    }
}
