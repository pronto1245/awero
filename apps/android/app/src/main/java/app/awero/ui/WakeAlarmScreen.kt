package app.awero.ui

import android.graphics.Color
import app.awero.R
import android.view.Gravity
import android.view.View
import android.widget.Button
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
            setPadding(0, 20, 0, 20)
        }

        val title = text(activity.getString(R.string.wake_heading), 32f)
        val status = text("AWERO", 20f).apply {
            gravity = Gravity.START
            setPadding(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
        }
        val primary = Button(activity).apply { text = activity.getString(R.string.wake_start); minHeight = (48 * resources.displayMetrics.density).toInt(); backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb); setTextColor(android.graphics.Color.WHITE) }
        val snooze = Button(activity).apply { text = activity.getString(R.string.wake_snooze) }
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
                addView(TextView(activity).apply {
                    text = "⚠"
                    setTextColor(0xFFD3313D.toInt())
                    textSize = 15f
                    setPadding(0, 0, (6 * resources.displayMetrics.density).toInt(), 0)
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
        val retry = Button(activity).apply { text = activity.getString(R.string.wake_retry) }

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
            root.addView(scroll, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ))
            root.addView(emergency, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        fun addHeader() {
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
            root.addView(header)
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

        addHeader()
        root.addView(title)
        root.addView(primary)
        root.addView(snooze)
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
