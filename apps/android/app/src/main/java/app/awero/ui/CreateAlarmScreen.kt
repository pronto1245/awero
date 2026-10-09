package app.awero.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.awero.R
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.missions.QRMissionRuntime
import java.util.Calendar

private val FormIvory = Color(0xFFFFF8EF)
private val FormNavy = Color(0xFF14294B)
private val FormCoral = Color(0xFFFF684B)

@Composable
fun CreateAlarmScreen(alarm: Alarm? = null, onSaved: () -> Unit) {
    val context = LocalContext.current
    val coordinator = remember { AlarmCoordinator(context) }
    val scope = rememberCoroutineScope()
    val now = Calendar.getInstance()
    var hour by remember { mutableIntStateOf(alarm?.hour ?: now.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(alarm?.minute ?: now.get(Calendar.MINUTE)) }
    var mission by remember { mutableStateOf(alarm?.missionType ?: MissionType.MATH) }
    var difficulty by remember { mutableStateOf(alarm?.difficulty ?: Difficulty.MEDIUM) }
    var weekdays by remember { mutableStateOf(alarm?.weekdays ?: (1..7).toSet()) }
    var qrExpectedCode by remember { mutableStateOf(alarm?.qrExpectedCode.orEmpty()) }
    var showCodeScanner by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveNeedsSettings by remember { mutableStateOf(false) }
    val owner = context as? ComponentActivity
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCodeScanner = true
        else scannerError = context.getString(R.string.permission_camera_denied)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(FormIvory).imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(if (alarm == null) R.string.create_title else R.string.create_edit_title),
            color = FormNavy,
            style = MaterialTheme.typography.headlineMedium
        )
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.create_time), color = FormNavy)
                OutlinedButton(
                    onClick = { TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.headlineLarge, color = FormNavy)
                }
                Text(stringResource(R.string.create_repeat), color = FormNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 3, 4, 5, 6, 7, 1).forEach { day ->
                        FilterChip(
                            selected = day in weekdays,
                            onClick = { weekdays = if (day in weekdays) weekdays - day else weekdays + day },
                            label = { Text(stringResource(weekdayResource(day))) }
                        )
                    }
                }
                if (weekdays.isEmpty()) Text(stringResource(R.string.create_no_weekdays), color = FormCoral)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.create_mission), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(MissionType.MATH, MissionType.STEPS, MissionType.QR).forEach { type ->
                        FilterChip(
                            selected = mission == type,
                            onClick = { mission = type },
                            label = { Text(missionLabel(type)) }
                        )
                    }
                }
                if (mission == MissionType.MATH) {
                    Text(stringResource(R.string.create_difficulty), color = FormNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { level ->
                            FilterChip(
                                selected = difficulty == level,
                                onClick = { difficulty = level },
                                label = { Text(difficultyLabel(level)) }
                            )
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
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEFDB)), shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(R.string.permission_alarm_body), color = FormNavy, modifier = Modifier.padding(16.dp))
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    try {
                        val code = qrExpectedCode.ifEmpty { null }
                        if (alarm == null) coordinator.create(hour, minute, mission, difficulty, code, weekdays)
                        else coordinator.update(alarm.copy(
                            hour = hour, minute = minute, weekdays = weekdays,
                            missionType = mission, difficulty = difficulty, qrExpectedCode = code
                        ))
                        onSaved()
                    } catch (error: Exception) {
                        saveNeedsSettings = error is IllegalStateException
                        saveError = error.localizedMessage ?: context.getString(R.string.create_error_title)
                    }
                }
            },
            enabled = weekdays.isNotEmpty() && (mission != MissionType.QR || qrExpectedCode.isNotBlank()),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormCoral),
            shape = RoundedCornerShape(18.dp)
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

@Composable
private fun difficultyLabel(level: Difficulty): String = stringResource(
    when (level) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
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
