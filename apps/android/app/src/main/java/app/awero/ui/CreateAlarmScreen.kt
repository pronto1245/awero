package app.awero.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import android.text.format.DateFormat
import java.text.DateFormatSymbols
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.awero.R
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.alarm.TimezoneMode
import app.awero.core.missions.QRMissionRuntime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val FormIvory = AweroDesign.ivory
private val FormNavy = AweroDesign.navy
private val FormCoral = AweroDesign.coral

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CreateAlarmScreen(alarm: Alarm? = null, onSaved: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    val scope = rememberCoroutineScope()
    val now = Calendar.getInstance()
    var hour by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.minute ?: 0) }
    var mission by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.missionType ?: MissionType.MATH) }
    var difficulty by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.difficulty ?: Difficulty.MEDIUM) }
    var weekdays by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.weekdays ?: (2..6).toSet()) }
    var followsDeviceTimezone by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.timezoneMode != TimezoneMode.FIXED) }
    var fixedTimezone by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.fixedTimezone ?: TimeZone.getDefault().id) }
    var qrExpectedCode by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.qrExpectedCode.orEmpty()) }
    var showCodeScanner by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveNeedsSettings by remember { mutableStateOf(false) }
    val owner = context as? ComponentActivity
    val compactLayout = LocalConfiguration.current.fontScale >= 1.35f
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCodeScanner = true
        else scannerError = context.getString(R.string.permission_camera_denied)
    }

    BackHandler(enabled = true) { onCancel() }

    Column(
        modifier = Modifier.fillMaxSize().background(FormIvory).imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .90f))
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = FormNavy)
            ) {
                Text(stringResource(R.string.create_cancel), maxLines = 1)
            }
            Text(
                stringResource(if (alarm == null) R.string.create_title else R.string.create_edit_title),
                color = FormNavy,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                maxLines = 1,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 100.dp)
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.create_time), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                Surface(
                    onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, DateFormat.is24HourFormat(context)).show() },
                    modifier = Modifier.fillMaxWidth().height(154.dp),
                    shape = RoundedCornerShape(AweroDesign.cardCorner),
                    color = AweroDesign.surfaceWarm.copy(alpha = .62f)
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        listOf(hour, minute).forEachIndexed { index, value ->
                            if (index == 1) Text(":", color = FormNavy, style = MaterialTheme.typography.headlineMedium)
                            Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val previous = if (index == 0) (value + 23) % 24 else (value + 59) % 60
                                val next = if (index == 0) (value + 1) % 24 else (value + 1) % 60
                                Text(String.format(Locale.ROOT, "%02d", previous), color = FormNavy.copy(alpha = .36f), style = MaterialTheme.typography.titleMedium)
                                Box(
                                    Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 3.dp)
                                        .clip(RoundedCornerShape(AweroDesign.cardCorner)).background(Color.White.copy(alpha = .60f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(String.format(Locale.ROOT, "%02d", value), color = FormNavy, style = MaterialTheme.typography.headlineMedium)
                                }
                                Text(String.format(Locale.ROOT, "%02d", next), color = FormNavy.copy(alpha = .36f), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.create_repeat), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                Surface(shape = RoundedCornerShape(AweroDesign.cardCorner), color = Color.White) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
                        listOf(2, 3, 4, 5, 6, 7, 1).forEach { day ->
                            val selected = day in weekdays
                            val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (selected) FormCoral else AweroDesign.surfaceWarm)
                                    .toggleable(
                                        value = selected,
                                        role = androidx.compose.ui.semantics.Role.Checkbox,
                                        onValueChange = { weekdays = if (selected) weekdays - day else weekdays + day }
                                    )
                                    .semantics { contentDescription = symbols.weekdays[day] },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(symbols.shortWeekdays[day].take(2), color = if (selected) Color.White else FormNavy.copy(alpha = .68f), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                if (weekdays.isEmpty()) Text(stringResource(R.string.create_no_weekdays), color = FormCoral)
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.create_mission), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                Surface(shape = RoundedCornerShape(AweroDesign.cardCorner), color = Color.White) {
                    if (compactLayout) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(MissionType.MATH, MissionType.STEPS, MissionType.QR).forEach { type ->
                                MissionOptionCard(type, mission == type, Modifier.fillMaxWidth(), onClick = { mission = type })
                            }
                        }
                    } else {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(MissionType.MATH, MissionType.STEPS, MissionType.QR).forEach { type ->
                                MissionOptionCard(type, mission == type, Modifier.weight(1f), onClick = { mission = type })
                            }
                        }
                    }
                }
                if (mission == MissionType.QR) {
                    Text(stringResource(R.string.permission_camera_body), color = FormNavy.copy(alpha = .7f))
                    OutlinedButton(
                        onClick = {
                            scannerError = null
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                showCodeScanner = true
                            } else requestCamera.launch(Manifest.permission.CAMERA)
                        },
                        enabled = owner != null
                    ) { Text(stringResource(R.string.create_scan)) }
                    OutlinedTextField(
                        value = qrExpectedCode,
                        onValueChange = { qrExpectedCode = it },
                        label = { Text(stringResource(R.string.create_qr_content)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(stringResource(R.string.create_qr_instructions), color = FormNavy.copy(alpha = .7f))
                    scannerError?.let { Text(it, color = FormCoral) }
                }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.create_timezone), color = FormNavy, style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = followsDeviceTimezone,
                    onClick = { followsDeviceTimezone = true },
                    label = { Text(stringResource(R.string.create_timezone_device)) }
                )
                FilterChip(
                    selected = !followsDeviceTimezone,
                    onClick = { followsDeviceTimezone = false },
                    label = { Text(stringResource(R.string.create_timezone_fixed)) }
                )
            }
            Text(
                text = if (followsDeviceTimezone) stringResource(R.string.create_timezone_device_hint)
                else stringResource(R.string.create_timezone_fixed_hint, fixedTimezone),
                color = FormNavy.copy(alpha = .7f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEFDB)), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.permission_alarm_body), color = FormNavy, modifier = Modifier.padding(16.dp))
        }

        }
        Button(
            onClick = {
                scope.launch {
                    try {
                        val code = qrExpectedCode.ifEmpty { null }
                        val timezoneMode = if (followsDeviceTimezone) TimezoneMode.DEVICE_LOCAL else TimezoneMode.FIXED
                        val selectedFixedTimezone = if (followsDeviceTimezone) null else fixedTimezone
                        if (alarm == null) coordinator.create(
                            hour, minute, mission, difficulty, code, weekdays, timezoneMode, selectedFixedTimezone
                        )
                        else coordinator.update(alarm.copy(
                            hour = hour, minute = minute, weekdays = weekdays,
                            timezoneMode = timezoneMode, fixedTimezone = selectedFixedTimezone,
                            missionType = mission, difficulty = difficulty, qrExpectedCode = code
                        ))
                        onSaved()
                    } catch (error: Exception) {
                        Log.e("AWERO.Alarm", "Could not save alarm", error)
                        saveNeedsSettings = error is IllegalStateException
                        saveError = context.getString(R.string.create_error_body)
                    }
                }
            },
            enabled = weekdays.isNotEmpty() && (mission != MissionType.QR || qrExpectedCode.isNotBlank()),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormCoral),
            shape = RoundedCornerShape(AweroDesign.controlCorner)
        ) {
            Text(stringResource(if (alarm == null) R.string.create_save else R.string.create_save_changes), color = Color.White)
        }
    }

    if (showCodeScanner && owner != null) {
        val runtime = remember { QRMissionRuntime(context) }
        DisposableEffect(runtime) { onDispose { runtime.close() } }
        AlertDialog(
            onDismissRequest = { showCodeScanner = false },
            title = { Text(stringResource(R.string.create_scan)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.create_point_camera))
                    AndroidView(
                        factory = { cameraContext ->
                            PreviewView(cameraContext).also { preview ->
                                runtime.start(owner, preview) { code ->
                                    if (code.isNotBlank()) {
                                        qrExpectedCode = code
                                        scannerError = null
                                    } else scannerError = context.getString(R.string.permission_camera_unavailable)
                                    showCodeScanner = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodeScanner = false }) { Text(stringResource(R.string.create_cancel)) }
            }
        )
    }
    if (saveError != null) {
        AlertDialog(
            onDismissRequest = { saveError = null; saveNeedsSettings = false },
            title = { Text(stringResource(R.string.create_error_title)) },
            text = { Text(saveError.orEmpty()) },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (saveNeedsSettings) {
                        TextButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            })
                            saveError = null
                            saveNeedsSettings = false
                        }) { Text(stringResource(R.string.permission_open_settings)) }
                    }
                    TextButton(onClick = { saveError = null; saveNeedsSettings = false }) {
                        Text(stringResource(R.string.home_ok))
                    }
                }
            }
        )
    }
}

@Composable
private fun missionLabel(type: MissionType): String = stringResource(
    when (type) {
        MissionType.MATH -> R.string.home_mission_math
        MissionType.STEPS -> R.string.home_mission_steps
        MissionType.QR -> R.string.home_mission_qr
        MissionType.PHOTO -> R.string.home_mission_photo
        MissionType.MIXED -> R.string.home_mission_mixed
    }
)

private fun weekdayResource(day: Int): Int = when (day) {
    Calendar.SUNDAY -> R.string.day_sunday
    Calendar.MONDAY -> R.string.day_monday
    Calendar.TUESDAY -> R.string.day_tuesday
    Calendar.WEDNESDAY -> R.string.day_wednesday
    Calendar.THURSDAY -> R.string.day_thursday
    Calendar.FRIDAY -> R.string.day_friday
    else -> R.string.day_saturday
}

@Composable
private fun MissionOptionCard(type: MissionType, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val title = missionLabel(type)
    val description = stringResource(when (type) {
        MissionType.MATH -> R.string.create_mission_math_body
        MissionType.STEPS -> R.string.create_mission_steps_body
        else -> R.string.create_mission_qr_body
    })
    Column(
        modifier = modifier
            .heightIn(min = 142.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFFFFEEE4) else Color(0xFFFFF8EF))
            .border(if (selected) 1.5.dp else 1.dp, if (selected) FormCoral else FormNavy.copy(alpha = .10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MissionChoiceIcon(when (type) {
            MissionType.MATH -> "math"
            MissionType.STEPS -> "steps"
            else -> "qr"
        }, tint = if (selected) FormCoral else FormNavy)
        Text(title, color = if (selected) FormCoral else FormNavy, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Text(description, color = FormNavy.copy(alpha = .65f), style = MaterialTheme.typography.labelSmall, maxLines = 3)
    }
}
