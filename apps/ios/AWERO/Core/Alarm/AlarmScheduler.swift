import Foundation
import UserNotifications
#if canImport(AlarmKit) && canImport(AppIntents)
import AlarmKit
#endif

enum AlarmReadiness: Equatable {
    case disabled
    case scheduled
    case notificationFallback
    case actionRequired
    case notScheduled

    var localizationKey: String {
        switch self {
        case .disabled: "alarm.status.disabled"
        case .scheduled: "alarm.status.scheduled"
        case .notificationFallback: "alarm.status.notificationFallback"
        case .actionRequired: "alarm.status.actionRequired"
        case .notScheduled: "alarm.status.notScheduled"
        }
    }
}

enum AlarmSchedulingError: LocalizedError, Equatable {
    case invalidTimezone
    case alarmAuthorizationDenied
    case noWeekdaysSelected
    case invalidAlarmTime
    case invalidWeekday
    case notificationScheduleIncomplete

    var errorDescription: String? {
        switch self {
        case .invalidTimezone:
            return String(localized: "alarm.error.invalid_timezone")
        case .alarmAuthorizationDenied:
            return String(localized: "alarm.error.permission_required")
        case .noWeekdaysSelected:
            return String(localized: "alarm.error.weekdays_required")
        case .invalidAlarmTime:
            return String(localized: "alarm.error.invalid_time")
        case .invalidWeekday:
            return String(localized: "alarm.error.invalid_weekday")
        case .notificationScheduleIncomplete:
            return String(localized: "alarm.error.schedule_incomplete")
        }
    }
}

protocol AlarmNotificationCenter {
    func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool
    func add(_ request: UNNotificationRequest) async throws
    func pendingNotificationRequests() async -> [UNNotificationRequest]
    func canDeliverAudibleNotifications() async -> Bool
    func removePendingNotificationRequests(withIdentifiers identifiers: [String])
}

private final class SystemAlarmNotificationCenter: AlarmNotificationCenter {
    private let center = UNUserNotificationCenter.current()

    func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool {
        try await center.requestAuthorization(options: options)
    }

    func add(_ request: UNNotificationRequest) async throws {
        try await center.add(request)
    }

    func pendingNotificationRequests() async -> [UNNotificationRequest] {
        await center.pendingNotificationRequests()
    }

    func canDeliverAudibleNotifications() async -> Bool {
        let settings = await center.notificationSettings()
        return settings.authorizationStatus == .authorized &&
            settings.alertSetting == .enabled &&
            settings.soundSetting == .enabled
    }

    func removePendingNotificationRequests(withIdentifiers identifiers: [String]) {
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
    }
}

final class AlarmScheduler {
    private let center: any AlarmNotificationCenter
    private let usesSystemAlarmKit: Bool

    init(center: (any AlarmNotificationCenter)? = nil, usesSystemAlarmKit: Bool? = nil) {
        self.center = center ?? SystemAlarmNotificationCenter()
        self.usesSystemAlarmKit = usesSystemAlarmKit ?? (center == nil)
    }

    static func validate(_ alarm: Alarm) throws {
        guard (0...23).contains(alarm.hour), (0...59).contains(alarm.minute) else {
            throw AlarmSchedulingError.invalidAlarmTime
        }
        guard !alarm.weekdays.isEmpty else {
            throw AlarmSchedulingError.noWeekdaysSelected
        }
        guard alarm.weekdays.allSatisfy({ (1...7).contains($0) }) else {
            throw AlarmSchedulingError.invalidWeekday
        }
        if alarm.timezoneMode == .fixed,
           TimeZone(identifier: alarm.fixedTimezone ?? "") == nil {
            throw AlarmSchedulingError.invalidTimezone
        }
    }

    func requestAuthorization() async throws {
        try await center.requestAuthorization(options: [.alert, .sound, .badge])
    }

    func schedule(_ alarm: Alarm) async throws {
        guard alarm.enabled else {
            await cancel(alarm)
            return
        }
        try Self.validate(alarm)

#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            try await SystemAlarmKitScheduler.schedule(alarm)
            await removeNotifications(for: alarm)
            return
        }
#endif

        await cancel(alarm)
        try await scheduleNotifications(for: alarm)
    }

    func scheduleTest(for alarm: Alarm, after seconds: TimeInterval = 30) async throws {
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            try await SystemAlarmKitScheduler.scheduleTest(for: alarm, after: seconds)
            await removeNotifications(for: alarm, kind: "test")
            return
        }
#endif

        let content = UNMutableNotificationContent()
        content.title = String(localized: "notification.test_title")
        content.body = String(localized: "notification.test_body")
        content.sound = .default
        try await center.add(
            UNNotificationRequest(
                identifier: "awero:test:\(alarm.id.uuidString):\(UUID().uuidString)",
                content: content,
                trigger: UNTimeIntervalNotificationTrigger(timeInterval: max(5, seconds), repeats: false)
            )
        )
    }

    func scheduleSnooze(for alarm: Alarm, isTestAlarm: Bool = false) async throws {
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            try await SystemAlarmKitScheduler.scheduleSnooze(for: alarm, isTestAlarm: isTestAlarm)
            await removeNotifications(for: alarm, kind: "snooze", isTestAlarm: isTestAlarm)
            return
        }
#endif

        let content = UNMutableNotificationContent()
        content.title = String(localized: "notification.snooze_title")
        content.body = String(localized: "notification.snooze_body")
        content.sound = .default
        try await center.add(
            UNNotificationRequest(
                identifier: "awero:snooze:\(alarm.id.uuidString):v\(alarm.version):\(isTestAlarm ? "test" : "real"):\(UUID().uuidString)",
                content: content,
                trigger: UNTimeIntervalNotificationTrigger(
                    timeInterval: TimeInterval(max(1, alarm.snoozeMinutes) * 60),
                    repeats: false
                )
            )
        )
    }

    func cancelSnooze(for alarm: Alarm, isTestAlarm: Bool = false) async {
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            SystemAlarmKitScheduler.cancelSnooze(for: alarm, isTestAlarm: isTestAlarm)
        }
#endif
        await removeNotifications(for: alarm, kind: "snooze", isTestAlarm: isTestAlarm)
    }

    func cancel(_ alarm: Alarm) async {
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *), usesSystemAlarmKit {
            SystemAlarmKitScheduler.cancel(alarm)
        }
#endif
        await removeNotifications(for: alarm)
    }

    func isScheduled(_ alarm: Alarm) async -> Bool {
        guard alarm.enabled && !alarm.weekdays.isEmpty else { return false }
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            return SystemAlarmKitScheduler.isScheduled(alarm)
        }
#endif
        let ids = Set((await center.pendingNotificationRequests()).map(\.identifier))
        return alarm.weekdays.allSatisfy {
            ids.contains("awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w\($0)")
        }
    }

    func readiness(for alarm: Alarm) async -> AlarmReadiness {
        guard alarm.enabled else { return .disabled }
#if canImport(AlarmKit) && canImport(AppIntents)
        if #available(iOS 26.0, *),
           usesSystemAlarmKit,
           alarm.timezoneMode == .deviceLocal {
            guard SystemAlarmKitScheduler.authorizationGranted else { return .actionRequired }
            return SystemAlarmKitScheduler.isScheduled(alarm) ? .scheduled : .notScheduled
        }
#endif
        guard await center.canDeliverAudibleNotifications() else { return .actionRequired }
        return await isScheduled(alarm) ? .notificationFallback : .notScheduled
    }

    func repair(_ alarm: Alarm) async throws {
        try await schedule(alarm)
    }

    private func scheduleNotifications(for alarm: Alarm) async throws {
        let alarmPrefix = "awero:alarm:\(alarm.id.uuidString):"
        let pendingBeforeScheduling = await center.pendingNotificationRequests()
        let previousAlarmRequests = pendingBeforeScheduling.filter {
            $0.identifier.hasPrefix("awero:alarm:") && !$0.identifier.hasPrefix(alarmPrefix)
        }

        do {
            for day in alarm.weekdays.sorted() {
                var components = DateComponents()
                components.calendar = Calendar(identifier: .gregorian)
                components.weekday = day
                components.hour = alarm.hour
                components.minute = alarm.minute
                if alarm.timezoneMode == .fixed {
                    guard let timezone = TimeZone(identifier: alarm.fixedTimezone ?? "") else {
                        throw AlarmSchedulingError.invalidTimezone
                    }
                    components.timeZone = timezone
                }

                let content = UNMutableNotificationContent()
                content.title = String(localized: "notification.alarm_title")
                content.body = String(localized: "notification.alarm_body")
                content.sound = .default

                let id = "awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w\(day)"
                try await center.add(
                    UNNotificationRequest(
                        identifier: id,
                        content: content,
                        trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
                    )
                )
            }

            let pendingIDs = Set((await center.pendingNotificationRequests()).map(\.identifier))
            let expectedIDs = alarm.weekdays.map {
                "awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w\($0)"
            }
            let previouslyScheduledIDs = previousAlarmRequests.map(\.identifier)
            guard (expectedIDs + previouslyScheduledIDs).allSatisfy(pendingIDs.contains) else {
                throw AlarmSchedulingError.notificationScheduleIncomplete
            }
        } catch {
            await removeNotifications(for: alarm)
            let pendingIDs = Set((await center.pendingNotificationRequests()).map(\.identifier))
            for request in previousAlarmRequests where !pendingIDs.contains(request.identifier) {
                try? await center.add(request)
            }
            throw error
        }
    }

    private func removeNotifications(
        for alarm: Alarm,
        kind: String? = nil,
        isTestAlarm: Bool? = nil
    ) async {
        let requests = await center.pendingNotificationRequests()
        let prefixes: [String]
        if let kind {
            prefixes = ["awero:\(kind):\(alarm.id.uuidString):"]
        } else {
            prefixes = [
                "awero:alarm:\(alarm.id.uuidString):",
                "awero:test:\(alarm.id.uuidString):",
                "awero:snooze:\(alarm.id.uuidString):"
            ]
        }
        let ids = requests.map(\.identifier).filter { id in
            if let isTestAlarm, kind == "snooze" {
                let marker = isTestAlarm ? ":test:" : ":real:"
                return id.hasPrefix("awero:snooze:\(alarm.id.uuidString):") && id.contains(marker)
            }
            return prefixes.contains { id.hasPrefix($0) }
        }
        center.removePendingNotificationRequests(withIdentifiers: ids)
    }
}
