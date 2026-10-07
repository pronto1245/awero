package app.awero.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_events")
data class WakeEventEntity(
    @PrimaryKey val id: String,
    val wakeSessionId: String,
    val eventType: String,
    val occurredAt: Long,
    val payload: String = "{}"
)