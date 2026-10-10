package app.awero.ui

import androidx.activity.ComponentActivity
import android.graphics.Color
import app.awero.R
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
            val padding = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, padding, 0, padding)
            setBackgroundColor(AweroDesign.ivoryArgb)
        }
        fun label(value: String, size: Float = 22f) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(AweroDesign.navyArgb)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 8)
        }
        root.addView(label(activity.getString(R.string.wake_title), 26f), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
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
            root.addView(label(activity.getString(R.string.mission_timeout), 16f))
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
                root.setBackgroundColor(Color.TRANSPARENT)
                val mission = MathMission(alarm.difficulty)
                mission.start()
                val problem = mission.problem ?: return root
                root.addView(label("${problem.left} ${problem.operation} ${problem.right} = ?", 34f).apply {
                    background = android.graphics.drawable.GradientDrawable().apply { setColor(0xF2FFFFFF.toInt()); cornerRadius = 20 * resources.displayMetrics.density }
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                val answer = EditText(activity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                    setTextColor(AweroDesign.navyArgb)
                    setTextSize(28f)
                    gravity = Gravity.CENTER
                    hint = activity.getString(R.string.mission_answer)
                    showSoftInputOnFocus = false
                }
                fun keypadKey(value: String, description: String? = null, action: () -> Unit) = Button(activity).apply {
                    text = value
                    description?.let { contentDescription = it }
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    setTextColor(AweroDesign.navyArgb)
                    textSize = 24f
                    minWidth = 0
                    val padding = (8 * resources.displayMetrics.density).toInt()
                    setPadding(padding, padding, padding, padding)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = 14 * resources.displayMetrics.density
                    }
                    setOnClickListener { action() }
                }
                val answerRow = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                answerRow.addView(answer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                answerRow.addView(keypadKey("±", activity.getString(R.string.mission_change_sign)) {
                    val current = answer.text.toString()
                    answer.setText(if (current.startsWith("-")) current.drop(1) else "-$current")
                }, LinearLayout.LayoutParams((48 * activity.resources.displayMetrics.density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT))
                root.addView(answerRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0"))
                var checkRow: LinearLayout? = null
                rows.forEach { values ->
                    val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                    checkRow = row
                    values.forEach { value ->
                        row.addView(keypadKey(value, if (value == "⌫") activity.getString(R.string.mission_delete_digit) else null) {
                            val current = answer.text.toString()
                            answer.setText(if (value == "⌫") current.dropLast(1) else if (current.length < 12) current + value else current)
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val gap = (4 * activity.resources.displayMetrics.density).toInt()
                    setMargins(gap, gap, gap, gap)
                })
                    }
                    root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                checkRow!!.addView(Button(activity).apply {
                    setTextColor(AweroDesign.navyArgb)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                    text = "✓"
                    contentDescription = activity.getString(R.string.mission_check)
                    textSize = 24f
                    minWidth = 0
                    minHeight = (48 * resources.displayMetrics.density).toInt()
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(AweroDesign.coralArgb)
                        cornerRadius = 14 * resources.displayMetrics.density
                    }
                    setOnClickListener {
                        val value = answer.text.toString().toIntOrNull()
                        if (value != null && mission.validate(value)) finish(true)
                        else { answer.text.clear(); answer.hint = activity.getString(R.string.mission_try_again) }
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val gap = (4 * activity.resources.displayMetrics.density).toInt()
                    setMargins(gap, gap, gap, gap)
                })
            }
            MissionType.STEPS -> {
                val mission = StepsMission(activity)
                cleanup = { mission.stop() }
                val motionStarted = runCatching { mission.start() }.isSuccess && mission.available
                val status = label(if (motionStarted) activity.getString(R.string.mission_steps_title) else activity.getString(R.string.mission_motion_unavailable), 30f)
                root.addView(status)
                root.addView(Button(activity).apply {
                    setTextColor(AweroDesign.navyArgb)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                    text = activity.getString(R.string.mission_check)
                    setOnClickListener {
                        status.text = activity.getString(R.string.mission_steps_progress, mission.steps)
                        if (mission.validate()) finish(true)
                    }
                })
                root.addView(Button(activity).apply {
                    setTextColor(AweroDesign.navyArgb)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                    text = activity.getString(R.string.mission_use_math)
                    setOnClickListener { finish(false) }
                })
            }
            MissionType.QR -> {
                val expected = alarm.qrExpectedCode
                if (expected.isNullOrBlank()) {
                    root.addView(label(activity.getString(R.string.mission_qr_unconfigured), 22f))
                    root.addView(Button(activity).apply {
                        setTextColor(AweroDesign.navyArgb)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                        text = activity.getString(R.string.mission_use_fallback)
                        setOnClickListener { finish(false) }
                    })
                } else {
                    val mission = app.awero.core.missions.QRMission(expected)
                    root.addView(label(activity.getString(R.string.mission_qr_title), 24f))
                    val preview = PreviewView(activity).apply {
                        contentDescription = activity.getString(R.string.mission_qr_instructions)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                        isFocusable = true
                    }
                    root.addView(preview, LinearLayout.LayoutParams(-1, (300 * activity.resources.displayMetrics.density).toInt()))
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
                    runtime.start(activity, preview) { scannedCode ->
                        activity.runOnUiThread {
                            finish(mission.validatePayload(scannedCode))
                        }
                    }
                    root.addView(Button(activity).apply {
                        setTextColor(AweroDesign.navyArgb)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                        text = activity.getString(R.string.mission_use_math)
                        setOnClickListener { finish(false) }
                    })
                }
            }
            else -> {
                root.addView(label(activity.getString(R.string.mission_unavailable), 22f))
                root.addView(Button(activity).apply {
                    setTextColor(AweroDesign.navyArgb)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                    text = activity.getString(R.string.mission_use_fallback)
                    setOnClickListener { finish(false) }
                })
            }
        }
        return root
    }
}
