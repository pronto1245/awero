package app.awero.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.awero.R
import app.awero.core.wake.WakeFlowController
import kotlinx.coroutines.launch

/**
 * Native alarm-host presentation used by [app.awero.core.alarm.WakeAlarmActivity].
 * All copy comes from localized resources; colors follow the approved AWERO palette.
 */
object WakeAlarmScreen {
    private val Ivory = Color.rgb(255, 248, 239)
    private val Navy = Color.rgb(20, 41, 75)
    private val NavyMuted = Color.argb(166, 20, 41, 75)
    private val ButtonCoral = Color.rgb(224, 80, 47)
    private val Danger = Color.rgb(196, 54, 54)

    fun create(activity: ComponentActivity, flow: WakeFlowController): LinearLayout {
        fun s(id: Int) = activity.getString(id)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Ivory)
        }

        fun text(value: String, size: Float, color: Int = Navy, bold: Boolean = false) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 20, 0, 20)
        }

        fun primaryButton(label: String) = Button(activity).apply {
            text = label
            isAllCaps = false
            textSize = 18f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(ButtonCoral)
            minHeight = 144
        }

        fun quietButton(label: String, color: Int) = Button(activity).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(color)
            setBackgroundColor(Color.TRANSPARENT)
        }

        val title = text(s(R.string.wake_title), 42f, bold = true)
        val status = text(s(R.string.app_name), 14f, NavyMuted)
        val primary = primaryButton(s(R.string.wake_start))
        val snooze = quietButton(s(R.string.wake_snooze), Navy)
        val emergency = quietButton(s(R.string.wake_emergency_stop), Danger)
        val retry = primaryButton(s(R.string.wake_retry))

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
            root.addView(text(s(R.string.wake_completed_title), 42f, bold = true))
            root.addView(text(s(R.string.wake_completed_body), 18f, NavyMuted))
        }

        fun showStopped() {
            root.removeAllViews()
            root.addView(text(s(R.string.wake_emergency_stop), 36f, bold = true))
            root.addView(text(s(R.string.wake_stopped_body), 18f, NavyMuted))
        }

        fun completeMission() {
            activity.lifecycleScope.launch {
                if (flow.completeMission()) showCompleted()
                else showError(flow.actionError.value ?: s(R.string.wake_error_completion_save), ::completeMission)
            }
        }

        fun showFallback() {
            activity.lifecycleScope.launch {
                if (!flow.fallbackToMath()) {
                    showError(flow.actionError.value ?: s(R.string.wake_error_fallback_save), ::showFallback)
                    return@launch
                }
                root.removeAllViews()
                status.text = s(R.string.wake_fallback_label)
                root.addView(status)
                root.addView(MissionRuntimeScreen.create(
                    activity,
                    flow.currentAlarm.value!!.copy(missionType = app.awero.core.alarm.MissionType.MATH),
                    onSuccess = ::completeMission,
                    onFailure = {}
                ))
                root.addView(emergency)
            }
        }

        fun showMission() {
            root.removeAllViews()
            status.text = s(R.string.app_name)
            root.addView(status)
            val missionView = MissionRuntimeScreen.create(
                activity,
                flow.currentAlarm.value!!.copy(missionType = flow.mission.value),
                onSuccess = ::completeMission,
                onFailure = ::showFallback
            )
            root.addView(missionView)
            root.addView(emergency)
        }

        primary.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.state.value == WakeFlowController.State.RINGING) {
                    if (flow.beginMission()) showMission()
                    else showError(flow.actionError.value ?: s(R.string.wake_error_mission_save))
                }
            }
        }

        snooze.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.snooze()) {
                    title.text = s(R.string.wake_snoozed_title)
                    status.text = s(R.string.wake_snoozed_body)
                    primary.isEnabled = false
                    snooze.isEnabled = false
                } else {
                    status.text = flow.snoozeError.value ?: s(R.string.wake_error_snooze_schedule)
                }
            }
        }

        emergency.setOnClickListener {
            activity.lifecycleScope.launch {
                if (flow.emergencyStop()) {
                    showStopped()
                } else {
                    showError(flow.actionError.value ?: s(R.string.wake_error_stop_save))
                }
            }
        }

        root.addView(status)
        root.addView(title)
        root.addView(primary)
        root.addView(snooze)
        root.addView(emergency)
        when (flow.state.value) {
            WakeFlowController.State.MISSION -> showMission()
            WakeFlowController.State.COMPLETED -> showCompleted()
            WakeFlowController.State.EMERGENCY_STOPPED -> showStopped()
            else -> Unit
        }
        return root
    }
}
