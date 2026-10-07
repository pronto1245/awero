package app.awero.core.alarm
import android.app.Activity
import android.os.Bundle
import app.awero.core.wake.WakeFlowController
class WakeAlarmActivity:Activity(){
 private lateinit var flow:WakeFlowController
 override fun onCreate(state:Bundle?){super.onCreate(state);val id=intent.getStringExtra(AlarmScheduler.EXTRA_ID)?:return finish();val v=intent.getIntExtra(AlarmScheduler.EXTRA_VERSION,-1);val a=AlarmStore(this).get(id)?:return finish();if(a.version!=v)return finish();flow=WakeFlowController();flow.begin(a,System.currentTimeMillis())}
}