package app.awero.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import app.awero.core.alarm.Alarm
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore

@Composable
fun AweroApp(statusRefreshKey: Int = 0) {
    val context = LocalContext.current
    val flow = remember { WakeFlowController(WakeSessionStore(context), context) }
    val state by flow.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf("home") }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(Unit) {
        flow.restore()
        if (flow.state.value != WakeFlowController.State.IDLE) {
            screen = "wake"
        }
    }

    MaterialTheme {
        when (screen) {
            "create" -> CreateAlarmScreen(
                onSaved = { editingAlarm = null; screen = "home" }
            )
            "edit" -> CreateAlarmScreen(
                alarm = editingAlarm,
                onSaved = { editingAlarm = null; screen = "home" }
            )
            "wake" -> WakeScreen(flow)
            else -> {
                if (state == WakeFlowController.State.RINGING ||
                    state == WakeFlowController.State.MISSION ||
                    state == WakeFlowController.State.COMPLETED ||
                    state == WakeFlowController.State.EMERGENCY_STOPPED
                ) {
                    LaunchedEffect(state) { screen = "wake" }
                }
                HomeScreen(
                    onCreateAlarm = {
                        editingAlarm = null
                        screen = "create"
                    },
                    onEditAlarm = {
                        editingAlarm = it
                        screen = "edit"
                    },
                    statusRefreshKey = statusRefreshKey
                )
            }
        }
    }
}
