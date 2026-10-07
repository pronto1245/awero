package app.awero.core.alarm
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.awero.core.wake.WakeFlowController
class AlarmReceiver:BroadcastReceiver(){
 override fun onReceive(c:Context,i:Intent){
  val id=i.getStringExtra(AlarmScheduler.EXTRA_ID)?:return;val version=i.getIntExtra(AlarmScheduler.EXTRA_VERSION,-1);val at=i.getLongExtra(AlarmScheduler.EXTRA_AT,System.currentTimeMillis())
  val alarm=AlarmStore(c).get(id)?:return;if(alarm.version!=version||!alarm.enabled)return
  val flow=WakeFlowController();flow.begin(alarm,at)
  val wake=Intent(c,WakeAlarmActivity::class.java).apply{putExtra(AlarmScheduler.EXTRA_ID,id);putExtra(AlarmScheduler.EXTRA_VERSION,version);addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)}
  c.startActivity(wake)
 }
}