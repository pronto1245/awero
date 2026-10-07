package app.awero.core.alarm
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class AlarmRecoveryReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){if(i.action !in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))return;val s=AlarmStore(c);val q=AlarmScheduler(c);s.all().forEach{if(it.enabled)q.schedule(it)else q.cancel(it)}}
}