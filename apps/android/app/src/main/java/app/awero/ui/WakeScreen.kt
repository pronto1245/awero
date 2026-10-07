package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.awero.core.missions.MissionType
import app.awero.core.wake.WakeFlowController

@Composable
fun WakeScreen(flow: WakeFlowController) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", color = Color.White.copy(alpha = .45f))
        Text("GET UP", style = MaterialTheme.typography.displayLarge, color = Color.White)

        when (flow.state) {
            WakeFlowController.State.RINGING -> {
                Text("The alarm is active.", color = Color.White.copy(alpha = .6f))
                Button(onClick = flow::beginMission, modifier = Modifier.fillMaxWidth()) {
                    Text("Start mission")
                }
                TextButton(onClick = { flow.snooze() }) { Text("Snooze") }
                TextButton(onClick = flow::emergencyStop) { Text("Emergency stop") }
            }
            WakeFlowController.State.MISSION -> {
                Text(flow.mission.name, color = Color.White.copy(alpha = .7f))
                Button(onClick = flow::completeMission, modifier = Modifier.fillMaxWidth()) {
                    Text("Complete mission")
                }
            }
            WakeFlowController.State.COMPLETED -> {
                Text("YOU'RE UP", color = Color.White)
                Text("Wake session completed.", color = Color.White.copy(alpha = .6f))
            }
            WakeFlowController.State.EMERGENCY_STOPPED -> {
                Text("Emergency stop", color = Color.White)
                Text("The session was recorded.", color = Color.White.copy(alpha = .6f))
            }
            WakeFlowController.State.IDLE -> {}
        }
    }
}
