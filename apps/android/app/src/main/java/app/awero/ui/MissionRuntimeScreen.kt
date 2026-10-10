package app.awero.ui

import androidx.compose.ui.graphics.toArgb
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
            gravity = Gravity.TOP
            val padding = (6 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.TRANSPARENT)
        }
        fun label(value: String, size: Float = 22f) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(AweroDesign.navyArgb)
            gravity = Gravity.CENTER
            val verticalPadding = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
        }
        val density = activity.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun matchWidth(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(top), 0, dp(bottom)) }
        fun heading(value: String, color: Int = AweroDesign.navyArgb) = label(value, 28f).apply {
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(color)
        }
        fun subtitle(value: String, color: Int = AweroDesign.textSecondary.toArgb()) = label(value, 16f).apply {
            setTextColor(color)
        }
        fun textAction(value: String, action: () -> Unit) = Button(activity).apply {
            text = value
            isAllCaps = false
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(AweroDesign.coralArgb)
            background = null
            minHeight = dp(44)
            setOnClickListener { action() }
        }
        if (alarm.missionType != MissionType.STEPS && alarm.missionType != MissionType.QR) {
            root.addView(heading(activity.getString(R.string.wake_heading)), matchWidth())
        }
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
                root.addView(label(activity.getString(R.string.wake_instruction), 16f))
                // The answer is typed straight into the problem card: "7 + 5 = 12".
                val answer = EditText(activity).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    setTextColor(AweroDesign.coral.toArgb())
                    setHintTextColor(AweroDesign.navyArgb)
                    textSize = 40f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    hint = "?"
                    contentDescription = activity.getString(R.string.mission_answer)
                    showSoftInputOnFocus = false
                    isCursorVisible = false
                    background = null
                    minWidth = dp(40)
                    setPadding(dp(6), 0, 0, 0)
                }
                val problemCard = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(18), dp(12), dp(18))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor((AweroDesign.surfaceArgb and 0x00FFFFFF) or (0xF7 shl 24))
                        cornerRadius = 22 * density
                    }
                    addView(TextView(activity).apply {
                        text = "${problem.left} ${problem.operation} ${problem.right} ="
                        textSize = 40f
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                        setTextColor(AweroDesign.navyArgb)
                        includeFontPadding = false
                    })
                    addView(answer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                root.addView(problemCard, matchWidth(top = 6, bottom = 6))
                val invalidMessage = label(activity.getString(R.string.mission_try_again), 16f).apply {
                    setTextColor(AweroDesign.warningArgb)
                    visibility = View.GONE
                    minHeight = (32 * resources.displayMetrics.density).toInt()
                }
                fun keypadKey(value: String, description: String? = null, action: () -> Unit) = Button(activity).apply {
                    text = value
                    description?.let { contentDescription = it }
                    minHeight = (52 * resources.displayMetrics.density).toInt()
                    setTextColor(AweroDesign.navyArgb)
                    textSize = 24f
                    minWidth = 0
                    val padding = (8 * resources.displayMetrics.density).toInt()
                    setPadding(padding, padding, padding, padding)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(AweroDesign.surfaceArgb)
                        cornerRadius = 14 * resources.displayMetrics.density
                    }
                    setOnClickListener { action() }
                }
                root.addView(invalidMessage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0"))
                var checkRow: LinearLayout? = null
                rows.forEach { values ->
                    val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
                    checkRow = row
                    values.forEach { value ->
                        row.addView(keypadKey(value, if (value == "⌫") activity.getString(R.string.mission_delete_digit) else null) {
                            val current = answer.text.toString()
                            answer.setText(if (value == "⌫") current.dropLast(1) else if (current.length < 12) current + value else current)
                            invalidMessage.visibility = View.GONE
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val gap = (4 * activity.resources.displayMetrics.density).toInt()
                    setMargins(gap, gap, gap, gap)
                })
                    }
                    root.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                }
                checkRow!!.addView(Button(activity).apply {
                    setTextColor(android.graphics.Color.WHITE)
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
                        else { answer.text.clear(); invalidMessage.visibility = View.VISIBLE }
                    }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    val gap = (4 * activity.resources.displayMetrics.density).toInt()
                    setMargins(gap, gap, gap, gap)
                })
            }
            MissionType.STEPS -> {
                val target = 30
                val mission = StepsMission(activity)
                val motionStarted = runCatching { mission.start(target) }.isSuccess && mission.available
                root.addView(heading(activity.getString(R.string.mission_steps_heading)), matchWidth())
                root.addView(subtitle(activity.getString(R.string.mission_steps_subtitle)), matchWidth())
                val ring = StepsRingView(activity)
                val count = TextView(activity).apply {
                    text = "0"
                    textSize = 52f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(AweroDesign.navyArgb)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }
                val ringBox = android.widget.FrameLayout(activity).apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    contentDescription = activity.getString(R.string.mission_steps_progress, 0)
                    addView(ring, android.widget.FrameLayout.LayoutParams(-1, -1))
                    addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        addView(TextView(activity).apply {
                            text = "🚶"
                            textSize = 26f
                            gravity = Gravity.CENTER
                        })
                        addView(count)
                        addView(TextView(activity).apply {
                            text = activity.getString(R.string.mission_steps_of_target)
                            textSize = 14f
                            setTextColor(AweroDesign.textSecondary.toArgb())
                            gravity = Gravity.CENTER
                        })
                    }, android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
                }
                root.addView(ringBox, LinearLayout.LayoutParams(dp(220), dp(220)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, dp(10), 0, dp(10))
                })
                if (motionStarted) {
                    root.addView(LinearLayout(activity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor((AweroDesign.surfaceArgb and 0x00FFFFFF) or (0xF0 shl 24))
                            cornerRadius = 20 * density
                        }
                        addView(TextView(activity).apply { text = "💡"; textSize = 18f })
                        addView(TextView(activity).apply {
                            text = activity.getString(R.string.mission_steps_tip)
                            textSize = 15f
                            setTextColor(AweroDesign.navyArgb)
                            setPadding(dp(10), 0, 0, 0)
                        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    }, matchWidth(top = 4, bottom = 4))
                } else {
                    root.addView(subtitle(activity.getString(R.string.mission_motion_unavailable)), matchWidth())
                }
                root.addView(textAction(activity.getString(R.string.mission_use_math)) { finish(false) }, matchWidth())
                root.addView(subtitle(activity.getString(R.string.mission_timeout)).apply { textSize = 12f }, matchWidth())
                // The step sensor reports in the background; refresh the ring twice a second and
                // finish as soon as the target is reached.
                val poll = object : Runnable {
                    override fun run() {
                        if (finished) return
                        val steps = mission.steps.coerceAtMost(target)
                        count.text = steps.toString()
                        ring.progress = steps / target.toFloat()
                        ringBox.contentDescription = activity.getString(R.string.mission_steps_progress, steps)
                        if (mission.validate()) finish(true) else root.postDelayed(this, 500)
                    }
                }
                cleanup = {
                    root.removeCallbacks(poll)
                    mission.stop()
                }
                if (motionStarted) root.post(poll)
            }
            MissionType.QR -> {
                val expected = alarm.qrExpectedCode
                val light = Color.WHITE
                val muted = Color.argb(0xB3, 0xFF, 0xFF, 0xFF)
                root.background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(QR_PANEL)
                    cornerRadius = 24 * density
                }
                root.setPadding(dp(16), dp(18), dp(16), dp(16))
                root.addView(heading(activity.getString(R.string.mission_qr_heading), light), matchWidth())
                root.addView(subtitle(activity.getString(R.string.mission_qr_subtitle), muted), matchWidth(bottom = 8))
                if (expected.isNullOrBlank()) {
                    root.addView(subtitle(activity.getString(R.string.mission_qr_unconfigured), muted), matchWidth(bottom = 8))
                } else {
                    val mission = app.awero.core.missions.QRMission(expected)
                    val preview = PreviewView(activity).apply {
                        contentDescription = activity.getString(R.string.mission_qr_instructions)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                        isFocusable = true
                        clipToOutline = true
                        outlineProvider = object : android.view.ViewOutlineProvider() {
                            override fun getOutline(view: View, outline: android.graphics.Outline) {
                                outline.setRoundRect(0, 0, view.width, view.height, 22 * density)
                            }
                        }
                    }
                    val previewBox = android.widget.FrameLayout(activity).apply {
                        addView(preview, android.widget.FrameLayout.LayoutParams(-1, -1))
                        addView(ScannerFrameView(activity), android.widget.FrameLayout.LayoutParams(-1, -1).apply {
                            setMargins(dp(18), dp(18), dp(18), dp(18))
                        })
                    }
                    root.addView(previewBox, LinearLayout.LayoutParams(-1, dp(280)))
                    root.addView(subtitle(activity.getString(R.string.mission_qr_instructions), muted), matchWidth(top = 4))
                    root.addView(subtitle(activity.getString(R.string.mission_timeout), muted).apply { textSize = 12f }, matchWidth())
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
                }
                root.addView(Button(activity).apply {
                    text = activity.getString(R.string.mission_qr_cant_scan)
                    isAllCaps = false
                    textSize = 16f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setTextColor(QR_BUTTON_TEXT)
                    minHeight = dp(54)
                    contentDescription = activity.getString(R.string.mission_qr_cant_scan) + ". " + activity.getString(R.string.mission_use_math)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(light)
                        cornerRadius = 14 * density
                    }
                    setOnClickListener { finish(false) }
                }, matchWidth(top = 10))
            }
            else -> {
                root.addView(label(activity.getString(R.string.mission_unavailable), 22f))
                root.addView(Button(activity).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb)
                    text = activity.getString(R.string.mission_use_fallback)
                    setOnClickListener { finish(false) }
                })
            }
        }
        return root
    }
}

/** The QR mission is a dark camera panel in both themes, as in the reference screen. */
private const val QR_PANEL = 0xFF2A2725.toInt()
private const val QR_BUTTON_TEXT = 0xFF1D2433.toInt()

/** Progress ring for the Steps mission: coral arc on a chip-colored track. */
private class StepsRingView(context: android.content.Context) : View(context) {
    private val stroke = 22 * context.resources.displayMetrics.density
    private val track = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = stroke
        color = AweroDesign.chip.toArgb()
    }
    private val arc = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = stroke
        color = AweroDesign.coral.toArgb()
    }
    private val bounds = android.graphics.RectF()

    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val inset = stroke / 2
        bounds.set(inset, inset, width - inset, height - inset)
        canvas.drawOval(bounds, track)
        if (progress > 0f) canvas.drawArc(bounds, -90f, 360f * progress, false, arc)
    }
}

/** Coral corner brackets and a scan line drawn over the camera preview. */
private class ScannerFrameView(context: android.content.Context) : View(context) {
    private val density = context.resources.displayMetrics.density
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4 * density
        strokeCap = android.graphics.Paint.Cap.ROUND
        color = AweroDesign.coral.toArgb()
    }
    private val line = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = AweroDesign.coral.toArgb()
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val arm = 34 * density
        val w = width.toFloat()
        val h = height.toFloat()
        val corners = listOf(
            floatArrayOf(0f, 0f, 1f, 1f),
            floatArrayOf(w, 0f, -1f, 1f),
            floatArrayOf(0f, h, 1f, -1f),
            floatArrayOf(w, h, -1f, -1f)
        )
        for (c in corners) {
            canvas.drawLine(c[0], c[1], c[0] + c[2] * arm, c[1], paint)
            canvas.drawLine(c[0], c[1], c[0], c[1] + c[3] * arm, paint)
        }
        canvas.drawRect(14 * density, h / 2 - density, w - 14 * density, h / 2 + density, line)
    }
}
