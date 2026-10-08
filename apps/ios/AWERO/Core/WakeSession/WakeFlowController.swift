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
    private var scheduler: AlarmScheduler?
    private let database: CoreDataStore
    private let statistics: StatisticsStore
    private(set) var currentAlarm: Alarm?
    private var maxSnoozes = 3

    init(
        sessionManager: WakeSessionManager? = nil,
        scheduler: AlarmScheduler? = nil,
        database: CoreDataStore = .shared
    ) {
        self.sessionManager = sessionManager ?? WakeSessionManager(database: database)
        self.scheduler = scheduler
        self.database = database
        self.statistics = StatisticsStore(database: database)
    }

    func restore() async {
        guard let session = await sessionManager.restore(),
              let alarm = await database.fetchAlarm(id: session.alarmId) else { return }
        currentAlarm = alarm
        currentMission = session.fallbackUsed ? .math : session.missionType
        maxSnoozes = alarm.maxSnoozes
        snoozeCount = session.snoozeCount
        state = session.missionStartedAt == nil ? .ringing : .mission
    }

    func start(alarmID: UUID, scheduledAt: Date = .now) async {
        guard let alarm = await database.fetchAlarm(id: alarmID), alarm.enabled else { return }
        await start(alarm: alarm, scheduledAt: scheduledAt)
    }

    func start(alarm: Alarm, scheduledAt: Date = .now) async {
        guard await sessionManager.trigger(alarm: alarm, scheduledAt: scheduledAt) else {
            await restore()
            return
        }
        currentAlarm = alarm
        currentMission = alarm.missionType
        maxSnoozes = alarm.maxSnoozes
        snoozeCount = 0
        await statistics.recordPlanned()
        state = .ringing
    }

    func beginMission() async {
        guard state == .ringing else { return }
        guard await sessionManager.startMission() else { return }
        state = .mission
    }

    func fallbackToMath() async {
        guard state == .mission else { return }
        guard await sessionManager.markFallback() else { return }
        currentMission = .math
    }

    func completeMission() async {
        guard state == .mission else { return }
        guard let session = await sessionManager.complete() else { return }
        await statistics.record(session)
        state = .completed
    }

    func snooze() async {
        guard let alarm = currentAlarm, state == .ringing, snoozeCount < maxSnoozes else { return }
        let nextSnoozeCount = snoozeCount + 1
        guard await sessionManager.setSnoozeCount(nextSnoozeCount) else { return }
        snoozeCount = nextSnoozeCount
        let scheduler = scheduler ?? AlarmScheduler()
        self.scheduler = scheduler
        Task {
            try? await scheduler.scheduleSnooze(for: alarm)
        }
        state = .idle
    }

    func emergencyStop() async {
        guard let session = await sessionManager.emergencyStop() else { return }
        await statistics.record(session)
        state = .emergencyStopped
    }
}
