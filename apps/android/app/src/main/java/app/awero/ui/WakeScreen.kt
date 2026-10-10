package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AweroDesign.ivory)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", color = AweroDesign.navy.copy(alpha = .45f))
        Text(stringResource(R.string.wake_title), style = MaterialTheme.typography.displayLarge, color = AweroDesign.navy)
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when (state) {
            WakeFlowController.State.RINGING -> {
                Text(stringResource(R.string.wake_active), color = AweroDesign.navy.copy(alpha = .65f))
                snoozeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { scope.launch { flow.beginMission() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AweroDesign.coral)
                ) {
                    Text(stringResource(R.string.wake_start))
                }
                TextButton(onClick = { scope.launch { flow.snooze() } }, colors = ButtonDefaults.textButtonColors(contentColor = AweroDesign.navy)) {
                    Text(stringResource(R.string.wake_snooze))
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE74B4B))) {
                    Text(stringResource(R.string.wake_emergency_stop))
                }
            }

            WakeFlowController.State.MISSION -> {
                Text(stringResource(missionLabel(mission)), color = AweroDesign.navy.copy(alpha = .7f))
                Button(
                    onClick = { scope.launch { flow.completeMission() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AweroDesign.coral)
                ) {
                    Text(stringResource(R.string.wake_complete))
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }) {
                    Text(stringResource(R.string.wake_emergency_stop))
                }
            }

            WakeFlowController.State.COMPLETED -> {
                Text(stringResource(R.string.wake_completed_title), color = AweroDesign.navy)
                Text(stringResource(R.string.wake_completed_body), color = AweroDesign.navy.copy(alpha = .6f))
            }

            WakeFlowController.State.EMERGENCY_STOPPED -> {
                Text(stringResource(R.string.wake_emergency_stop), color = AweroDesign.navy)
                Text(stringResource(R.string.wake_stopped_body), color = AweroDesign.navy.copy(alpha = .6f))
            }

            WakeFlowController.State.IDLE -> Unit
        }
    }
}

@Composable
private fun missionLabel(mission: MissionType): Int = when (mission) {
    MissionType.MATH -> R.string.home_mission_math
    MissionType.STEPS -> R.string.home_mission_steps
    MissionType.QR -> R.string.home_mission_qr
    MissionType.PHOTO -> R.string.home_mission_photo
    MissionType.MIXED -> R.string.home_mission_mixed
}
