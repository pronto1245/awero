package app.awero.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlarmEntity::class,
        WakeSessionEntity::class,
        WakeEventEntity::class,
        StatisticsEntity::class,
        SyncOperationEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AweroDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun wakeSessions(): WakeSessionDao
    abstract fun wakeEvents(): WakeEventDao
    abstract fun statistics(): StatisticsDao
    abstract fun syncOperations(): SyncOperationDao
    abstract fun analyticsEvents(): AnalyticsEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS wake_events (id TEXT NOT NULL, wakeSessionId TEXT NOT NULL, eventType TEXT NOT NULL, occurredAt INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(id))")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_wake_events_wakeSessionId ON wake_events(wakeSessionId)")
            }
        }

        @Volatile private var instance: AweroDatabase? = null

        fun get(context: Context): AweroDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AweroDatabase::class.java,
                    "awero.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}