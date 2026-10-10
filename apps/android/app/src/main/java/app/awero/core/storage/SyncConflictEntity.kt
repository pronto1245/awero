package app.awero.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_conflicts")
data class SyncConflictEntity(
    @PrimaryKey val operationId: String,
    val operationType: String,
    val entityType: String,
    val entityId: String,
    val clientVersion: Int?,
    val localPayload: String,
    val occurredAt: Long,
    val code: String,
    val serverVersion: Int?,
    val serverEntityJson: String?,
    val detectedAt: Long
)
