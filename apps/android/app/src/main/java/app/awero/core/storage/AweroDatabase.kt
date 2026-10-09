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
        SyncConflictEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AweroDatabase : RoomDatabase() {
    abstract fun alarms(): AlarmDao
    abstract fun wakeSessions(): WakeSessionDao
    abstract fun wakeEvents(): WakeEventDao
    abstract fun statistics(): StatisticsDao
    abstract fun syncOperations(): SyncOperationDao
    abstract fun syncConflicts(): SyncConflictDao
    abstract fun analyticsEvents(): AnalyticsEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS wake_events (id TEXT NOT NULL, wakeSessionId TEXT NOT NULL, eventType TEXT NOT NULL, occurredAt INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(id))"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_wake_events_wakeSessionId ON wake_events(wakeSessionId)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS sync_conflicts (operationId TEXT NOT NULL, operationType TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, clientVersion INTEGER, localPayload TEXT NOT NULL, occurredAt INTEGER NOT NULL, code TEXT NOT NULL, serverVersion INTEGER, serverEntityJson TEXT, detectedAt INTEGER NOT NULL, PRIMARY KEY(operationId))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE alarms ADD COLUMN label TEXT NOT NULL DEFAULT 'Alarm'")
            }
        }

        @Volatile
        private var instance: AweroDatabase? = null

        internal fun closeForTesting() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }

        fun get(context: Context): AweroDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AweroDatabase::class.java,
                    "awero.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }

        internal fun createForTesting(context: Context, name: String): AweroDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AweroDatabase::class.java,
                name
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }
}
