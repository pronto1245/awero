package app.awero.ui

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.awero.core.wake.WakeFlowController
import kotlinx.coroutines.launch

object WakeAlarmScreen {
    fun create(activity: ComponentActivity, flow: WakeFlowController): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.BLACK)
        }

        fun text(value: String, size: Float) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val title = text("GET UP", 42f)
        val status = text("AWERO", 14f)
        val primary = Button(activity).apply { text = "Start mission" }
        val snooze = Button(activity).apply { text = "Snooze" }
        val emergency = Button(activity).apply { text = "Emergency stop" }

        fun showError(message: String) {
            status.text = message
            if (status.parent == null) root.addView(status, 0)
        }

        fun showCompleted() {
            root.removeAllViews()
            root.addView(text("YOU'RE UP", 42f))
            root.addView(text("Wake session completed", 18f))
        }

        fun showStopped() {
            root.removeAllViews()
            root.addView(text("STOPPED", 42f))
            root.addView(text("Session recorded", 18f))
        }

        fun showMission() {
            root.removeAllViews()
            status.text = "AWERO"
            root.addView(status)
            val missionView = MissionRuntimeScreen.create(
                activity,
                flow.currentAlarm.value!!,
                onSuccess = {
                    activity.lifecycleScope.launch {
                        if (flow.completeMission()) showCompleted()
                        else showError(flow.actionError.value ?: "Could not save completion. Your session is still active.")
                    }
                },
                onFailure = {
                    activity.lifecycleScope.launch {
                        if (!flow.fallbackToMath()) {
                            showError(flow.actionError.value ?: "Could not save the fallback. Your session is still active.")
                            return@launch
                        }
                        root.removeAllViews()
                        status.text = "FALLBACK"
                        root.addView(status)
                        root.addView(MissionRuntimeScreen.create(
                            activity,
                            flow.currentAlarm.value!!.copy(missionType = app.awero.core.alarm.MissionType.MATH),
                            onSuccess = {
                                activity.lifecycleScope.launch {
                                    if (flow.completeMission()) showCompleted()
                                    else showError(flow.actionError.value ?: "Could not save completion. Your session is still active.")
                                }
                            },
                            onFailure = {}
                        ))
                    }
                }
            )
            root.addView(missionView)
            root.addView(emergency)
        }

        primary.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.state.value == WakeFlowController.State.RINGING) {
                    flow.beginMission()
                    showMission()
                }
            }
        }

        snooze.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.snooze()) {
                    title.text = "SNOOZED"
                    status.text = "Alarm scheduled again"
                    primary.isEnabled = false
                    snooze.isEnabled = false
                } else {
                    status.text = flow.snoozeError.value ?: "Snooze failed. The alarm is still ringing."
                }
            }
        }

        emergency.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.emergencyStop()) {
                    showStopped()
                } else {
                    showError(flow.actionError.value ?: "Could not record the stop. The alarm is still active.")
                }
            }
        }

        root.addView(status)
        root.addView(title)
        root.addView(primary)
        root.addView(snooze)
        root.addView(emergency)
        return root
    }
}
