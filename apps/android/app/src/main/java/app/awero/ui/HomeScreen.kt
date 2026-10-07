package app.awero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator

@Composable
fun HomeScreen(
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    var alarms by remember { mutableStateOf(emptyList<Alarm>()) }\n    val scope = rememberCoroutineScope()\n\n    LaunchedEffect(Unit) {\n        alarms = coordinator.all()\n    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)
    ) {
        Text("AWERO", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text("Wake up. Stay up.", color = Color.White.copy(alpha = .6f))
        Spacer(Modifier.height(24.dp))

        if (alarms.isEmpty()) {
            Text("No alarms", color = Color.White.copy(alpha = .7f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .08f))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("%02d:%02d".format(alarm.hour, alarm.minute), color = Color.White, style = MaterialTheme.typography.headlineLarge)
                            Text(alarm.missionType.name, color = Color.White.copy(alpha = .7f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { coordinator.test(alarm) }) { Text("Test") }
                                TextButton(onClick = { onEditAlarm(alarm) }) { Text("Edit") }
                                TextButton(onClick = {
                                    scope.launch {\n                                        coordinator.delete(alarm)\n                                        alarms = coordinator.all()\n                                    }
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }

        Button(onClick = onCreateAlarm, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Create alarm")
        }
    }
}
