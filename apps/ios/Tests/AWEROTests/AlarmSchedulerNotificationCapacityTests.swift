import UserNotifications
import XCTest
@testable import AWERO

@MainActor
final class AlarmSchedulerNotificationCapacityTests: XCTestCase {
    func testMissingPendingWeekdayIsReportedAndPartialScheduleIsRemoved() async throws {
        let alarm = Alarm(hour: 7, minute: 0)
        let missingIdentifier = "awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w7"
        let center = NotificationCenterStub(droppedIdentifiers: [missingIdentifier])
        let scheduler = AlarmScheduler(center: center, usesSystemAlarmKit: false)

        do {
            try await scheduler.schedule(alarm)
            XCTFail("Every selected weekday must be registered before the alarm is accepted.")
        } catch {
            XCTAssertEqual(error as? AlarmSchedulingError, .notificationScheduleIncomplete)
        }

        let pending = await center.pendingNotificationRequests()
        XCTAssertTrue(pending.isEmpty, "A partial alarm schedule must not be left behind.")
        let isScheduled = await scheduler.isScheduled(alarm)
        XCTAssertFalse(isScheduled)
    }

    func testAllSelectedWeekdaysMustAppearInPendingRequests() async throws {
        let alarm = Alarm(hour: 7, minute: 0, weekdays: [2, 4])
        let center = NotificationCenterStub()
        let scheduler = AlarmScheduler(center: center, usesSystemAlarmKit: false)

        try await scheduler.schedule(alarm)

        let isScheduled = await scheduler.isScheduled(alarm)
        XCTAssertTrue(isScheduled)
        let pending = await center.pendingNotificationRequests()
        XCTAssertEqual(pending.count, 2)
    }
}

private final class NotificationCenterStub: AlarmNotificationCenter {
    private var requests: [String: UNNotificationRequest] = [:]
    private let droppedIdentifiers: Set<String>

    init(droppedIdentifiers: Set<String> = []) {
        self.droppedIdentifiers = droppedIdentifiers
    }

    func requestAuthorization(options: UNAuthorizationOptions) async throws -> Bool { true }

    func add(_ request: UNNotificationRequest) async throws {
        guard !droppedIdentifiers.contains(request.identifier) else { return }
        requests[request.identifier] = request
    }

    func pendingNotificationRequests() async -> [UNNotificationRequest] {
        Array(requests.values)
    }

    func canDeliverAudibleNotifications() async -> Bool { true }

    func removePendingNotificationRequests(withIdentifiers identifiers: [String]) {
        identifiers.forEach { requests.removeValue(forKey: $0) }
    }
}
