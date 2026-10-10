package app.awero.core.wake

import app.awero.R
import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.AlarmRingingService
import app.awero.core.alarm.AlarmStore
import app.awero.core.alarm.MissionType
import app.awero.core.missions.MissionFallbackPolicy
import app.awero.core.statistics.StatisticsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WakeFlowController(
    private val sessions: WakeSessionStore,
    private val context: Context,
    private val statistics: StatisticsStore = StatisticsStore(context),
    private val testAlarm: Boolean = false,
    private val alarmStore: AlarmStore = AlarmStore(context),
    private val scheduleAlarmSnooze: (Alarm, Int, Boolean) -> Unit = { alarm, minutes, isTest ->
        AlarmScheduler(context).scheduleSnooze(alarm, minutes, isTest)
    },
    private val cancelAlarmSnooze: (Alarm, Boolean) -> Unit = { alarm, isTest ->
        AlarmScheduler(context).cancelSnooze(alarm, isTest)
    },
    private val completeSession: suspend () -> WakeSession? = { sessions.complete() }
) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }

    private val _state = MutableStateFlow(State.IDLE)
    private val _mission = MutableStateFlow(MissionType.MATH)
    private val _snoozeCount = MutableStateFlow(0)
    private val _snoozeError = MutableStateFlow<String?>(null)
    private val _actionError = MutableStateFlow<String?>(null)
    private val _currentAlarm = MutableStateFlow<Alarm?>(null)
    private var snoozeInProgress = false
    private var activeTestAlarm = testAlarm

    val state: StateFlow<State> = _state.asStateFlow()
    val mission: StateFlow<MissionType> = _mission.asStateFlow()
    val snoozeCount: StateFlow<Int> = _snoozeCount.asStateFlow()
    val snoozeError: StateFlow<String?> = _snoozeError.asStateFlow()
    val actionError: StateFlow<String?> = _actionError.asStateFlow()
    val currentAlarm: StateFlow<Alarm?> = _currentAlarm.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    suspend fun restore(alarmId: String? = null, testAlarm: Boolean? = null) {
        val session = sessions.loadActive(alarmId, testAlarm) ?: return
        val alarm = alarmStore.get(session.alarmId) ?: return
        activeTestAlarm = session.isTest
        _currentAlarm.value = alarm
        _mission.value = if (session.fallbackUsed) MissionType.MATH else alarm.missionType
        _snoozeCount.value = session.snoozeCount
        _state.value = if (session.missionStartedAt != null) State.MISSION else State.RINGING
    }

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()) {
        val created = sessions.start(alarm, scheduledAt, testAlarm)
        if (!created) {
            restore(alarm.id, testAlarm)
            return
        }
        activeTestAlarm = testAlarm
        _currentAlarm.value = alarm
        _mission.value = alarm.missionType
        _snoozeCount.value = 0
        if (!activeTestAlarm) statistics.recordPlanned()
        _state.value = State.RINGING
    }

    suspend fun beginMission(): Boolean {
        if (_state.value != State.RINGING) return false
        _actionError.value = null
        val started = runCatching { sessions.startMission() }.getOrDefault(false)
        if (!started) {
            _actionError.value = context.getString(R.string.wake_error_mission_save)
            return false
        }
        _state.value = State.MISSION
        return true
    }

    suspend fun fallbackToMath(): Boolean {
        if (_state.value != State.MISSION) return false
        val fallback = MissionFallbackPolicy.nextAfter(_mission.value) ?: return false
        _actionError.value = null
        val saved = runCatching { sessions.markFallback() }.getOrDefault(false)
        if (!saved) {
            _actionError.value = context.getString(R.string.wake_error_fallback_save)
            return false
        }
        _mission.value = fallback
        return true
    }

    suspend fun completeMission(): Boolean {
        if (_state.value != State.MISSION) return false
        _actionError.value = null
        val session = runCatching { completeSession() }.getOrNull()
        if (session == null) {
            _actionError.value = context.getString(R.string.wake_error_completion_save)
            return false
        }
        if (!session.isTest) runCatching { statistics.record(session) }
        stopCurrentRing()
        _state.value = State.COMPLETED
        return true
    }

    suspend fun snooze(): Boolean {
        val alarm = _currentAlarm.value ?: return false
        if (_state.value != State.RINGING || _snoozeCount.value >= alarm.maxSnoozes) return false
        if (snoozeInProgress) return false
        snoozeInProgress = true
        try {
            val previousCount = _snoozeCount.value
            val nextCount = previousCount + 1
            _snoozeError.value = null
            try {
                scheduleAlarmSnooze(alarm, alarm.snoozeMinutes, activeTestAlarm)
                if (!sessions.setSnoozeCount(nextCount)) {
                    runCatching { cancelAlarmSnooze(alarm, activeTestAlarm) }
                    _snoozeError.value = context.getString(R.string.wake_error_snooze_save)
                    return false
                }
            } catch (error: Exception) {
                runCatching { cancelAlarmSnooze(alarm, activeTestAlarm) }
                _snoozeError.value = error.message ?: context.getString(R.string.wake_error_snooze_schedule)
                return false
            }
            _snoozeCount.value = nextCount
            stopCurrentRing()
            _state.value = State.IDLE
            return true
        } finally {
            snoozeInProgress = false
        }
    }

    suspend fun emergencyStop(): Boolean {
        _actionError.value = null
        val session = runCatching { sessions.emergencyStop() }.getOrNull()
        if (session == null) {
            _actionError.value = context.getString(R.string.wake_error_stop_save)
            return false
        }
        if (!session.isTest) runCatching { statistics.record(session) }
        stopCurrentRing()
        _state.value = State.EMERGENCY_STOPPED
        return true
    }

    private fun stopCurrentRing() {
        _currentAlarm.value?.let { alarm ->
            AlarmRingingService.stop(context, alarm.id, activeTestAlarm)
        }
    }
}
