package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.awero.core.wake.WakeFlowController

@Composable
fun WakeScreen(flow: WakeFlowController) {
    val scope = rememberCoroutineScope()

    val state by flow.state.collectAsState()\n    val mission by flow.mission.collectAsState()\n\n    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", color = Color.White.copy(alpha = .45f))
        Text("GET UP", style = MaterialTheme.typography.displayLarge, color = Color.White)

        when (state) {
            WakeFlowController.State.RINGING -> {
                Text("The alarm is active.", color = Color.White.copy(alpha = .6f))
                Button(onClick = { scope.launch { flow.beginMission() } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start mission")
                }
                TextButton(onClick = { scope.launch { flow.snooze() } }) { Text("Snooze") }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }) { Text("Emergency stop") }
            }
            WakeFlowController.State.MISSION -> {
                Text(mission.name, color = Color.White.copy(alpha = .7f))
                Button(onClick = { scope.launch { flow.completeMission() } }, modifier = Modifier.fillMaxWidth()) {
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
