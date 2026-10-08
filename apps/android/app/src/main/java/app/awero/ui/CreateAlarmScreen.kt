package app.awero.ui

import android.Manifest
import android.content.pm.PackageManager
import android.app.TimePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.awero.core.alarm.AlarmCoordinator
import app.awero.core.alarm.Difficulty
import app.awero.core.alarm.MissionType
import app.awero.core.missions.QRMissionRuntime
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
    var qrExpectedCode by remember { mutableStateOf(alarm?.qrExpectedCode.orEmpty()) }
    var showCodeScanner by remember { mutableStateOf(false) }
    var scannerError by remember { mutableStateOf<String?>(null) }
    val owner = context as? ComponentActivity
    val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCodeScanner = true
        else scannerError = "Camera access was denied. You can enter the code content manually."
    }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
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
                FilterChip(selected = mission == type, onClick = { mission = type }, label = { Text(if (type == MissionType.QR) "QR/barcode" else type.name) })
            }
        }
        if (mission == MissionType.QR) {
            OutlinedButton(onClick = {
                scannerError = null
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    showCodeScanner = true
                } else requestCamera.launch(Manifest.permission.CAMERA)
            }, enabled = owner != null) { Text("Scan QR/barcode") }
            OutlinedTextField(
                value = qrExpectedCode, onValueChange = { qrExpectedCode = it },
                label = { Text("QR/barcode content") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Scan a code where you want to wake up, or enter its exact content.", color = Color.White)
            scannerError?.let { Text(it, color = Color.White) }
        }
        if (mission == MissionType.MATH) {
            Text("Difficulty", color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { d ->
                    FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.name) })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            scope.launch {
                try {
                    val code = qrExpectedCode.ifEmpty { null }
                    if (alarm == null) coordinator.create(hour, minute, mission, difficulty, code)
                    else coordinator.update(alarm.copy(hour = hour, minute = minute, missionType = mission, difficulty = difficulty, qrExpectedCode = code))
                    onSaved()
                } catch (error: Exception) {
                    saveError = error.message ?: "Could not schedule this alarm."
                }
            }
        }, enabled = mission != MissionType.QR || qrExpectedCode.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(if (alarm == null) "Create alarm" else "Save changes")
        }
    }
    if (showCodeScanner && owner != null) {
        val runtime = remember { QRMissionRuntime(context) }
        DisposableEffect(runtime) { onDispose { runtime.close() } }
        AlertDialog(
            onDismissRequest = { showCodeScanner = false },
            title = { Text("Scan QR/barcode") },
            text = {
                AndroidView(
                    factory = { cameraContext ->
                        PreviewView(cameraContext).also { preview ->
                            runtime.start(owner, preview) { code ->
                                if (code.isNotBlank()) {
                                    qrExpectedCode = code
                                    scannerError = null
                                } else scannerError = "Camera is unavailable. You can enter the code content manually."
                                showCodeScanner = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showCodeScanner = false }) { Text("Cancel") }
            }
        )
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
