package app.awero.core.alarm
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone
class AlarmScheduler(private val context:Context){
 private val manager=context.getSystemService(AlarmManager::class.java)
 fun schedule(a:Alarm){cancel(a);if(!a.enabled)return;a.weekdays.forEach{day->
  val at=next(a,day);val i=Intent(context,AlarmReceiver::class.java).apply{action=ACTION_ALARM;putExtra(EXTRA_ID,a.id);putExtra(EXTRA_VERSION,a.version);putExtra(EXTRA_AT,at.timeInMillis)}
  val p=PendingIntent.getBroadcast(context,code(a,day),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  if(manager.canScheduleExactAlarms())manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.timeInMillis,p) else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at.timeInMillis,p)
 }}
 fun scheduleTest(a:Alarm,seconds:Long=30){val at=System.currentTimeMillis()+seconds*1000;val i=Intent(context,AlarmReceiver::class.java).apply{action=ACTION_TEST;putExtra(EXTRA_ID,a.id);putExtra(EXTRA_VERSION,a.version);putExtra(EXTRA_AT,at)};val p=PendingIntent.getBroadcast(context,a.id.hashCode() xor 0x55AA,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);if(manager.canScheduleExactAlarms())manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p) else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p)}
 fun cancel(a:Alarm){(1..7).forEach{d->val i=Intent(context,AlarmReceiver::class.java).apply{action=ACTION_ALARM};val p=PendingIntent.getBroadcast(context,code(a,d),i,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE);if(p!=null)manager.cancel(p)}}
 fun isScheduled(a:Alarm)=a.weekdays.all{d->val i=Intent(context,AlarmReceiver::class.java).apply{action=ACTION_ALARM};PendingIntent.getBroadcast(context,AlarmReceiver::class.java.hashCode()+code(a,d),i,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)!=null}
 private fun next(a:Alarm,d:Int):Calendar{val tz=if(a.timezoneMode==TimezoneMode.FIXED&&a.fixedTimezone!=null)TimeZone.getTimeZone(a.fixedTimezone)else TimeZone.getDefault();val now=Calendar.getInstance(tz);val t=Calendar.getInstance(tz).apply{set(Calendar.HOUR_OF_DAY,a.hour);set(Calendar.MINUTE,a.minute);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);set(Calendar.DAY_OF_WEEK,d)};if(t.timeInMillis<=now.timeInMillis)t.add(Calendar.WEEK_OF_YEAR,1);return t}
 private fun code(a:Alarm,d:Int)=a.id.hashCode()*31+a.version*7+d
 companion object{const val ACTION_ALARM="app.awero.ALARM";const val ACTION_TEST="app.awero.TEST_ALARM";const val EXTRA_ID="alarm_id";const val EXTRA_VERSION="alarm_version";const val EXTRA_AT="scheduled_at"}
}