package app.awero.core.sync

import app.awero.core.storage.AweroDatabase
import app.awero.core.storage.SyncConflictEntity
import app.awero.core.storage.SyncOperationEntity
import androidx.room.withTransaction

class SyncConflictStore(private val database: AweroDatabase) {
    suspend fun record(operation: SyncOperationEntity, conflict: SyncConflict) {
        database.withTransaction {
            database.syncConflicts().insert(
                SyncConflictEntity(
                    operationId = operation.id,
                    operationType = operation.operationType,
                    entityType = operation.entityType,
                    entityId = operation.entityId,
                    clientVersion = operation.clientVersion,
                    localPayload = operation.payload,
                    occurredAt = operation.occurredAt,
                    code = conflict.code,
                    serverVersion = conflict.serverVersion,
                    serverEntityJson = conflict.serverEntityJson,
                    detectedAt = System.currentTimeMillis()
                )
            )
            database.syncOperations().delete(operation.id)
        }
    }

    suspend fun all() = database.syncConflicts().all()

    suspend fun delete(operationId: String) = database.syncConflicts().delete(operationId)
}
