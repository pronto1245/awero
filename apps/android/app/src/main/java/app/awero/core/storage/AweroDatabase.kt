package app.awero.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlarmEntity::class,
        WakeSessionEntity::class,
        StatisticsEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AweroDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun wakeSessions(): WakeSessionDao
    abstract fun statistics(): StatisticsDao
    abstract fun syncOperations(): SyncOperationDao

    companion object {
        @Volatile private var instance: AweroDatabase? = null

        fun get(context: Context): AweroDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AweroDatabase::class.java,
                    "awero.db"
                ).build().also { instance = it }
            }
    }
}
