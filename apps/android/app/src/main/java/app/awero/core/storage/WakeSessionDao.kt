package app.awero.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WakeSessionDao {
    @Query("SELECT * FROM wake_sessions WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): WakeSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WakeSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: WakeEventEntity)

    @Transaction
    suspend fun upsertWithEvent(entity: WakeSessionEntity, event: WakeEventEntity) {
        upsert(entity)
        insertEvent(event)
    }

    @Query("SELECT * FROM wake_sessions WHERE (:includeTest = 1 OR isTest = 0) ORDER BY scheduledAt DESC LIMIT 200")
    suspend fun recent(includeTest: Boolean = false): List<WakeSessionEntity>

    @Query("SELECT * FROM wake_sessions WHERE id = :id LIMIT 1")
    suspend fun get(id: String): WakeSessionEntity?

    @Query("SELECT * FROM wake_sessions WHERE result IS NULL ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun active(): WakeSessionEntity?

    @Query("SELECT * FROM wake_sessions WHERE alarmId = :alarmId AND isTest = :isTest AND result IS NULL ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun activeForAlarm(alarmId: String, isTest: Boolean): WakeSessionEntity?
}
