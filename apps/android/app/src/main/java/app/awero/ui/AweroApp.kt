package app.awero.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun AweroApp() {
    MaterialTheme {
        HomeScreen(onCreateAlarm = {})
    }
}
