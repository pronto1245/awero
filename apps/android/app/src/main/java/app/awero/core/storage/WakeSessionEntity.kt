package app.awero.core.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wake_sessions")
data class WakeSessionEntity(
    @PrimaryKey val id: String,
    val alarmId: String,
    val alarmVersion: Int,
    val scheduledAt: Long,
    val triggeredAt: Long?,
    val missionStartedAt: Long?,
    val completedAt: Long?,
    val result: String?,
    val snoozeCount: Int,
    val fallbackUsed: Boolean,
    val emergencyStop: Boolean,
    @ColumnInfo(defaultValue = "0") val isTest: Boolean
)
