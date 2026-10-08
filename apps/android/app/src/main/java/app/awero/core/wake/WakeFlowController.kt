package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmScheduler
import app.awero.core.alarm.AlarmRingingService
import app.awero.core.alarm.AlarmStore
import app.awero.core.alarm.MissionType
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
    private val scheduleAlarmSnooze: (Alarm, Int) -> Unit = { alarm, minutes ->
        AlarmScheduler(context).scheduleSnooze(alarm, minutes)
    },
    private val cancelAlarmSnooze: (Alarm) -> Unit = { alarm ->
        AlarmScheduler(context).cancelSnooze(alarm)
    }
) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }

    private val _state = MutableStateFlow(State.IDLE)
    private val _mission = MutableStateFlow(MissionType.MATH)
    private val _snoozeCount = MutableStateFlow(0)
    private val _snoozeError = MutableStateFlow<String?>(null)
    private val _currentAlarm = MutableStateFlow<Alarm?>(null)

    val state: StateFlow<State> = _state.asStateFlow()
    val mission: StateFlow<MissionType> = _mission.asStateFlow()
    val snoozeCount: StateFlow<Int> = _snoozeCount.asStateFlow()
    val snoozeError: StateFlow<String?> = _snoozeError.asStateFlow()
    val currentAlarm: StateFlow<Alarm?> = _currentAlarm.asStateFlow()

    suspend fun restore() {
        val session = sessions.loadActive() ?: return
        val alarm = alarmStore.get(session.alarmId) ?: return
        _currentAlarm.value = alarm
        _mission.value = if (session.fallbackUsed) MissionType.MATH else alarm.missionType
        _snoozeCount.value = session.snoozeCount
        _state.value = if (session.missionStartedAt != null) State.MISSION else State.RINGING
    }

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()) {
        val created = sessions.start(alarm, scheduledAt)
        if (!created) {
            restore()
            return
        }
        _currentAlarm.value = alarm
        _mission.value = alarm.missionType
        _snoozeCount.value = 0
        if (!testAlarm) statistics.recordPlanned()
        _state.value = State.RINGING
    }

    suspend fun beginMission() {
        if (_state.value == State.RINGING) {
            sessions.startMission()
            _state.value = State.MISSION
        }
    }

    suspend fun fallbackToMath() {
        if (_state.value == State.MISSION) {
            sessions.markFallback()
            _mission.value = MissionType.MATH
        }
    }

    suspend fun completeMission() {
        if (_state.value == State.MISSION) {
            sessions.complete()?.let { if (!testAlarm) statistics.record(it) }
            AlarmRingingService.stop(context)
            _state.value = State.COMPLETED
        }
    }

    suspend fun snooze(): Boolean {
        val alarm = _currentAlarm.value ?: return false
        if (_state.value != State.RINGING || _snoozeCount.value >= alarm.maxSnoozes) return false
        val previousCount = _snoozeCount.value
        val nextCount = previousCount + 1
        _snoozeError.value = null
        try {
            scheduleAlarmSnooze(alarm, alarm.snoozeMinutes)
            sessions.setSnoozeCount(nextCount)
        } catch (error: Exception) {
            runCatching { cancelAlarmSnooze(alarm) }
            runCatching { sessions.setSnoozeCount(previousCount) }
            _snoozeError.value = error.message ?: "Could not schedule snooze. The alarm is still ringing."
            return false
        }
        _snoozeCount.value = nextCount
        AlarmRingingService.stop(context)
        _state.value = State.IDLE
        return true
    }

    suspend fun emergencyStop() {
        sessions.emergencyStop()?.let { if (!testAlarm) statistics.record(it) }
        AlarmRingingService.stop(context)
        _state.value = State.EMERGENCY_STOPPED
    }
}
