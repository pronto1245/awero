package app.awero.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WakeEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: WakeEventEntity)

    @Query("SELECT * FROM wake_events WHERE wakeSessionId = :sessionId ORDER BY occurredAt")
    suspend fun forSession(sessionId: String): List<WakeEventEntity>
}