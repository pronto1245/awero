package app.awero.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import java.util.Calendar

@Composable
fun CreateAlarmScreen(alarm: app.awero.core.alarm.Alarm? = null, onSaved: () -> Unit) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    val scope = rememberCoroutineScope()
    val now = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(alarm?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: now.get(Calendar.MINUTE)) }
    var mission by remember { mutableStateOf(alarm?.missionType ?: MissionType.MATH) }
    var difficulty by remember { mutableStateOf(alarm?.difficulty ?: Difficulty.MEDIUM) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(if (alarm == null) "New alarm" else "Edit alarm", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineLarge)
        }
        Text("Mission", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MissionType.entries.filter { it != MissionType.PHOTO && it != MissionType.MIXED }.forEach { type ->
                FilterChip(selected = mission == type, onClick = { mission = type }, label = { Text(type.name) })
            }
        }
        Text("Difficulty", color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { d ->
                FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.name) })
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            scope.launch {
                try {
                    if (alarm == null) coordinator.create(hour, minute, mission, difficulty)
                    else coordinator.update(alarm.copy(hour = hour, minute = minute, missionType = mission, difficulty = difficulty))
                    onSaved()
                } catch (error: Exception) {
                    saveError = error.message ?: "Could not schedule this alarm."
                }
            }
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (alarm == null) "Create alarm" else "Save changes")
        }
    }
    if (saveError != null) {
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text("Alarm was not scheduled") },
            text = { Text(saveError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { saveError = null }) { Text("OK") }
            }
        )
    }
}
