package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.awero.R
import app.awero.core.alarm.MissionType
import androidx.compose.ui.unit.dp
import app.awero.core.wake.WakeFlowController
import kotlinx.coroutines.launch

@Composable
fun WakeScreen(flow: WakeFlowController) {
    val scope = rememberCoroutineScope()
    val state by flow.state.collectAsState()
    val mission by flow.mission.collectAsState()
    val snoozeError by flow.snoozeError.collectAsState()
    val actionError by flow.actionError.collectAsState()
    val currentAlarm by flow.currentAlarm.collectAsState()
    val context = LocalContext.current
    var pendingRetry by remember { mutableStateOf<MissionRetry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8EF))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", color = Color(0xFF14294B).copy(alpha = .45f))
        Text(stringResource(R.string.wake_title), style = MaterialTheme.typography.displayLarge, color = Color(0xFF14294B))
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when (state) {
            WakeFlowController.State.RINGING -> {
                Text(stringResource(R.string.wake_active), color = Color(0xFF14294B).copy(alpha = .65f))
                snoozeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { scope.launch { flow.beginMission() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF684B))
                ) {
                    Text(stringResource(R.string.wake_start))
                }
                TextButton(onClick = { scope.launch { flow.snooze() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF14294B))) {
                    Text(stringResource(R.string.wake_snooze))
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE74B4B))) {
                    Text(stringResource(R.string.wake_emergency_stop))
                }
            }

            WakeFlowController.State.MISSION -> {
                Text(stringResource(missionLabel(mission)), color = Color(0xFF14294B).copy(alpha = .7f))
                // The session can only be completed by solving the real mission; there is no
                // shortcut button. A failed save offers a retry of exactly the failed action.
                val alarm = currentAlarm
                val activity = context as? ComponentActivity
                if (alarm != null && activity != null && pendingRetry == null) {
                    key(alarm.id, mission) {
                        AndroidView(
                            factory = {
                                MissionRuntimeScreen.create(
                                    activity,
                                    alarm.copy(missionType = mission),
                                    onSuccess = {
                                        scope.launch {
                                            if (!flow.completeMission()) pendingRetry = MissionRetry.COMPLETE
                                        }
                                    },
                                    onFailure = {
                                        scope.launch {
                                            if (!flow.fallbackToMath()) pendingRetry = MissionRetry.FALLBACK
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        )
                    }
                }
                pendingRetry?.let { retry ->
                    Button(
                        onClick = {
                            scope.launch {
                                val saved = when (retry) {
                                    MissionRetry.COMPLETE -> flow.completeMission()
                                    MissionRetry.FALLBACK -> flow.fallbackToMath()
                                }
                                if (saved) pendingRetry = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0502F))
                    ) {
                        Text(stringResource(R.string.wake_retry))
                    }
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }) {
                    Text(stringResource(R.string.wake_emergency_stop))
                }
            }

            WakeFlowController.State.COMPLETED -> {
                Text(stringResource(R.string.wake_completed_title), color = Color(0xFF14294B))
                Text(stringResource(R.string.wake_completed_body), color = Color(0xFF14294B).copy(alpha = .6f))
            }

            WakeFlowController.State.EMERGENCY_STOPPED -> {
                Text(stringResource(R.string.wake_emergency_stop), color = Color(0xFF14294B))
                Text(stringResource(R.string.wake_stopped_body), color = Color(0xFF14294B).copy(alpha = .6f))
            }

            WakeFlowController.State.IDLE -> Unit
        }
    }
}

private enum class MissionRetry { COMPLETE, FALLBACK }

@Composable
private fun missionLabel(mission: MissionType): Int = when (mission) {
    MissionType.MATH -> R.string.home_mission_math
    MissionType.STEPS -> R.string.home_mission_steps
    MissionType.QR -> R.string.home_mission_qr
    MissionType.PHOTO -> R.string.home_mission_photo
    MissionType.MIXED -> R.string.home_mission_mixed
}
