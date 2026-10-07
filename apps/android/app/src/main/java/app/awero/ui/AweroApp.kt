package app.awero.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import app.awero.core.alarm.Alarm

@Composable
fun AweroApp() {
    var screen by rememberSaveable { mutableStateOf("home") }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    MaterialTheme {
        when (screen) {
            "create" -> CreateAlarmScreen(
                onSaved = { editingAlarm = null; screen = "home" }
            )
            "edit" -> CreateAlarmScreen(
                alarm = editingAlarm,
                onSaved = { editingAlarm = null; screen = "home" }
            )
            else -> HomeScreen(
                onCreateAlarm = {
                    editingAlarm = null
                    screen = "create"
                },
                onEditAlarm = {
                    editingAlarm = it
                    screen = "edit"
                }
            )
        }
    }
}
