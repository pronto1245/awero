package app.awero.core.wake

import android.content.Context
import app.awero.core.alarm.Alarm
import app.awero.core.alarm.AlarmScheduler
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
    private val testAlarm: Boolean = false
) {
    enum class State { IDLE, RINGING, MISSION, COMPLETED, EMERGENCY_STOPPED }

    private val alarmStore = AlarmStore(context)
    private val _state = MutableStateFlow(State.IDLE)
    private val _mission = MutableStateFlow(MissionType.MATH)
    private val _snoozeCount = MutableStateFlow(0)
    private val _currentAlarm = MutableStateFlow<Alarm?>(null)

    val state: StateFlow<State> = _state.asStateFlow()
    val mission: StateFlow<MissionType> = _mission.asStateFlow()
    val snoozeCount: StateFlow<Int> = _snoozeCount.asStateFlow()
    val currentAlarm: StateFlow<Alarm?> = _currentAlarm.asStateFlow()

    suspend fun restore() {
        val session = sessions.loadActive() ?: return
        val alarm = alarmStore.get(session.alarmId) ?: return
        _currentAlarm.value = alarm
        _mission.value = if (session.fallbackUsed) MissionType.MATH else session.missionType
        _snoozeCount.value = session.snoozeCount
        _state.value = if (session.missionStartedAt != null) State.MISSION else State.RINGING
    }

    suspend fun start(alarm: Alarm, scheduledAt: Long = System.currentTimeMillis()) {
        _currentAlarm.value = alarm
        _mission.value = alarm.missionType
        _snoozeCount.value = 0
        val created = sessions.start(alarm, scheduledAt)
        if (created && !testAlarm) statistics.recordPlanned()
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
            _state.value = State.COMPLETED
        }
    }

    suspend fun snooze(): Boolean {
        val alarm = _currentAlarm.value ?: return false
        if (_state.value != State.RINGING || _snoozeCount.value >= alarm.maxSnoozes) return false
        val nextCount = _snoozeCount.value + 1
        _snoozeCount.value = nextCount
        sessions.setSnoozeCount(nextCount)
        AlarmScheduler(context).scheduleSnooze(alarm, alarm.snoozeMinutes)
        _state.value = State.IDLE
        return true
    }

    suspend fun emergencyStop() {
        sessions.emergencyStop()?.let { if (!testAlarm) statistics.record(it) }
        _state.value = State.EMERGENCY_STOPPED
    }
}
