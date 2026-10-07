package app.awero.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey val id: Int = 1,
    val planned: Int = 0,
    val completed: Int = 0,
    val snoozes: Int = 0,
    val fallback: Int = 0,
    val emergencyStops: Int = 0,
    val totalCompletionSeconds: Long = 0
)
