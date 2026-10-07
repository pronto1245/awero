package app.awero.ui

import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.app.Activity
import app.awero.core.wake.WakeFlowController

object WakeAlarmScreen {
    fun create(activity: Activity, flow: WakeFlowController): LinearLayout {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.BLACK)
        }
        fun text(value: String, size: Float): TextView = TextView(activity).apply {
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

        primary.setOnClickListener {
            if (flow.state == WakeFlowController.State.RINGING) {
                flow.beginMission()
                primary.text = "Complete mission"
                status.text = flow.mission.name
            } else if (flow.state == WakeFlowController.State.MISSION) {
                flow.completeMission()
                title.text = "YOU'RE UP"
                status.text = "Wake session completed"
                primary.isEnabled = false
                snooze.isEnabled = false
                emergency.isEnabled = false
            }
        }
        snooze.setOnClickListener {
            if (flow.snooze()) {
                title.text = "SNOOZED"
                status.text = "Alarm will be handled by the next scheduled occurrence"
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
