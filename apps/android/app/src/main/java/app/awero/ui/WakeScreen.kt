package app.awero.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.awero.R
import app.awero.core.wake.WakeFlowController
import kotlinx.coroutines.launch

@Composable
fun WakeScreen(flow: WakeFlowController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val state by flow.state.collectAsState()
    val mission by flow.mission.collectAsState()
    val alarm by flow.currentAlarm.collectAsState()
    val snoozeError by flow.snoozeError.collectAsState()
    val actionError by flow.actionError.collectAsState()
    val activity = context as? ComponentActivity
    val active = state == WakeFlowController.State.RINGING || state == WakeFlowController.State.MISSION

    Box(Modifier.fillMaxSize().background(AweroDesign.ivory)) {
        WakeSceneArtwork(
            modifier = Modifier.align(Alignment.TopCenter),
            height = 390.dp
        )

        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (state == WakeFlowController.State.MISSION) 8.dp else 14.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AWERO",
                        modifier = Modifier.weight(1f),
                        color = AweroDesign.navy,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                    )
                    if (active) {
                        TextButton(
                            onClick = { scope.launch { flow.emergencyStop() } },
                            modifier = Modifier.heightIn(min = 44.dp).semantics {
                                contentDescription = context.getString(R.string.wake_emergency_stop)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = AweroDesign.navy)
                        ) {
                            Text("×", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                when (state) {
                    WakeFlowController.State.RINGING -> {
                        Text(
                            stringResource(R.string.wake_heading),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                            color = AweroDesign.navy
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AweroDesign.cardCorner),
                            color = Color.White.copy(alpha = .94f)
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.wake_instruction), color = AweroDesign.navy, style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(R.string.wake_active), color = AweroDesign.navy.copy(alpha = .68f))
                                actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                snoozeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                Button(
                                    onClick = { scope.launch { flow.beginMission() } },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AweroDesign.coral),
                                    shape = RoundedCornerShape(AweroDesign.controlCorner)
                                ) { Text(stringResource(R.string.wake_start), color = Color.White) }
                                TextButton(
                                    onClick = { scope.launch { flow.snooze() } },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = AweroDesign.navy)
                                ) { Text(stringResource(R.string.wake_snooze)) }
                            }
                        }
                    }

                    WakeFlowController.State.MISSION -> {
                        actionError?.let {
                            Surface(color = Color.White.copy(alpha = .94f), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(it, color = AweroDesign.navy)
                                    Button(onClick = { flow.clearActionError() }) {
                                        Text(stringResource(R.string.wake_retry))
                                    }
                                }
                            }
                        }
                        val current = alarm
                        if (current != null && activity != null) {
                            key(mission, actionError) {
                                AndroidView(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 430.dp),
                                    factory = { _ ->
                                        MissionRuntimeScreen.create(
                                            activity,
                                            current.copy(missionType = mission),
                                            onSuccess = { scope.launch { flow.completeMission() } },
                                            onFailure = { scope.launch { flow.fallbackToMath() } }
                                        )
                                    },
                                    update = {}
                                )
                            }
                        }
                    }

                    WakeFlowController.State.COMPLETED -> terminalState(
                        R.string.wake_completed_title,
                        R.string.wake_completed_body,
                        AweroDesign.sage
                    )

                    WakeFlowController.State.EMERGENCY_STOPPED -> terminalState(
                        R.string.wake_emergency_stop,
                        R.string.wake_stopped_body,
                        AweroDesign.coral
                    )

                    WakeFlowController.State.IDLE -> terminalState(
                        R.string.wake_snoozed_title,
                        R.string.wake_snoozed_body,
                        AweroDesign.navy
                    )
                }
                Spacer(Modifier.height(if (state == WakeFlowController.State.MISSION) 0.dp else 10.dp))
            }

            if (active) {
                Button(
                    onClick = { scope.launch { flow.emergencyStop() } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (state == WakeFlowController.State.MISSION) 4.dp else 8.dp).heightIn(min = 62.dp)
                        .semantics { contentDescription = context.getString(R.string.wake_emergency_stop) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE8E2)),
                    shape = RoundedCornerShape(AweroDesign.cardCorner)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AweroNavigationIcon("warning", tint = Color(0xFFD3313D))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.wake_emergency_stop), color = Color(0xFFD3313D), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.wake_emergency_stop_hint), color = AweroDesign.navy.copy(alpha = .58f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun terminalState(title: Int, body: Int, iconColor: Color) {
    Surface(color = Color.White.copy(alpha = .94f), shape = RoundedCornerShape(AweroDesign.cardCorner)) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("✓", color = iconColor, style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(title), color = AweroDesign.navy, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(body), color = AweroDesign.navy.copy(alpha = .68f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
