package app.awero.ui

import android.graphics.Color
import app.awero.R
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
            setBackgroundColor(AweroDesign.ivoryArgb)
        }

        fun text(value: String, size: Float) = TextView(activity).apply {
            text = value
            textSize = size
            setTextColor(AweroDesign.navyArgb)
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val title = text(activity.getString(R.string.wake_title), 42f)
        val status = text("AWERO", 14f)
        val primary = Button(activity).apply { text = activity.getString(R.string.wake_start); minHeight = (48 * resources.displayMetrics.density).toInt(); backgroundTintList = android.content.res.ColorStateList.valueOf(AweroDesign.coralArgb); setTextColor(AweroDesign.navyArgb) }
        val snooze = Button(activity).apply { text = activity.getString(R.string.wake_snooze) }
        val emergency = Button(activity).apply { text = activity.getString(R.string.wake_emergency_stop) }
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
            root.addView(text(activity.getString(R.string.wake_completed_title), 42f))
            root.addView(text(activity.getString(R.string.wake_completed_body), 18f))
        }

        fun showStopped() {
            root.removeAllViews()
            root.addView(text(activity.getString(R.string.wake_emergency_stop), 42f))
            root.addView(text(activity.getString(R.string.wake_stopped_body), 18f))
        }

        fun completeMission() {
            activity.lifecycleScope.launch {
                if (flow.completeMission()) showCompleted()
                else showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence), ::completeMission)
            }
        }

        fun showFallback() {
            activity.lifecycleScope.launch {
                if (!flow.fallbackToMath()) {
                    showError(flow.actionError.value ?: activity.getString(R.string.alarm_error_persistence), ::showFallback)
                    return@launch
                }
                root.removeAllViews()
                status.text = activity.getString(R.string.mission_use_math)
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
            status.text = "AWERO"
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
