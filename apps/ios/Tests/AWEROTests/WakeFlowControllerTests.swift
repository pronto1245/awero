import XCTest
@testable import AWERO

@MainActor
final class WakeFlowControllerTests: XCTestCase {
    #if canImport(AlarmKit) && canImport(AppIntents)
    @available(iOS 26.0, *)
    func testAlarmKitStopIntentStartsMissionOnPersistedWakeSession() async throws {
        let (database, alarm) = try await makeDatabase()
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)

        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )

        await StartAweroMissionIntent.trigger(alarmID: alarm.id.uuidString, using: flow)

        XCTAssertEqual(flow.state, .mission)
        let activeSession = await database.fetchActiveWakeSession()
        XCTAssertEqual(activeSession?.alarmId, alarm.id)
        XCTAssertNotNil(activeSession?.missionStartedAt)

        await flow.completeMission()
        XCTAssertEqual(flow.state, .completed)
    }
    #endif

    func testRestoreFallbackAndCompletionPersistAcrossControllerRestart() async throws {
        let (database, alarm) = try await makeDatabase()
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

    func testRestoredSnoozeCountAndRingingStateSurviveControllerRestart() async throws {
        let (database, alarm) = try await makeDatabase(maxSnoozes: 2)
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await flow.start(alarm: alarm)
        let snoozed = await flow.snooze(schedule: { _ in }, cancel: { _ in })
        XCTAssertTrue(snoozed)

        let restored = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await restored.restore()

        XCTAssertEqual(restored.state, .ringing)
        XCTAssertEqual(restored.snoozeCount, 1)
        let activeSession = await database.fetchActiveWakeSession()
        XCTAssertEqual(activeSession?.snoozeCount, 1)
    }

    func testTestAlarmStaysOutOfRealStatisticsAndWakeHistory() async throws {
        let (database, alarm) = try await makeDatabase()
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )

        await flow.start(alarm: alarm, isTestAlarm: true)
        await flow.beginMission()
        await flow.completeMission()

        let stats = await database.fetchStatistics()
        XCTAssertNil(stats, "Test alarms must not create real statistics")
        let realHistory = await database.fetchWakeSessions()
        XCTAssertTrue(realHistory.isEmpty)
        let savedTest = await database.fetchWakeSessions(includeTestAlarms: true)
        XCTAssertEqual(savedTest.count, 1)
        XCTAssertTrue(savedTest[0].isTest)
    }

    func testTestAlarmSnoozeStateSurvivesRestoreAndRedelivery() async throws {
        let (database, alarm) = try await makeDatabase(maxSnoozes: 2)
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let firstFlow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await firstFlow.start(alarm: alarm, isTestAlarm: true)
        let snoozed = await firstFlow.snooze(schedule: { _ in }, cancel: { _ in })
        XCTAssertTrue(snoozed)

        let restored = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await restored.restore()
        XCTAssertEqual(restored.state, .ringing)
        XCTAssertEqual(restored.snoozeCount, 1)

        await restored.start(alarm: alarm, isTestAlarm: true)
        XCTAssertEqual(restored.state, .ringing)
        XCTAssertEqual(restored.snoozeCount, 1)
        await restored.beginMission()
        await restored.completeMission()

        let stats = await database.fetchStatistics()
        XCTAssertNil(stats, "Test alarms must not create real statistics")
        let realHistory = await database.fetchWakeSessions()
        let allHistory = await database.fetchWakeSessions(includeTestAlarms: true)
        XCTAssertTrue(realHistory.isEmpty)
        XCTAssertEqual(allHistory.count, 1)
    }

    func testRepeatedSnoozeWhileSchedulingDoesNotCancelSuccessfulAlarm() async throws {
        let (database, alarm) = try await makeDatabase(maxSnoozes: 2)
        let saved = await database.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let flow = WakeFlowController(
            sessionManager: WakeSessionManager(database: database),
            database: database
        )
        await flow.start(alarm: alarm, scheduledAt: .now)

        var resumeSchedule: CheckedContinuation<Void, Never>?
        var scheduledCount = 0
        var cancelledCount = 0
        let firstSnooze = Task { @MainActor in
            await flow.snooze(
                schedule: { _ in
                    scheduledCount += 1
                    await withCheckedContinuation { resumeSchedule = $0 }
                },
                cancel: { _ in cancelledCount += 1 }
            )
        }
        while resumeSchedule == nil { await Task.yield() }

        let repeated = await flow.snooze(
            schedule: { _ in scheduledCount += 1 },
            cancel: { _ in cancelledCount += 1 }
        )
        resumeSchedule?.resume()
        let succeeded = await firstSnooze.value

        XCTAssertFalse(repeated)
        XCTAssertTrue(succeeded)
        XCTAssertEqual(scheduledCount, 1)
        XCTAssertEqual(cancelledCount, 0)
        XCTAssertEqual(flow.state, .idle)
        XCTAssertEqual(flow.snoozeCount, 1)
        let session = await database.fetchActiveWakeSession()
        XCTAssertEqual(session?.snoozeCount, 1)

        // A later alarm delivery can still use the remaining snooze allowance.
        await flow.start(alarm: alarm, scheduledAt: .now)
        let laterSnooze = await flow.snooze(
            schedule: { _ in scheduledCount += 1 },
            cancel: { _ in cancelledCount += 1 }
        )
        XCTAssertTrue(laterSnooze)
        XCTAssertEqual(scheduledCount, 2)
        XCTAssertEqual(cancelledCount, 0)
        XCTAssertEqual(flow.snoozeCount, 2)
    }

    func testFailedSnoozeKeepsAlarmRingingAndCanRetry() async throws {
        let (database, alarm) = try await makeDatabase(maxSnoozes: 2)
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
        let (database, alarm) = try await makeDatabase()
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

    private func makeDatabase(maxSnoozes: Int = 3) async throws -> (CoreDataStore, Alarm) {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-wake-flow-tests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let storeURL = directory.appendingPathComponent("AWERO.sqlite")
        let database = await Task.detached {
            CoreDataStore(storeURL: storeURL)
        }.value
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
