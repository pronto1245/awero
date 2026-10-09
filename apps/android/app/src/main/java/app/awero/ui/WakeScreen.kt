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
            .background(Color.Black)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AWERO", color = Color.White.copy(alpha = .45f))
        Text("GET UP", style = MaterialTheme.typography.displayLarge, color = Color.White)
        actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when (state) {
            WakeFlowController.State.RINGING -> {
                Text("The alarm is active.", color = Color.White.copy(alpha = .6f))
                snoozeError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { scope.launch { flow.beginMission() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF684B))
                ) {
                    Text("Start mission")
                }
                TextButton(onClick = { scope.launch { flow.snooze() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF14294B))) {
                    Text("Snooze")
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE74B4B))) {
                    Text("Emergency stop")
                }
            }

            WakeFlowController.State.MISSION -> {
                Text(mission.name, color = Color.White.copy(alpha = .7f))
                Button(
                    onClick = { scope.launch { flow.completeMission() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF684B))
                ) {
                    Text("Complete mission")
                }
                TextButton(onClick = { scope.launch { flow.emergencyStop() } }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE74B4B))) {
                    Text("Emergency stop")
                }
            }

            WakeFlowController.State.COMPLETED -> {
                Text("YOU'RE UP", color = Color.White)
                Text("Wake session completed.", color = Color.White.copy(alpha = .6f))
            }

            WakeFlowController.State.EMERGENCY_STOPPED -> {
                Text(stringResource(R.string.wake_emergency_stop), color = Color(0xFF14294B))
                Text("The session was recorded.", color = Color.White.copy(alpha = .6f))
            }

            WakeFlowController.State.IDLE -> Unit
        }
    }
}

