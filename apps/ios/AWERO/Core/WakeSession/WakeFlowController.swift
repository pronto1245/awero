import Foundation
import Combine

@MainActor
final class WakeFlowController: ObservableObject {
    static let shared = WakeFlowController()
    enum State: Equatable { case idle, ringing, mission, completed, emergencyStopped }
    @Published private(set) var state: State = .idle
    @Published private(set) var currentMission: MissionType = .math
    @Published private(set) var snoozeCount = 0

    private let sessionManager: WakeSessionManager
    private let scheduler: AlarmScheduler
    private let statistics = StatisticsStore()
    private(set) var currentAlarm: Alarm?
    private var maxSnoozes = 3

    init(
        sessionManager: WakeSessionManager? = nil,
        scheduler: AlarmScheduler? = nil
    ) {
        self.sessionManager = sessionManager ?? WakeSessionManager()
        self.scheduler = scheduler ?? AlarmScheduler()
    }

    func start(alarm: Alarm, scheduledAt: Date = .now) async {
        currentAlarm = alarm
        currentMission = alarm.missionType
        maxSnoozes = alarm.maxSnoozes
        snoozeCount = 0
        let created = await sessionManager.trigger(alarm: alarm, scheduledAt: scheduledAt)
        if created { await statistics.recordPlanned() }
        state = .ringing
    }

    func beginMission() async {
        guard state == .ringing else { return }
        state = .mission
        await sessionManager.startMission()
    }

    func fallbackToMath() async {
        guard state == .mission else { return }
        currentMission = .math
        await sessionManager.markFallback()
    }

    func completeMission() async {
        guard state == .mission else { return }
        await sessionManager.complete()
        if let session = sessionManager.current { await statistics.record(session) }
        state = .completed
    }

    func snooze() async {
        guard let alarm = currentAlarm, state == .ringing, snoozeCount < maxSnoozes else { return }
        snoozeCount += 1
        await sessionManager.setSnoozeCount(snoozeCount)
        Task {
            try? await scheduler.scheduleSnooze(for: alarm)
        }
        state = .idle
    }

    func emergencyStop() async {
        await sessionManager.emergencyStop()
        if let session = sessionManager.current { await statistics.record(session) }
        state = .emergencyStopped
    }
}
