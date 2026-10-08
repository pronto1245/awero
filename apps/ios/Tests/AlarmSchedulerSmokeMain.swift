import Foundation
import UserNotifications

private enum SmokeFailure: Error { case expected }

private final class RecordingNotificationCenter: AlarmNotificationCenter {
    var requests: [String: UNNotificationRequest] = [:]
    var canDeliver = true
    var addCount = 0
    var failOnAdd: Int?

    func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool { true }

    func add(_ request: UNNotificationRequest) async throws {
        addCount += 1
        if addCount == failOnAdd { throw SmokeFailure.expected }
        requests[request.identifier] = request
    }

    func pendingNotificationRequests() async -> [UNNotificationRequest] {
        Array(requests.values)
    }

    func canDeliverAudibleNotifications() async -> Bool { canDeliver }

    func removePendingNotificationRequests(withIdentifiers identifiers: [String]) {
        identifiers.forEach { requests.removeValue(forKey: $0) }
    }
}

@main
private struct AlarmSchedulerSmoke {
    static func main() async throws {
        let alarm = Alarm(hour: 7, minute: 30, weekdays: [2, 4])
        let center = RecordingNotificationCenter()
        let scheduler = AlarmScheduler(center: center)

        try await scheduler.schedule(alarm)
        let expectedIDs: Set<String> = [
            "awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w2",
            "awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w4"
        ]
        precondition(Set(center.requests.keys) == expectedIDs, "Repeating alarm must schedule every selected weekday")
        let scheduled = await scheduler.isScheduled(alarm)
        precondition(scheduled, "All weekday requests must be visible as scheduled")
        let readiness = await scheduler.readiness(for: alarm)
        precondition(readiness == .notificationFallback, "Notification fallback readiness must be reported truthfully")

        let recoveryCenter = RecordingNotificationCenter()
        let recoveryScheduler = AlarmScheduler(center: recoveryCenter)
        try await recoveryScheduler.schedule(alarm)
        var revisedAlarm = alarm
        revisedAlarm.version += 1
        try await recoveryScheduler.repair(revisedAlarm)
        let revisedIDs: Set<String> = [
            "awero:alarm:\(revisedAlarm.id.uuidString):v\(revisedAlarm.version):w2",
            "awero:alarm:\(revisedAlarm.id.uuidString):v\(revisedAlarm.version):w4"
        ]
        precondition(Set(recoveryCenter.requests.keys) == revisedIDs, "Repair must replace stale alarm versions")
        recoveryCenter.requests.removeValue(forKey: revisedIDs.sorted().last!)
        let incompleteSchedule = await recoveryScheduler.isScheduled(revisedAlarm)
        precondition(!incompleteSchedule, "A schedule missing a selected weekday must not report as complete")
        try await recoveryScheduler.repair(revisedAlarm)
        let recoveredSchedule = await recoveryScheduler.isScheduled(revisedAlarm)
        precondition(recoveredSchedule, "Repair must restore every selected weekday")

        let deniedCenter = RecordingNotificationCenter()
        deniedCenter.canDeliver = false
        let deniedScheduler = AlarmScheduler(center: deniedCenter)
        try await deniedScheduler.schedule(alarm)
        let deniedReadiness = await deniedScheduler.readiness(for: alarm)
        precondition(deniedReadiness == .actionRequired, "Missing audible-alert permission must require user action")

        let partialCenter = RecordingNotificationCenter()
        partialCenter.failOnAdd = 2
        let partialScheduler = AlarmScheduler(center: partialCenter)
        do {
            try await partialScheduler.schedule(alarm)
            throw SmokeFailure.expected
        } catch SmokeFailure.expected {
            precondition(partialCenter.requests.isEmpty, "A partial weekly schedule must be removed when any weekday fails")
        }

        var invalidAlarm = alarm
        invalidAlarm.hour = 24
        do {
            try AlarmScheduler.validate(invalidAlarm)
            throw SmokeFailure.expected
        } catch AlarmSchedulingError.invalidAlarmTime {
        }

        await scheduler.cancel(alarm)
        precondition(center.requests.isEmpty, "Cancel must remove all requests belonging to an alarm")
        print("iOS alarm scheduler smoke: PASS")
    }
}
