package app.awero.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: SyncConflictEntity)

    @Query("SELECT * FROM sync_conflicts ORDER BY detectedAt")
    suspend fun all(): List<SyncConflictEntity>

    @Query("SELECT * FROM sync_conflicts WHERE operationId = :operationId LIMIT 1")
    suspend fun get(operationId: String): SyncConflictEntity?

    @Query("DELETE FROM sync_conflicts WHERE operationId = :operationId")
    suspend fun delete(operationId: String)
}
