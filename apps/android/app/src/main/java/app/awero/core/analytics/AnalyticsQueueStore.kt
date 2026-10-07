package app.awero.core.analytics

import android.content.Context
import app.awero.core.storage.AnalyticsEventEntity
import app.awero.core.storage.AweroDatabase
import org.json.JSONObject
import java.util.UUID

class AnalyticsQueueStore(context: Context) {
    private val database = AweroDatabase.get(context.applicationContext)

    suspend fun enqueue(
        eventName: String,
        properties: JSONObject = JSONObject(),
        eventVersion: Int = 1,
        occurredAt: Long = System.currentTimeMillis()
    ) {
        database.analyticsEvents().insert(
            AnalyticsEventEntity(
                id = UUID.randomUUID().toString(),
                eventName = eventName,
                eventVersion = eventVersion,
                payload = properties.toString(),
                occurredAt = occurredAt
            )
        )
    }

    suspend fun pending() = database.analyticsEvents().pending()

    suspend fun acknowledge(id: String) {
        database.analyticsEvents().delete(id)
    }
}
