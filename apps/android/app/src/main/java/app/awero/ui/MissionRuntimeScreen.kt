package app.awero.ui

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.MissionType
import app.awero.core.missions.MathMission
import app.awero.core.missions.QRMissionRuntime
import app.awero.core.missions.StepsMission

object MissionRuntimeScreen {
    fun create(activity: Activity, alarm: Alarm, onSuccess: () -> Unit, onFailure: () -> Unit): View {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.BLACK)
        }
        fun label(value: String, size: Float = 22f) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(label("WAKE MISSION", 28f))

        when (alarm.missionType) {
            MissionType.MATH -> {
                val mission = MathMission(alarm.difficulty)
                mission.start()
                val problem = mission.problem ?: return root
                root.addView(label("${problem.left} ${problem.operation} ${problem.right} = ?", 34f))
                val answer = EditText(activity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    setTextColor(Color.WHITE)
                    setTextSize(28f)
                    gravity = Gravity.CENTER
                    hint = "Answer"
                }
                root.addView(answer)
                root.addView(Button(activity).apply {
                    text = "CHECK"
                    setOnClickListener {
                        val value = answer.text.toString().toIntOrNull()
                        if (value != null && mission.validate(value)) onSuccess()
                        else { answer.text.clear(); answer.hint = "Try again" }
                    }
                })
            }
            MissionType.STEPS -> {
                val mission = StepsMission(activity)
                mission.start()
                val status = label("Walk 30 steps", 30f)
                root.addView(status)
                root.addView(Button(activity).apply {
                    text = "CHECK STEPS"
                    setOnClickListener {
                        status.text = "Steps: ${mission.steps} / 30"
                        if (mission.validate()) { mission.stop(); onSuccess() }
                    }
                })
                root.addView(Button(activity).apply {
                    text = "I CAN'T WALK"
                    setOnClickListener { mission.stop(); onFailure() }
                })
            }
            MissionType.QR -> {
                val expected = alarm.qrExpectedCode
                if (expected.isNullOrBlank()) {
                    root.addView(label("QR mission is not configured.", 22f))
                    root.addView(Button(activity).apply {
                        text = "USE FALLBACK"
                        setOnClickListener { onFailure() }
                    })
                } else {
                    root.addView(label("Scan your wake-up QR code", 24f))
                    val preview = PreviewView(activity)
                    root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
                    val runtime = QRMissionRuntime(activity)
                    runtime.start(activity, preview) { code ->
                        activity.runOnUiThread {
                            if (runtime.matches(expected)) onSuccess() else onFailure()
                            runtime.close()
                        }
                    }
                }
            }
            else -> {
                root.addView(label("This mission is not available in MVP.", 22f))
                root.addView(Button(activity).apply {
                    text = "USE FALLBACK"
                    setOnClickListener { onFailure() }
                })
            }
        }
        return root
    }
}
