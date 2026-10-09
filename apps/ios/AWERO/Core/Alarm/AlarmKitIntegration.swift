#if canImport(AlarmKit) && canImport(AppIntents)
import AlarmKit
import AppIntents
import CryptoKit
import Foundation
import SwiftUI

@available(iOS 26.0, *)
struct AweroAlarmMetadata: AlarmMetadata {}

@available(iOS 26.0, *)
struct StartAweroMissionIntent: LiveActivityIntent {
    static var title: LocalizedStringResource = "Start mission"
    static var openAppWhenRun: Bool = true

    @Parameter(title: "Alarm ID") var alarmID: String
    @Parameter(title: "Test alarm") var isTestAlarm: Bool

    init() {
        alarmID = ""
        isTestAlarm = false
    }

    init(alarmID: String, isTestAlarm: Bool = false) {
        self.alarmID = alarmID
        self.isTestAlarm = isTestAlarm
    }

    @MainActor
    static func trigger(alarmID: String, isTestAlarm: Bool = false, using controller: WakeFlowController) async {
        guard let id = UUID(uuidString: alarmID) else { return }
        await controller.start(alarmID: id, isTestAlarm: isTestAlarm)
        await controller.beginMission()
    }

    func perform() async throws -> some IntentResult {
        await Self.trigger(alarmID: alarmID, isTestAlarm: isTestAlarm, using: .shared)
        return .result()
    }
}

@available(iOS 26.0, *)
enum SystemAlarmKitScheduler {
    static var authorizationGranted: Bool {
        if case .authorized = AlarmManager.shared.authorizationState { return true }
        return false
    }

    static func schedule(_ alarm: Alarm) async throws {
        try await ensureAuthorization()

        let alarmID = identifier(for: "alarm:\(alarm.id.uuidString):v\(alarm.version)")
        try? AlarmManager.shared.cancel(id: alarmID)

        let weekdays = alarm.weekdays.sorted().compactMap(localeWeekday)
        let time = AlarmKit.Alarm.Schedule.Relative.Time(hour: alarm.hour, minute: alarm.minute)
        let schedule = AlarmKit.Alarm.Schedule.relative(
            .init(time: time, repeats: .weekly(weekdays))
        )
        let configuration = configuration(
            schedule: schedule,
            alarmID: alarm.id
        )
        try await AlarmManager.shared.schedule(id: alarmID, configuration: configuration)

        if alarm.version > 1 {
            for version in 1..<alarm.version {
                let oldID = identifier(for: "alarm:\(alarm.id.uuidString):v\(version)")
                try? AlarmManager.shared.cancel(id: oldID)
            }
        }
    }

    static func scheduleTest(for alarm: Alarm, after seconds: TimeInterval) async throws {
        try await ensureAuthorization()
        let id = identifier(for: "test:\(alarm.id.uuidString)")
        try? AlarmManager.shared.cancel(id: id)
        let schedule = AlarmKit.Alarm.Schedule.fixed(Date(timeIntervalSinceNow: max(5, seconds)))
        try await AlarmManager.shared.schedule(
            id: id,
            configuration: configuration(schedule: schedule, alarmID: alarm.id, isTestAlarm: true)
        )
    }

    static func scheduleSnooze(for alarm: Alarm, isTestAlarm: Bool = false) async throws {
        try await ensureAuthorization()
        let id = identifier(for: snoozeKey(alarm, isTestAlarm: isTestAlarm))
        try? AlarmManager.shared.cancel(id: id)
        let schedule = AlarmKit.Alarm.Schedule.fixed(
            Date(timeIntervalSinceNow: TimeInterval(max(1, alarm.snoozeMinutes) * 60))
        )
        try await AlarmManager.shared.schedule(
            id: id,
            configuration: configuration(schedule: schedule, alarmID: alarm.id, isTestAlarm: isTestAlarm)
        )
    }

    static func cancelSnooze(for alarm: Alarm, isTestAlarm: Bool = false) {
        try? AlarmManager.shared.cancel(id: identifier(for: snoozeKey(alarm, isTestAlarm: isTestAlarm)))
    }

    static func cancel(_ alarm: Alarm) {
        let lastVersion = max(1, alarm.version)
        for version in 1...lastVersion {
            let id = identifier(for: "alarm:\(alarm.id.uuidString):v\(version)")
            try? AlarmManager.shared.cancel(id: id)
        }
        try? AlarmManager.shared.cancel(id: identifier(for: "test:\(alarm.id.uuidString)"))
        cancelSnooze(for: alarm)
        cancelSnooze(for: alarm, isTestAlarm: true)
    }

    static func isScheduled(_ alarm: Alarm) -> Bool {
        let id = identifier(for: "alarm:\(alarm.id.uuidString):v\(alarm.version)")
        do {
            return try AlarmManager.shared.alarms.contains { $0.id == id }
        } catch {
            return false
        }
    }

    private static func configuration(
        schedule: AlarmKit.Alarm.Schedule,
        alarmID: UUID,
        isTestAlarm: Bool = false
    ) -> AlarmManager.AlarmConfiguration<AweroAlarmMetadata> {
        let stopButton = AlarmButton(
            text: "Start mission",
            textColor: .white,
            systemImageName: "alarm.fill"
        )
        let alert = AlarmPresentation.Alert(title: "AWERO", stopButton: stopButton)
        let attributes = AlarmAttributes<AweroAlarmMetadata>(
            presentation: AlarmPresentation(alert: alert),
            metadata: nil,
            tintColor: .orange
        )
        return .alarm(
            schedule: schedule,
            attributes: attributes,
            stopIntent: StartAweroMissionIntent(alarmID: alarmID.uuidString, isTestAlarm: isTestAlarm),
            secondaryIntent: nil,
            sound: .default
        )
    }

    private static func snoozeKey(_ alarm: Alarm, isTestAlarm: Bool) -> String {
        "snooze:\(isTestAlarm ? "test:" : "")\(alarm.id.uuidString)"
    }

    private static func ensureAuthorization() async throws {
        switch AlarmManager.shared.authorizationState {
        case .authorized:
            return
        case .notDetermined:
            let state = try await AlarmManager.shared.requestAuthorization()
            guard state == .authorized else {
                throw AlarmSchedulingError.alarmAuthorizationDenied
            }
        case .denied:
            throw AlarmSchedulingError.alarmAuthorizationDenied
        @unknown default:
            throw AlarmSchedulingError.alarmAuthorizationDenied
        }
    }

    private static func localeWeekday(_ day: Int) -> Locale.Weekday? {
        switch day {
        case 1: .sunday
        case 2: .monday
        case 3: .tuesday
        case 4: .wednesday
        case 5: .thursday
        case 6: .friday
        case 7: .saturday
        default: nil
        }
    }

    private static func identifier(for value: String) -> UUID {
        let bytes = Array(SHA256.hash(data: Data(value.utf8)).prefix(16))
        return UUID(uuid: (
            bytes[0], bytes[1], bytes[2], bytes[3],
            bytes[4], bytes[5], bytes[6], bytes[7],
            bytes[8], bytes[9], bytes[10], bytes[11],
            bytes[12], bytes[13], bytes[14], bytes[15]
        ))
    }
}
#endif
