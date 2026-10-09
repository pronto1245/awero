package app.awero.core.storage

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val weekdays: String,
    val timezoneMode: String,
    val fixedTimezone: String?,
    val missionType: String,
    val difficulty: String,
    val maxSnoozes: Int,
    val snoozeMinutes: Int,
    val qrExpectedCode: String?,
    @ColumnInfo(defaultValue = "'Alarm'") val label: String
)
