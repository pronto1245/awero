package app.awero.ui

import androidx.activity.ComponentActivity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import app.awero.core.wake.WakeFlowController

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

        fun showMission() {
            root.removeAllViews()
            val missionView = MissionRuntimeScreen.create(
                activity,
                flow.currentAlarm!!,
                onSuccess = {
                    flow.completeMission()
                    root.removeAllViews()
                    root.addView(text("YOU'RE UP", 42f))
                    root.addView(text("Wake session completed", 18f))
                },
                onFailure = {
                    flow.fallbackToMath()
                    root.removeAllViews()
                    root.addView(text("FALLBACK", 30f))
                    root.addView(MissionRuntimeScreen.create(
                        activity,
                        flow.currentAlarm!!.copy(missionType = app.awero.core.alarm.MissionType.MATH),
                        onSuccess = {
                            flow.completeMission()
                            root.removeAllViews()
                            root.addView(text("YOU'RE UP", 42f))
                        },
                        onFailure = {}
                    ))
                }
            )
            root.addView(missionView)
        }

        primary.setOnClickListener {
            if (flow.state == WakeFlowController.State.RINGING) {
                flow.beginMission()
                showMission()
            }
        }

        snooze.setOnClickListener {
            if (flow.snooze()) {
                title.text = "SNOOZED"
                status.text = "Alarm scheduled again"
                primary.isEnabled = false
                snooze.isEnabled = false
            }
        }

        emergency.setOnClickListener {
            flow.emergencyStop()
            title.text = "STOPPED"
            status.text = "Session recorded"
            primary.isEnabled = false
            snooze.isEnabled = false
            emergency.isEnabled = false
        }

        root.addView(status)
        root.addView(title)
        root.addView(primary)
        root.addView(snooze)
        root.addView(emergency)
        return root
    }
}
