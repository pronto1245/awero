package app.awero.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.awero.R
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.AlarmReadiness
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.AlarmTimeFormatter
import app.awero.core.alarm.MissionType
import app.awero.core.alarm.nextAlarmOccurrence
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.time.Instant
import java.util.Date
import java.util.TimeZone

private val AweroIvory = Color(0xFFFFF8EF)
private val AweroNavy = Color(0xFF14294B)
private val AweroCoral = Color(0xFFFF684B)
private val AweroSage = Color(0xFF4F8B66)

private fun alarmTimeZoneId(alarm: Alarm): String? =
    if (alarm.timezoneMode == app.awero.core.alarm.TimezoneMode.FIXED) alarm.fixedTimezone else null

@Composable
fun HomeScreen(
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onOpenSettings: () -> Unit,
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

    val now = remember(alarms, refreshKey) { Instant.now() }
    val nextAlarm = remember(alarms, refreshKey) {
        alarms.asSequence()
            .filter { it.enabled }
            .flatMap { alarm ->
                alarm.weekdays.asSequence().mapNotNull { day ->
                    runCatching { alarm to nextAlarmOccurrence(alarm, day, now) }.getOrNull()
                }
            }
            .minByOrNull { it.second.toInstant() }
    }
    val nextAlarmDescription = nextAlarm?.let {
        DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).apply {
            timeZone = TimeZone.getTimeZone(it.second.zone)
        }.format(Date.from(it.second.toInstant()))
    }
    val nextAlarmTime = nextAlarm?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT).apply {
            timeZone = TimeZone.getTimeZone(it.second.zone)
        }.format(Date.from(it.second.toInstant()))
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AweroIvory).padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AWERO",
                style = MaterialTheme.typography.headlineMedium,
                color = AweroNavy,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = context.getString(R.string.settings_title) }
            ) {
                Text("⚙", color = AweroNavy, style = MaterialTheme.typography.titleLarge)
            }
        }
        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineLarge, color = AweroNavy)
        Text(stringResource(R.string.home_subtitle), color = AweroNavy.copy(alpha = .65f))
        Spacer(Modifier.height(18.dp))

        if (nextAlarm != null && nextAlarmDescription != null && nextAlarmTime != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFE6C6), Color(0xFFFFFDF7))))
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("☀  " + stringResource(R.string.home_next_alarm), color = AweroNavy)
                        Text(nextAlarm.first.label, color = AweroNavy.copy(alpha = .72f))
                        Text(
                            text = nextAlarmTime,
                            style = MaterialTheme.typography.displaySmall,
                            color = AweroNavy
                        )
                        Text(nextAlarmDescription, color = AweroNavy.copy(alpha = .7f))
                        Text(missionLabel(nextAlarm.first.missionType), color = AweroNavy.copy(alpha = .8f))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        Text(stringResource(R.string.home_alarms), style = MaterialTheme.typography.titleLarge, color = AweroNavy)
        Spacer(Modifier.height(10.dp))

        if (alarms.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleLarge, color = AweroNavy)
                    Text(stringResource(R.string.home_empty_body), color = AweroNavy.copy(alpha = .7f))
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    val state = readiness[alarm.id]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        AlarmTimeFormatter.format(alarm.hour, alarm.minute, alarmTimeZoneId(alarm)),
                                        color = AweroNavy,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    Text(alarm.label, color = AweroNavy.copy(alpha = .78f), style = MaterialTheme.typography.titleMedium)
                                    Text(weekdaySummary(alarm), color = AweroNavy.copy(alpha = .55f))
                                    Text(missionLabel(alarm.missionType), color = AweroNavy.copy(alpha = .65f))
                                }
                                Switch(
                                    checked = alarm.enabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            try {
                                                coordinator.update(alarm.copy(enabled = enabled))
                                                alarms = coordinator.all()
                                            } catch (error: Exception) {
                                                actionError = error.localizedMessage
                                                    ?: context.getString(R.string.home_error_title)
                                            }
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = context.getString(
                                            R.string.home_toggle_alarm,
                                            "%02d:%02d".format(alarm.hour, alarm.minute)
                                        )
                                    }
                                )
                            }
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
                                    color = if (state == AlarmReadiness.SCHEDULED) AweroSage else AweroCoral,
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
                            } else {
                                Text(stringResource(R.string.home_disabled), color = AweroNavy.copy(alpha = .5f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = {
                                    scope.launch {
                                        try { coordinator.test(alarm) } catch (error: Exception) {
                                            actionError = error.localizedMessage ?: context.getString(R.string.home_error_title)
                                        }
                                    }
                                }) { Text(stringResource(R.string.home_test)) }
                                TextButton(onClick = { onEditAlarm(alarm) }) { Text(stringResource(R.string.home_edit)) }
                                TextButton(onClick = {
                                    scope.launch {
                                        coordinator.delete(alarm)
                                        alarms = coordinator.all()
                                    }
                                }) { Text(stringResource(R.string.home_delete), color = AweroCoral) }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCreateAlarm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AweroCoral),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(stringResource(R.string.home_add_alarm), color = Color.White)
        }
        Spacer(Modifier.height(10.dp))
    }

    if (actionError != null) {
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text(stringResource(R.string.home_error_title)) },
            text = { Text(actionError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { actionError = null }) {
                    Text(stringResource(R.string.home_ok))
                }
            }
        )
    }
}

@Composable
private fun missionLabel(mission: MissionType): String = stringResource(
    when (mission) {
        MissionType.MATH -> R.string.home_mission_math
        MissionType.STEPS -> R.string.home_mission_steps
        MissionType.QR -> R.string.home_mission_qr
        MissionType.PHOTO -> R.string.home_mission_photo
        MissionType.MIXED -> R.string.home_mission_mixed
    }
)

private fun weekdaySummary(alarm: Alarm): String {
    val labels = DateFormatSymbols.getInstance().shortWeekdays
    return alarm.weekdays.sorted().mapNotNull { day ->
        labels.getOrNull(day)?.takeIf(String::isNotBlank)
    }.joinToString(" · ")
}
