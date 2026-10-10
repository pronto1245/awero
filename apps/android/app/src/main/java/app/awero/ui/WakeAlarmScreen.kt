package app.awero.ui

import android.graphics.Color
import app.awero.R
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
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
            val density = resources.displayMetrics.density
            setPadding((20 * density).toInt(), (12 * density).toInt(), (20 * density).toInt(), (12 * density).toInt())
            background = AweroSunriseBackground()
        }

        fun text(value: String, size: Float) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(AweroDesign.navyArgb)
            gravity = Gravity.CENTER
            val verticalPadding = (10 * resources.displayMetrics.density).toInt()
            setPadding(0, verticalPadding, 0, verticalPadding)
        }

        fun actionButton(value: String, primaryAction: Boolean = false) = Button(activity).apply {
            text = value
            textSize = 16f
            isAllCaps = false
            minHeight = (54 * resources.displayMetrics.density).toInt()
            setTextColor(if (primaryAction) android.graphics.Color.WHITE else AweroDesign.navyArgb)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(if (primaryAction) AweroDesign.coralArgb else android.graphics.Color.WHITE)
                cornerRadius = 18 * resources.displayMetrics.density
            }
        }

        val title = text(activity.getString(R.string.wake_heading), 32f)
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        val status = text("AWERO", 20f).apply {
            gravity = Gravity.START
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        }
        val primary = actionButton(activity.getString(R.string.wake_start), primaryAction = true)
        val snooze = actionButton(activity.getString(R.string.wake_snooze))
        val emergency = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            contentDescription = activity.getString(R.string.wake_emergency_stop) + ". " + activity.getString(R.string.wake_emergency_stop_hint)
            val density = resources.displayMetrics.density
            setPadding((10 * density).toInt(), (7 * density).toInt(), (10 * density).toInt(), (7 * density).toInt())
            minimumHeight = (60 * density).toInt()
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFE8E2.toInt())
                setStroke((1 * density).toInt(), 0xFFFFC5BD.toInt())
                cornerRadius = 20 * density
            }
            val headline = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                val warningIcon = FrameLayout(activity)
                warningIcon.addView(TextView(activity).apply {
                    text = "▲"
                    setTextColor(0xFFD3313D.toInt())
                    textSize = 21f
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                }, FrameLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt(), Gravity.CENTER))
                warningIcon.addView(TextView(activity).apply {
                    text = "!"
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 11f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    includeFontPadding = false
                    translationY = (2 * resources.displayMetrics.density)
                }, FrameLayout.LayoutParams((24 * resources.displayMetrics.density).toInt(), (24 * resources.displayMetrics.density).toInt(), Gravity.CENTER))
                addView(warningIcon, LinearLayout.LayoutParams((28 * resources.displayMetrics.density).toInt(), (28 * resources.displayMetrics.density).toInt()).apply {
                    marginEnd = (6 * resources.displayMetrics.density).toInt()
                })
                addView(TextView(activity).apply {
                    text = activity.getString(R.string.wake_emergency_stop)
                    setTextColor(0xFFD3313D.toInt())
                    textSize = 15f
                    gravity = Gravity.CENTER
                })
            }
            addView(headline)
            addView(TextView(activity).apply {
                text = activity.getString(R.string.wake_emergency_stop_hint)
                setTextColor(AweroDesign.navyArgb)
                alpha = .62f
                textSize = 12f
                gravity = Gravity.CENTER
            })
        }
        val retry = actionButton(activity.getString(R.string.wake_retry), primaryAction = true)

        fun showError(message: String, retryAction: (() -> Unit)? = null) {
            status.text = message
            if (status.parent == null) root.addView(status, 0)
            if (retryAction != null) {
                retry.setOnClickListener { retryAction() }
                if (retry.parent == null) root.addView(retry)
            }
        }

        fun showCompleted() {
            root.removeAllViews()
            root.addView(text(activity.getString(R.string.wake_completed_title), 32f))
            root.addView(text(activity.getString(R.string.wake_completed_body), 18f))
        }

        fun showStopped() {
            root.removeAllViews()
            root.addView(text(activity.getString(R.string.wake_emergency_stop), 32f))
            root.addView(text(activity.getString(R.string.wake_stopped_body), 18f))
        }

        fun completeMission() {
            activity.lifecycleScope.launch {
                if (flow.completeMission()) showCompleted()
                else showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence), ::completeMission)
            }
        }

        fun addMission(view: View) {
            val scroll = ScrollView(activity).apply {
                isFillViewport = true
                addView(view, android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                ))
            }
            // On short screens or with large text the mission is taller than the space above the
            // emergency card. The answer keypad and ✓ sit at the bottom of the mission, so after
            // the first layout bring the bottom into view; only the heading scrolls off the top
            // and the person can still scroll back to it.
            scroll.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    val overflow = view.height - scroll.height
                    if (scroll.height <= 0 || view.height <= 0) return
                    scroll.removeOnLayoutChangeListener(this)
                    if (overflow > 0) scroll.post { scroll.scrollTo(0, overflow) }
                }
            })
            root.addView(scroll, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            root.addView(emergency, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        fun addHeader(target: LinearLayout = root) {
            val header = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            (status.parent as? android.view.ViewGroup)?.removeView(status)
            header.addView(status, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val close = Button(activity).apply {
                text = "×"
                textSize = 22f
                setTextColor(AweroDesign.navyArgb)
                minWidth = (44 * resources.displayMetrics.density).toInt()
                minHeight = (44 * resources.displayMetrics.density).toInt()
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xCCFFFFFF.toInt())
                    cornerRadius = 12 * resources.displayMetrics.density
                }
                contentDescription = activity.getString(R.string.wake_emergency_stop)
                setOnClickListener { emergency.performClick() }
            }
            header.addView(close)
            target.addView(header)
        }

        fun showFallback() {
            activity.lifecycleScope.launch {
                if (!flow.fallbackToMath()) {
                    showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence), ::showFallback)
                    return@launch
                }
                root.removeAllViews()
                status.text = "AWERO"
                addHeader()
                addMission(MissionRuntimeScreen.create(
                    activity,
                    flow.currentAlarm.value!!.copy(missionType = app.awero.core.alarm.MissionType.MATH),
                    onSuccess = ::completeMission,
                    onFailure = {}
                ))
            }
        }

        fun showMission() {
            root.removeAllViews()
            status.text = "AWERO"
            addHeader()
            val missionView = MissionRuntimeScreen.create(
                activity,
                flow.currentAlarm.value!!.copy(missionType = flow.mission.value),
                onSuccess = ::completeMission,
                onFailure = ::showFallback
            )
            addMission(missionView)
        }

        primary.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.state.value == WakeFlowController.State.RINGING) {
                    if (flow.beginMission()) showMission()
                    else showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence))
                }
            }
        }

        snooze.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.snooze()) {
                    title.text = activity.getString(R.string.wake_snoozed_title)
                    status.text = activity.getString(R.string.wake_snoozed_body)
                    primary.isEnabled = false
                    snooze.isEnabled = false
                } else {
                    status.text = flow.snoozeError.value ?: activity.getString(R.string.wake_snooze_error_body)
                }
            }
        }

        emergency.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.emergencyStop()) {
                    showStopped()
                } else {
                    showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence))
                }
            }
        }

        val startContent = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        addHeader(startContent)
        startContent.addView(title)
        startContent.addView(text(activity.getString(R.string.wake_instruction), 17f))
        startContent.addView(primary, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            val gap = (8 * activity.resources.displayMetrics.density).toInt()
            setMargins(0, gap, 0, gap)
        })
        startContent.addView(snooze, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val startScroll = ScrollView(activity).apply { isFillViewport = true; addView(startContent) }
        root.addView(startScroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(emergency, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        when (flow.state.value) {
            WakeFlowController.State.MISSION -> showMission()
            WakeFlowController.State.COMPLETED -> showCompleted()
            WakeFlowController.State.EMERGENCY_STOPPED -> showStopped()
            else -> Unit
        }
        return root
    }
}
