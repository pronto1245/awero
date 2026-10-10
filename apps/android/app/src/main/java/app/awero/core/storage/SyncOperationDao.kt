package app.awero.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations WHERE nextAttemptAt <= :now ORDER BY occurredAt LIMIT 100")
    suspend fun due(now: Long): List<SyncOperationEntity>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sync_operations SET attempts = attempts + 1, nextAttemptAt = :nextAttemptAt WHERE id = :id")
    suspend fun retry(id: String, nextAttemptAt: Long)
}
