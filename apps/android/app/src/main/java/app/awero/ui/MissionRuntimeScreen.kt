package app.awero.ui

import androidx.activity.ComponentActivity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.MissionType
import app.awero.core.missions.MathMission
import app.awero.core.missions.QRMissionRuntime
import app.awero.core.missions.StepsMission

object MissionRuntimeScreen {
    fun create(activity: ComponentActivity, alarm: Alarm, onSuccess: () -> Unit, onFailure: () -> Unit, timeoutMillis: Long = 120_000L): View {
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
        var finished = false
        var cleanup: () -> Unit = {}
        var deadline: Runnable? = null
        fun finish(success: Boolean) {
            if (finished) return
            finished = true
            deadline?.let { root.removeCallbacks(it) }
            cleanup()
            if (success) onSuccess() else onFailure()
        }
        if (alarm.missionType == MissionType.STEPS || alarm.missionType == MissionType.QR) {
            deadline = Runnable { finish(false) }
            root.addView(label("After 2 minutes, this task switches to Math.", 16f))
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                if (!finished) deadline?.let { root.postDelayed(it, timeoutMillis.coerceAtLeast(0)) }
            }
            override fun onViewDetachedFromWindow(view: View) {
                finished = true
                deadline?.let { root.removeCallbacks(it) }
                cleanup()
            }
        })

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
                        if (value != null && mission.validate(value)) finish(true)
                        else { answer.text.clear(); answer.hint = "Try again" }
                    }
                })
            }
            MissionType.STEPS -> {
                val mission = StepsMission(activity)
                cleanup = { mission.stop() }
                val motionStarted = runCatching { mission.start() }.isSuccess && mission.available
                val status = label(if (motionStarted) "Walk 30 steps" else "Motion unavailable. Use Math fallback.", 30f)
                root.addView(status)
                root.addView(Button(activity).apply {
                    text = "CHECK STEPS"
                    setOnClickListener {
                        status.text = "Steps: ${mission.steps} / 30"
                        if (mission.validate()) finish(true)
                    }
                })
                root.addView(Button(activity).apply {
                    text = "I CAN'T WALK"
                    setOnClickListener { finish(false) }
                })
            }
            MissionType.QR -> {
                val expected = alarm.qrExpectedCode
                if (expected.isNullOrBlank()) {
                    root.addView(label("QR mission is not configured.", 22f))
                    root.addView(Button(activity).apply {
                        text = "USE FALLBACK"
                        setOnClickListener { finish(false) }
                    })
                } else {
                    root.addView(label("Scan your saved QR or barcode", 24f))
                    val preview = PreviewView(activity)
                    root.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
                    val runtime = QRMissionRuntime(activity)
                    val lifecycleObserver = object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            runtime.close()
                            owner.lifecycle.removeObserver(this)
                        }
                    }
                    activity.lifecycle.addObserver(lifecycleObserver)
                    cleanup = {
                        runtime.close()
                        activity.lifecycle.removeObserver(lifecycleObserver)
                    }
                    runtime.start(activity, preview) {
                        activity.runOnUiThread {
                            finish(runtime.matches(expected))
                        }
                    }
                    root.addView(Button(activity).apply {
                        text = "USE MATH FALLBACK"
                        setOnClickListener { finish(false) }
                    })
                }
            }
            else -> {
                root.addView(label("This mission is not available in MVP.", 22f))
                root.addView(Button(activity).apply {
                    text = "USE FALLBACK"
                    setOnClickListener { finish(false) }
                })
            }
        }
        return root
    }
}
