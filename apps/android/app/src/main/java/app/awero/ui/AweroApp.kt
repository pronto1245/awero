package app.awero.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun AweroApp() {
    var screen by rememberSaveable { mutableStateOf("home") }
    MaterialTheme {
        when (screen) {
            "create" -> CreateAlarmScreen(onSaved = { screen = "home" })
            else -> HomeScreen(onCreateAlarm = { screen = "create" })
        }
    }
}
