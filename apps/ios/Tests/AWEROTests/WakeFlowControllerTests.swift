import XCTest
@testable import AWERO

@MainActor
final class WakeFlowControllerTests: XCTestCase {
    func testRestoreFallbackAndCompletionPersistAcrossControllerRestart() async throws {
        let (database, alarm) = try makeDatabase()
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)

        let firstFlow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await firstFlow.start(alarm: alarm, scheduledAt: .now)
        await firstFlow.beginMission()
        await firstFlow.fallbackToMath()

        XCTAssertEqual(firstFlow.state, .mission)
        XCTAssertEqual(firstFlow.currentMission, .math)

        let restoredFlow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await restoredFlow.restore()
        XCTAssertEqual(restoredFlow.state, .mission)
        XCTAssertEqual(restoredFlow.currentMission, .math)

        await restoredFlow.completeMission()
        await restoredFlow.completeMission()
        XCTAssertEqual(restoredFlow.state, .completed)

        let persisted = await database.fetchActiveWakeSession()
        XCTAssertNil(persisted)
        let statistics = await database.fetchStatistics()
        XCTAssertEqual(statistics?.completed, 1)
        XCTAssertEqual(statistics?.fallback, 1)
    }

    func testFailedSnoozeKeepsAlarmRingingAndCanRetry() async throws {
        let (database, alarm) = try makeDatabase(maxSnoozes: 2)
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)

        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await flow.start(alarm: alarm, scheduledAt: .now)

        var scheduledCount = 0
        var cancelledCount = 0
        let failed = await flow.snooze(
            schedule: { _ in
                scheduledCount += 1
                throw WakeFlowTestError.schedulingFailed
            },
            cancel: { _ in cancelledCount += 1 }
        )
        XCTAssertFalse(failed)
        XCTAssertEqual(flow.state, .ringing)
        XCTAssertEqual(flow.snoozeCount, 0)
        XCTAssertEqual(scheduledCount, 1)
        XCTAssertEqual(cancelledCount, 1)
        let sessionAfterFailure = await database.fetchActiveWakeSession()
        XCTAssertEqual(sessionAfterFailure?.snoozeCount, 0)

        let succeeded = await flow.snooze(
            schedule: { _ in scheduledCount += 1 },
            cancel: { _ in cancelledCount += 1 }
        )
        XCTAssertTrue(succeeded)
        XCTAssertEqual(flow.state, .idle)
        XCTAssertEqual(flow.snoozeCount, 1)
        let sessionAfterRetry = await database.fetchActiveWakeSession()
        XCTAssertEqual(sessionAfterRetry?.snoozeCount, 1)
    }

    func testEmergencyStopPersistsAndMissingSessionIsNoOp() async throws {
        let (database, alarm) = try makeDatabase()
        let idleFlow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await idleFlow.emergencyStop()
        XCTAssertEqual(idleFlow.state, .idle)

        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await flow.start(alarm: alarm, scheduledAt: .now)
        await flow.beginMission()
        await flow.emergencyStop()
        XCTAssertEqual(flow.state, .emergencyStopped)
        let activeSession = await database.fetchActiveWakeSession()
        XCTAssertNil(activeSession)
    }

    private func makeDatabase(maxSnoozes: Int = 3) throws -> (CoreDataStore, Alarm) {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-wake-flow-tests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let database = CoreDataStore(storeURL: directory.appendingPathComponent("AWERO.sqlite"))
        let alarm = Alarm(
            hour: 7,
            minute: 30,
            missionType: .qr,
            difficulty: .easy,
            maxSnoozes: maxSnoozes
        )
        return (database, alarm)
    }
}

private enum WakeFlowTestError: Error {
    case schedulingFailed
}
