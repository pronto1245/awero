package app.awero.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import java.util.Calendar

@Composable
fun CreateAlarmScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    val now = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    var mission by remember { mutableStateOf(MissionType.MATH) }
    var difficulty by remember { mutableStateOf(Difficulty.MEDIUM) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("New alarm", color = Color.White, style = MaterialTheme.typography.headlineMedium)
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
            coordinator.create(hour, minute, mission, difficulty)
            onSaved()
        }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Create alarm")
        }
    }
}
