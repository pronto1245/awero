package app.awero.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.awero.R
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.AlarmReadiness
import app.awero.core.alarm.AlarmScheduler

@Composable
fun HomeScreen(
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    statusRefreshKey: Int = 0
) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    var alarms by remember { mutableStateOf(emptyList<Alarm>()) }
    var readiness by remember { mutableStateOf<Map<String, AlarmReadiness>>(emptyMap()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { alarms = coordinator.all() }
    LaunchedEffect(alarms, refreshKey, statusRefreshKey) {
        val scheduler = AlarmScheduler(context)
        readiness = alarms.associate { it.id to scheduler.readiness(it) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("AWERO", style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text("Wake up. Stay up.", color = Color.White.copy(alpha = .6f))
        Spacer(Modifier.height(24.dp))

        if (alarms.isEmpty()) {
            Text("No alarms", color = Color.White.copy(alpha = .7f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(alarms, key = { it.id }) { alarm ->
                    val state = readiness[alarm.id]
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .08f))) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("%02d:%02d".format(alarm.hour, alarm.minute), color = Color.White, style = MaterialTheme.typography.headlineLarge)
                            Text(alarm.missionType.name, color = Color.White.copy(alpha = .7f))
                            if (alarm.enabled) {
                                Text(
                                    text = stringResource(
                                        when (state) {
                                            AlarmReadiness.SCHEDULED -> R.string.alarm_state_scheduled
                                            AlarmReadiness.PERMISSION_REQUIRED -> R.string.alarm_state_permission
                                            AlarmReadiness.INVALID -> R.string.alarm_state_invalid
                                            AlarmReadiness.NOT_SCHEDULED -> R.string.alarm_state_missing
                                            else -> R.string.alarm_state_checking
                                        }
                                    ),
                                    color = if (state == AlarmReadiness.SCHEDULED) Color(0xFF91E89A) else Color(0xFFFFC36B),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (state != AlarmReadiness.SCHEDULED) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = {
                                            scope.launch {
                                                coordinator.repair(forceReschedule = true)
                                                refreshKey++
                                            }
                                        }) { Text(stringResource(R.string.alarm_retry)) }
                                        if (state == AlarmReadiness.PERMISSION_REQUIRED) {
                                            TextButton(onClick = {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                )
                                            }) { Text(stringResource(R.string.alarm_open_settings)) }
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    scope.launch {
                                        try { coordinator.test(alarm) } catch (error: Exception) {
                                            actionError = error.message ?: "Could not start alarm test."
                                        }
                                    }
                                }) { Text("Test") }
                                TextButton(onClick = { onEditAlarm(alarm) }) { Text("Edit") }
                                TextButton(onClick = {
                                    scope.launch {
                                        coordinator.delete(alarm)
                                        alarms = coordinator.all()
                                    }
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
    if (actionError != null) {
        AlertDialog(onDismissRequest = { actionError = null }, title = { Text("Alarm action failed") },
            text = { Text(actionError.orEmpty()) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("OK") } })
    }
}
