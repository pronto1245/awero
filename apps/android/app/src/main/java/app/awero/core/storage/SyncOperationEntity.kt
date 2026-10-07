package app.awero.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val operationType: String,
    val entityType: String,
    val entityId: String,
    val clientVersion: Int?,
    val payload: String,
    val occurredAt: Long,
    val attempts: Int = 0,
    val nextAttemptAt: Long = 0
)
