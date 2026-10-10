package app.awero.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AnalyticsEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AnalyticsEventEntity)

    @Query("SELECT * FROM analytics_events ORDER BY occurredAt LIMIT 100")
    suspend fun pending(): List<AnalyticsEventEntity>

    @Query("DELETE FROM analytics_events WHERE id = :id")
    suspend fun delete(id: String)
}
