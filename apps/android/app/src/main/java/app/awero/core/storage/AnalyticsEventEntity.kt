package app.awero.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey val id: String,
    val eventName: String,
    val eventVersion: Int,
    val payload: String,
    val occurredAt: Long
)
