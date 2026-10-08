package app.awero.core.sync

import android.content.Context
import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.SyncOperationEntity
import org.json.JSONObject
import java.util.UUID

class SyncQueueStore(context: Context, private val database: AweroDatabase = AweroDatabase.get(context.applicationContext)) {

    suspend fun enqueue(
        operationType: String,
        entityType: String,
        entityId: String,
        payload: JSONObject,
        clientVersion: Int? = null,
        occurredAt: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString()
    ): String {
        database.syncOperations().insert(
            SyncOperationEntity(
                id = id,
                operationType = operationType,
                entityType = entityType,
                entityId = entityId,
                clientVersion = clientVersion,
                payload = payload.toString(),
                occurredAt = occurredAt
            )
        )
        return id
    }

    suspend fun due(now: Long = System.currentTimeMillis()) =
        database.syncOperations().due(now)

    suspend fun acknowledge(id: String) {
        database.syncOperations().delete(id)
    }

    suspend fun retry(id: String, attempts: Int) {
        val delay = (1L shl attempts.coerceIn(0, 6)) * 1000L
        database.syncOperations().retry(id, System.currentTimeMillis() + delay)
    }
}
