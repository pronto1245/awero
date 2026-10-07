package app.awero.core.wake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import app.awero.core.alarm.Alarm
import java.util.UUID

class WakeSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("awero_wake_sessions", Context.MODE_PRIVATE)

    private var active: WakeSession? = null

    fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()): WakeSession {
        val session = WakeSession(
            id = UUID.randomUUID().toString(),
            alarmId = alarm.id,
            alarmVersion = alarm.version,
            scheduledAt = scheduledAt
        )
        active = session
        save(session)
        return session
    }

    fun startMission() {
        active?.let {
            it.triggeredAt = it.triggeredAt ?: System.currentTimeMillis()
            it.missionStartedAt = System.currentTimeMillis()
            save(it)
        }
    }

    fun markFallback() {
        active?.let { it.fallbackUsed = true; save(it) }
    }

    fun complete(): WakeSession? {
        active?.let {
            it.completedAt = System.currentTimeMillis()
            it.result = "SUCCESS"
            save(it)
            active = null
            return it
        }
        return null
    }

    fun setSnoozeCount(count: Int) {
        active?.let { it.snoozeCount = count; save(it) }
    }

    fun emergencyStop(): WakeSession? {
        active?.let {
            it.emergencyStop = true
            it.result = "EMERGENCY_STOP"
            it.completedAt = System.currentTimeMillis()
            save(it)
            active = null
            return it
        }
        return null
    }

    fun save(session: WakeSession) {
        val current = load().toMutableList()
        val index = current.indexOfFirst { it.id == session.id }
        if (index >= 0) current[index] = session else current.add(session)
        val array = JSONArray()
        current.takeLast(200).forEach { array.put(toJson(it)) }
        prefs.edit().putString("sessions", array.toString()).apply()
    }

    fun load(): List<WakeSession> {
        val raw = prefs.getString("sessions", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) add(fromJson(array.getJSONObject(i)))
            }
        }.getOrDefault(emptyList())
    }

    private fun toJson(s: WakeSession): JSONObject = JSONObject().apply {
        put("id", s.id); put("alarmId", s.alarmId); put("alarmVersion", s.alarmVersion)
        put("scheduledAt", s.scheduledAt); put("triggeredAt", s.triggeredAt ?: JSONObject.NULL)
        put("missionStartedAt", s.missionStartedAt ?: JSONObject.NULL)
        put("completedAt", s.completedAt ?: JSONObject.NULL); put("result", s.result ?: JSONObject.NULL)
        put("snoozeCount", s.snoozeCount); put("fallbackUsed", s.fallbackUsed); put("emergencyStop", s.emergencyStop)
    }

    private fun fromJson(o: JSONObject) = WakeSession(
        id=o.getString("id"), alarmId=o.getString("alarmId"), alarmVersion=o.getInt("alarmVersion"),
        scheduledAt=o.getLong("scheduledAt"), triggeredAt=o.optLongOrNull("triggeredAt"),
        missionStartedAt=o.optLongOrNull("missionStartedAt"), completedAt=o.optLongOrNull("completedAt"),
        result=o.optStringOrNull("result"), snoozeCount=o.optInt("snoozeCount",0),
        fallbackUsed=o.optBoolean("fallbackUsed",false), emergencyStop=o.optBoolean("emergencyStop",false)
    )
    private fun JSONObject.optLongOrNull(key:String):Long?=if(isNull(key)) null else optLong(key)
    private fun JSONObject.optStringOrNull(key:String):String?=if(isNull(key)) null else optString(key)
}
