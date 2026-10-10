package app.awero.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import app.awero.core.alarm.Alarm
import app.awero.core.wake.WakeFlowController
import app.awero.core.wake.WakeSessionStore

@Composable
fun AweroApp(statusRefreshKey: Int = 0) {
    val context = LocalContext.current
    val flow = remember { WakeFlowController(WakeSessionStore(context), context) }
    val state by flow.state.collectAsState()
    val preferences = remember { context.getSharedPreferences("awero.onboarding", 0) }
    var screen by rememberSaveable { mutableStateOf("loading") }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    var editingAlarmId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = screen == "create" || screen == "edit" || screen == "settings" || screen == "progress" || screen == "profile") {
        editingAlarm = null
        editingAlarmId = null
        screen = if (screen == "settings") "profile" else "home"
    }

    LaunchedEffect(Unit) {
        flow.restore()
        if (flow.state.value != WakeFlowController.State.IDLE) {
            screen = "wake"
        } else if (screen == "loading") {
            val alarms = runCatching { app.awero.core.alarm.AlarmCoordinator(context).all() }.getOrNull()
            if (preferences.getBoolean("completed", false) || alarms == null) {
                screen = "home"
            } else if (alarms.isEmpty()) {
                screen = "onboarding"
            } else {
                preferences.edit().putBoolean("completed", true).apply()
                screen = "home"
            }
        }
    }
    LaunchedEffect(editingAlarmId) {
        val id = editingAlarmId ?: return@LaunchedEffect
        if (editingAlarm?.id != id) {
            val restored = runCatching {
                app.awero.core.alarm.AlarmCoordinator(context).all().firstOrNull { it.id == id }
            }.getOrNull()
            if (restored == null) {
                editingAlarmId = null
                screen = "home"
            } else {
                editingAlarm = restored
            }
        }
    }

    MaterialTheme(colorScheme = AweroDesign.colors) {
        when (screen) {
            "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            "onboarding" -> OnboardingScreen(onContinue = {
                preferences.edit().putBoolean("completed", true).apply()
                (context as? app.awero.MainActivity)?.requestAlarmAccessAfterEducation()
                screen = "create"
            })
            "create" -> CreateAlarmScreen(
                onSaved = { editingAlarm = null; screen = "home" },
                onCancel = { screen = "home" }
            )
            "edit" -> if (editingAlarm?.id == editingAlarmId) {
                CreateAlarmScreen(
                    alarm = editingAlarm,
                    onSaved = { editingAlarm = null; editingAlarmId = null; screen = "home" },
                    onCancel = { editingAlarm = null; editingAlarmId = null; screen = "home" }
                )
            } else {
                CircularProgressIndicator()
            }
            "wake" -> WakeScreen(flow)
            else -> {
                if (state == WakeFlowController.State.RINGING ||
                    state == WakeFlowController.State.MISSION ||
                    state == WakeFlowController.State.COMPLETED ||
                    state == WakeFlowController.State.EMERGENCY_STOPPED
                ) {
                    LaunchedEffect(state) { screen = "wake" }
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar(containerColor = AweroDesign.ivory, tonalElevation = 0.dp) {
                            val itemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AweroDesign.coral,
                                selectedTextColor = AweroDesign.navy,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = AweroDesign.navy.copy(alpha = .65f),
                                unselectedTextColor = AweroDesign.navy.copy(alpha = .65f)
                            )
                            NavigationBarItem(
                                selected = screen == "home",
                                onClick = { screen = "home" },
                                colors = itemColors,
                                icon = { AweroNavigationIcon("home") },
                                label = { Text(androidx.compose.ui.res.stringResource(app.awero.R.string.nav_home), fontSize = 10.sp, letterSpacing = (-0.1).sp, maxLines = 1, softWrap = false) }
                            )
                            NavigationBarItem(
                                selected = screen == "progress",
                                onClick = { screen = "progress" },
                                colors = itemColors,
                                icon = { AweroNavigationIcon("progress") },
                                label = { Text(androidx.compose.ui.res.stringResource(app.awero.R.string.nav_progress), fontSize = 10.sp, letterSpacing = (-0.1).sp, maxLines = 1, softWrap = false) }
                            )
                            NavigationBarItem(
                                selected = screen == "profile" || screen == "settings",
                                onClick = { screen = "profile" },
                                colors = itemColors,
                                icon = { AweroNavigationIcon("profile") },
                                label = { Text(androidx.compose.ui.res.stringResource(app.awero.R.string.nav_profile), fontSize = 10.sp, letterSpacing = (-0.1).sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (screen) {
                        "progress" -> AweroProgressScreen(
                            modifier = Modifier.padding(innerPadding),
                            onSetAlarm = { screen = "create" },
                            onOpenSettings = { screen = "settings" }
                        )
                        "settings" -> Box(Modifier.padding(innerPadding)) {
                            SettingsScreen(onBack = { screen = "profile" })
                        }
                        "profile" -> ProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenSettings = { screen = "settings" }
                        )
                        else -> HomeScreen(
                            onCreateAlarm = {
                                editingAlarm = null
                                editingAlarmId = null
                                screen = "create"
                            },
                            onOpenSettings = { screen = "settings" },
                            onEditAlarm = {
                                editingAlarm = it
                                editingAlarmId = it.id
                                screen = "edit"
                            },
                            statusRefreshKey = statusRefreshKey,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
