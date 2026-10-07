package app.awero.core.alarm
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
class AlarmStore(context:Context){
 private val p=context.getSharedPreferences("awero_alarms",Context.MODE_PRIVATE)
 fun save(a:Alarm){p.edit().putString("alarm:"+a.id,toJson(a).toString()).putStringSet("ids",(ids()+a.id).toSet()).apply()}
 fun get(id:String)=p.getString("alarm:"+id,null)?.let{fromJson(JSONObject(it))}
 fun all()=ids().mapNotNull(::get)
 fun delete(id:String){p.edit().remove("alarm:"+id).putStringSet("ids",ids().filterNot{it==id}.toSet()).apply()}
 private fun ids()=p.getStringSet("ids",emptySet())?:emptySet()
 private fun toJson(a:Alarm)=JSONObject().apply{put("id",a.id);put("version",a.version);put("hour",a.hour);put("minute",a.minute);put("enabled",a.enabled);put("weekdays",JSONArray(a.weekdays.toList()));put("timezoneMode",a.timezoneMode.name);put("fixedTimezone",a.fixedTimezone?:JSONObject.NULL);put("missionType",a.missionType.name);put("difficulty",a.difficulty.name);put("maxSnoozes",a.maxSnoozes);put("snoozeMinutes",a.snoozeMinutes)}
 private fun fromJson(o:JSONObject):Alarm{val d=buildSet{val x=o.getJSONArray("weekdays");for(i in 0 until x.length())add(x.getInt(i))};return Alarm(o.getString("id"),o.getInt("version"),o.getInt("hour"),o.getInt("minute"),o.getBoolean("enabled"),d,TimezoneMode.valueOf(o.getString("timezoneMode")),o.optString("fixedTimezone",null),MissionType.valueOf(o.getString("missionType")),Difficulty.valueOf(o.getString("difficulty")),o.optInt("maxSnoozes",3),o.optInt("snoozeMinutes",10))}
}