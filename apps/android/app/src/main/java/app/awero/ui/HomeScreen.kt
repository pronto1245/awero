package app.awero.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.paint
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

private val AweroIvory get() = AweroDesign.ivory
private val AweroNavy get() = AweroDesign.navy
private val AweroCoral get() = AweroDesign.coralStrong
private val AweroSage get() = AweroDesign.sage

private enum class AlarmLoadState { Loading, Loaded, Failed }

private fun alarmTimeZoneId(alarm: Alarm): String? =
    if (alarm.timezoneMode == app.awero.core.alarm.TimezoneMode.FIXED) alarm.fixedTimezone else null

@Composable
fun HomeScreen(
    onCreateAlarm: () -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onOpenSettings: () -> Unit,
    statusRefreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    var alarms by remember { mutableStateOf(emptyList<Alarm>()) }
    var alarmLoadState by remember { mutableStateOf(AlarmLoadState.Loading) }
    var readiness by remember { mutableStateOf<Map<String, AlarmReadiness>>(emptyMap()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<Alarm?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        alarmLoadState = AlarmLoadState.Loading
        try {
            alarms = coordinator.all()
            alarmLoadState = AlarmLoadState.Loaded
        } catch (_: Exception) {
            alarmLoadState = AlarmLoadState.Failed
        }
    }
    LaunchedEffect(alarms, refreshKey, statusRefreshKey) {
        val scheduler = AlarmScheduler(context)
        readiness = alarms.associate { it.id to scheduler.readiness(it) }
    }

    val now = remember(alarms, refreshKey) { Instant.now() }
    val nextAlarm = remember(alarms, refreshKey, alarmLoadState) {
        if (alarmLoadState != AlarmLoadState.Loaded) return@remember null
        alarms.asSequence()
            .filter { it.enabled }
            .flatMap { alarm ->
                alarm.weekdays.asSequence().mapNotNull { day ->
                    runCatching { alarm to nextAlarmOccurrence(alarm, day, now) }.getOrNull()
                }
            }
            .minByOrNull { it.second.toInstant() }
    }
    val todayLabel = stringResource(R.string.home_today)
    val tomorrowLabel = stringResource(R.string.home_tomorrow)
    // "Завтра, Пн" / "Сегодня, Пт" for the next two days, otherwise "Ср, 15 окт.".
    val nextAlarmDescription = nextAlarm?.let { (_, at) ->
        val locale = java.util.Locale.getDefault()
        val today = java.time.LocalDate.now(at.zone)
        val weekday = at.format(java.time.format.DateTimeFormatter.ofPattern("EEE", locale))
        when (at.toLocalDate()) {
            today -> "$todayLabel, $weekday"
            today.plusDays(1) -> "$tomorrowLabel, $weekday"
            else -> at.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        }
    }
    val nextAlarmTime = nextAlarm?.let {
        DateFormat.getTimeInstance(DateFormat.SHORT).apply {
            timeZone = TimeZone.getTimeZone(it.second.zone)
        }.format(Date.from(it.second.toInstant()))
    }

    Column(
        modifier = modifier.fillMaxSize().background(AweroIvory)
            // Full-screen illustrated scene behind the page; it fades to ivory at the bottom.
            .paint(painterResource(AweroScene.page(AweroScene.current()).portrait), contentScale = ContentScale.Crop)
            .padding(horizontal = 20.dp)
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AWERO",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
                color = AweroNavy,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = context.getString(R.string.settings_title) }
            ) {
                AweroNavigationIcon("settings")
            }
        }
        Text(
            stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = AweroNavy
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(R.string.home_subtitle), color = AweroNavy.copy(alpha = .68f))
            AweroNavigationIcon("sun", tint = AweroDesign.sun)
        }
        // Space for the scene's sun and peaks; the next-alarm card overlaps them.
        Spacer(Modifier.height(150.dp))
        Spacer(Modifier.height(18.dp))

        if (alarmLoadState == AlarmLoadState.Loaded && nextAlarm != null && nextAlarmDescription != null && nextAlarmTime != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).offset(y = (-28).dp)
                    .clickable { onEditAlarm(nextAlarm.first) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                    .background(AweroDesign.surface)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                        .fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AweroNavigationIcon("sun", tint = AweroDesign.sun)
                            Text(stringResource(R.string.home_next_alarm), color = AweroNavy, style = MaterialTheme.typography.titleSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(text = nextAlarmTime, style = MaterialTheme.typography.displaySmall, color = AweroNavy)
                                Text(
                                    "$nextAlarmDescription · ${missionLabel(nextAlarm.first.missionType)}",
                                    color = AweroDesign.textSecondary,
                                    maxLines = 1
                                )
                            }
                            Text("›", color = AweroCoral, style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        if (alarmLoadState == AlarmLoadState.Loaded && alarms.isNotEmpty() && nextAlarm == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AweroDesign.surfaceWarm),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.home_no_enabled_title), color = AweroNavy, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.home_no_enabled_body), color = AweroNavy.copy(alpha = .68f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text(stringResource(R.string.home_alarms), style = MaterialTheme.typography.titleLarge, color = AweroNavy)
        Spacer(Modifier.height(10.dp))

        if (alarmLoadState == AlarmLoadState.Loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AweroDesign.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AweroCoral)
                }
            }
        } else if (alarmLoadState == AlarmLoadState.Failed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AweroDesign.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.home_alarms_load_error_title), style = MaterialTheme.typography.titleLarge, color = AweroNavy)
                    Text(stringResource(R.string.home_alarms_load_error_body), color = AweroNavy.copy(alpha = .7f))
                    TextButton(onClick = { refreshKey++ }) {
                        Text(stringResource(R.string.home_retry_loading_alarms), color = AweroCoral)
                    }
                }
            }
        } else if (alarms.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AweroDesign.surface),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                alarms.forEach { alarm ->
                    AlarmRow(
                        alarm = alarm,
                        state = readiness[alarm.id],
                        onToggle = { enabled ->
                            scope.launch {
                                try {
                                    coordinator.update(alarm.copy(enabled = enabled))
                                    alarms = coordinator.all()
                                } catch (error: Exception) {
                                    actionError = context.getString(R.string.home_error_body)
                                }
                            }
                        },
                        onEdit = { onEditAlarm(alarm) },
                        onTest = {
                            scope.launch {
                                try { coordinator.test(alarm) } catch (error: Exception) {
                                    actionError = context.getString(R.string.home_error_body)
                                }
                            }
                        },
                        onDelete = { deleteCandidate = alarm },
                        onRetry = {
                            scope.launch {
                                coordinator.repair(forceReschedule = true)
                                refreshKey++
                            }
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }
            }
        }

        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCreateAlarm,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AweroCoral),
            shape = RoundedCornerShape(AweroDesign.controlCorner),
            enabled = alarmLoadState == AlarmLoadState.Loaded
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
    deleteCandidate?.let { alarm ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.home_delete_confirm_title)) },
            text = { Text(stringResource(R.string.home_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    scope.launch {
                        try {
                            coordinator.delete(alarm)
                            alarms = coordinator.all()
                        } catch (error: Exception) {
                            actionError = context.getString(R.string.home_error_body)
                        }
                    }
                }) { Text(stringResource(R.string.home_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(stringResource(R.string.create_cancel))
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

/** "Будни", "Выходные", "Каждый день", or short weekday names starting from the locale's first day. */
@Composable
private fun weekdaySummary(alarm: Alarm): String {
    val days = alarm.weekdays
    return when {
        days.size == 7 -> stringResource(R.string.home_days_everyday)
        days == setOf(2, 3, 4, 5, 6) -> stringResource(R.string.home_days_weekdays)
        days == setOf(1, 7) -> stringResource(R.string.home_days_weekends)
        else -> {
            val labels = DateFormatSymbols.getInstance().shortWeekdays
            val first = java.util.Calendar.getInstance().firstDayOfWeek
            (0 until 7).map { (first - 1 + it) % 7 + 1 }.filter { it in days }
                .mapNotNull { labels.getOrNull(it)?.takeIf(String::isNotBlank) }
                .joinToString(", ")
        }
    }
}

/**
 * One alarm on Home, as in the reference screen: mission icon, time, "days · mission" and the
 * switch. Tapping edits; a long press offers edit, test and delete. A problem with scheduling
 * shows as a chip with a fix action; a healthy alarm shows nothing extra.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AlarmRow(
    alarm: Alarm,
    state: AlarmReadiness?,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val time = AlarmTimeFormatter.format(alarm.hour, alarm.minute, alarmTimeZoneId(alarm))
    val editLabel = stringResource(R.string.home_edit)
    val testLabel = stringResource(R.string.home_test)
    val deleteLabel = stringResource(R.string.home_delete)
    Card(
        colors = CardDefaults.cardColors(containerColor = AweroDesign.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth()
                    .combinedClickable(onClickLabel = editLabel, onClick = onEdit, onLongClick = { menuOpen = true })
                    .semantics {
                        customActions = listOf(
                            androidx.compose.ui.semantics.CustomAccessibilityAction(testLabel) { onTest(); true },
                            androidx.compose.ui.semantics.CustomAccessibilityAction(deleteLabel) { onDelete(); true }
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.alpha(if (alarm.enabled) 1f else .6f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                        when (alarm.missionType) {
                            MissionType.STEPS -> MissionChoiceIcon("steps", Modifier.size(22.dp), tint = AweroDesign.textSecondary)
                            MissionType.QR -> MissionChoiceIcon("qr", Modifier.size(22.dp), tint = AweroDesign.textSecondary)
                            else -> AweroNavigationIcon("sun", tint = AweroDesign.sun)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            time,
                            color = AweroNavy,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                        Text(
                            "${weekdaySummary(alarm)} · ${missionLabel(alarm.missionType)}",
                            color = AweroDesign.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    Switch(
                        checked = alarm.enabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AweroDesign.coral,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = AweroDesign.chip,
                            uncheckedBorderColor = AweroDesign.chip
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.home_toggle_alarm, time)
                        }
                    )
                }
                val problem = alarm.enabled && (state == AlarmReadiness.PERMISSION_REQUIRED ||
                    state == AlarmReadiness.NOT_SCHEDULED || state == AlarmReadiness.INVALID)
                if (problem) {
                    Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(AweroDesign.border))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(
                                if (state == AlarmReadiness.PERMISSION_REQUIRED) R.string.home_status_permission
                                else R.string.home_status_not_scheduled
                            ),
                            color = AweroDesign.warning,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.background(AweroDesign.warningSoft, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = when (state) {
                            AlarmReadiness.PERMISSION_REQUIRED -> onOpenSettings
                            AlarmReadiness.INVALID -> onEdit
                            else -> onRetry
                        }) {
                            Text(
                                stringResource(R.string.home_fix),
                                color = AweroDesign.coralStrong,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(editLabel) }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text(testLabel) }, onClick = { menuOpen = false; onTest() })
                DropdownMenuItem(text = { Text(deleteLabel, color = AweroDesign.warning) }, onClick = { menuOpen = false; onDelete() })
            }
        }
    }
}
