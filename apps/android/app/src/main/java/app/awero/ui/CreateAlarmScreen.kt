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
import androidx.compose.ui.draw.drawWithContent
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

private val FormIvory get() = AweroDesign.ivory
private val FormNavy get() = AweroDesign.navy
private val FormCoral get() = AweroDesign.coralStrong

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
    var showAdvanced by rememberSaveable(alarm?.id) { mutableStateOf(false) }
    // Set once a new alarm is saved, so a retry after a failed test updates it instead of creating another.
    var savedAlarm by remember { mutableStateOf<Alarm?>(null) }
    val canSave = weekdays.isNotEmpty() && (mission != MissionType.QR || qrExpectedCode.isNotBlank())

    /** Saves the alarm; with [test], also schedules a test ring that does not count as a wake. */
    fun save(test: Boolean) {
        if (!canSave) return
        scope.launch {
            try {
                val code = qrExpectedCode.ifEmpty { null }
                val timezoneMode = if (followsDeviceTimezone) TimezoneMode.DEVICE_LOCAL else TimezoneMode.FIXED
                val selectedFixedTimezone = if (followsDeviceTimezone) null else fixedTimezone
                val existing = savedAlarm ?: alarm
                val stored = if (existing == null) coordinator.create(
                    hour, minute, mission, difficulty, code, weekdays, timezoneMode, selectedFixedTimezone
                ) else coordinator.update(existing.copy(
                    hour = hour, minute = minute, weekdays = weekdays,
                    timezoneMode = timezoneMode, fixedTimezone = selectedFixedTimezone,
                    missionType = mission, difficulty = difficulty, qrExpectedCode = code
                ))
                savedAlarm = stored
                if (test) coordinator.test(stored)
                onSaved()
            } catch (error: Exception) {
                Log.e("AWERO.Alarm", "Could not save alarm", error)
                saveNeedsSettings = error is IllegalStateException
                saveError = context.getString(R.string.create_error_body)
            }
        }
    }
    val owner = context as? ComponentActivity
    val configuration = LocalConfiguration.current
    // Stack mission choices on narrow phones too: three columns leave ~70dp per card there.
    val compactLayout = configuration.fontScale >= 1.35f || configuration.screenWidthDp < 360
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
            // Board: a back chevron on the left and the title centred. The chevron keeps the
            // localized "Cancel" as its accessibility label.
            val cancelLabel = stringResource(R.string.create_cancel)
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterStart)
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = cancelLabel },
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = FormNavy)
            ) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            }
            Text(
                stringResource(if (alarm == null) R.string.create_title else R.string.create_edit_title),
                color = FormNavy,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 76.dp)
            )
            TextButton(
                onClick = { save(test = false) },
                enabled = canSave,
                modifier = Modifier.align(Alignment.CenterEnd).defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text(
                    stringResource(R.string.create_done),
                    color = if (canSave) FormCoral else AweroDesign.textSecondary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.create_time), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                val timeLabel = stringResource(R.string.create_time)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AweroDesign.cardCorner),
                    color = AweroDesign.surfaceMuted
                ) {
                    Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.fillMaxWidth().height(WheelItemHeight)
                                .clip(RoundedCornerShape(14.dp)).background(AweroDesign.surfaceStrong)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TimeWheel(hour, 24, timeLabel, { hour = it }, Modifier.weight(1f))
                            Text(":", color = FormNavy, style = MaterialTheme.typography.headlineMedium)
                            TimeWheel(minute, 60, timeLabel, { minute = it }, Modifier.weight(1f))
                        }
                    }
                }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.create_repeat), color = FormNavy, style = MaterialTheme.typography.titleMedium)
                run {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        listOf(2, 3, 4, 5, 6, 7, 1).forEach { day ->
                            val selected = day in weekdays
                            val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (selected) FormCoral else AweroDesign.chip)
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
                run {
                    if (compactLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(MissionType.MATH, MissionType.STEPS, MissionType.QR).forEach { type ->
                                MissionOptionCard(type, mission == type, Modifier.fillMaxWidth(), onClick = { mission = type })
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Difficulty and time zone, collapsed by default as in the reference screen.
        Surface(shape = RoundedCornerShape(AweroDesign.cardCorner), color = AweroDesign.surface) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.create_advanced), color = FormNavy, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(if (showAdvanced) "⌃" else "⌄", color = AweroDesign.textSecondary, style = MaterialTheme.typography.titleMedium)
                }
                if (showAdvanced) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (mission == MissionType.MATH) {
                            Text(stringResource(R.string.create_difficulty), color = FormNavy, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(Difficulty.EASY to R.string.difficulty_easy, Difficulty.MEDIUM to R.string.difficulty_medium, Difficulty.HARD to R.string.difficulty_hard).forEach { (level, label) ->
                                    FilterChip(selected = difficulty == level, onClick = { difficulty = level }, label = { Text(stringResource(label)) })
                                }
                            }
                        }
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
                            color = AweroDesign.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        }
        Button(
            onClick = { save(test = true) },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FormCoral),
            shape = RoundedCornerShape(AweroDesign.controlCorner)
        ) {
            Text(stringResource(R.string.create_save_and_test), color = Color.White)
        }
        Text(
            stringResource(R.string.create_save_and_test_hint),
            color = AweroDesign.textSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
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
            .background(if (selected) AweroDesign.coralSoft else AweroDesign.surface)
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
        FitOneLineText(title, color = if (selected) FormCoral else FormNavy, style = MaterialTheme.typography.bodyMedium)
        Text(description, color = FormNavy.copy(alpha = .65f), style = MaterialTheme.typography.labelSmall, maxLines = 3)
    }
}

/**
 * Single-line text that shrinks (down to 70%) instead of breaking a word in the middle,
 * e.g. "Математика" or "Mathématiques" in a narrow mission card.
 */
@Composable
private fun FitOneLineText(text: String, color: Color, style: androidx.compose.ui.text.TextStyle) {
    var scale by remember(text, style) { mutableFloatStateOf(1f) }
    var ready by remember(text, style) { mutableStateOf(false) }
    Text(
        text,
        color = color,
        style = style.copy(fontSize = style.fontSize * scale),
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { layout ->
            if (layout.didOverflowWidth && scale > 0.7f) scale -= 0.05f else ready = true
        }
    )
}

private val WheelItemHeight = 46.dp

/**
 * A scrolling, snapping number wheel (00–[range]-1) showing the selected value in the middle row,
 * as in the reference New alarm screen. TalkBack reads the label and value and can scroll it.
 */
@Composable
private fun TimeWheel(value: Int, range: Int, label: String, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val loops = 100
    val base = range * (loops / 2)
    val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = base + value - 1)
    val fling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = state)
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val centered = (state.firstVisibleItemIndex + 1) % range
            if (centered != value) onChange(centered)
        }
    }
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && (state.firstVisibleItemIndex + 1) % range != value) {
            state.scrollToItem(base + value - 1)
        }
    }
    androidx.compose.foundation.lazy.LazyColumn(
        state = state,
        flingBehavior = fling,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(WheelItemHeight * 3).semantics {
            contentDescription = label + " " + String.format(Locale.ROOT, "%02d", value)
        }
    ) {
        items(range * loops) { index ->
            val selected = index == state.firstVisibleItemIndex + 1
            Box(Modifier.fillMaxWidth().height(WheelItemHeight), contentAlignment = Alignment.Center) {
                Text(
                    String.format(Locale.ROOT, "%02d", index % range),
                    color = if (selected) FormNavy else FormNavy.copy(alpha = .36f),
                    style = if (selected) MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    else MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
