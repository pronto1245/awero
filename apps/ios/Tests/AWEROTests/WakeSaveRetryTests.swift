import XCTest
@testable import AWERO

@MainActor
final class WakeSaveRetryTests: XCTestCase {
    func testFailedMissionStartOffersRetryWithoutAdvancingState() async throws {
        let (flow, database) = try await makeReadOnlyFlow(missionStarted: false)
        await flow.beginMission()
        XCTAssertEqual(flow.state, .ringing)
        XCTAssertNotNil(flow.actionError)
        await flow.retryPendingAction()
        XCTAssertEqual(flow.state, .ringing)
        XCTAssertNotNil(flow.actionError)
        let session = try await database.fetchActiveWakeSession().get()
        XCTAssertNil(session?.missionStartedAt)
    }

    func testFailedFallbackOffersRetryWithoutChangingPersistedMission() async throws {
        let (flow, database) = try await makeReadOnlyFlow(missionStarted: true)
        await flow.fallbackToMath()
        XCTAssertEqual(flow.currentMission, .qr)
        XCTAssertNotNil(flow.actionError)
        await flow.retryPendingAction()
        XCTAssertEqual(flow.state, .mission)
        XCTAssertEqual(flow.currentMission, .qr)
        let session = try await database.fetchActiveWakeSession().get()
        XCTAssertEqual(session?.fallbackUsed, false)
    }

    func testFailedCompletionOffersRetryAndKeepsSessionActive() async throws {
        let (flow, database) = try await makeReadOnlyFlow(missionStarted: true)
        await flow.completeMission()
        XCTAssertEqual(flow.state, .mission)
        XCTAssertNotNil(flow.actionError)
        await flow.retryPendingAction()
        XCTAssertEqual(flow.state, .mission)
        let session = try await database.fetchActiveWakeSession().get()
        XCTAssertNotNil(session)
        XCTAssertNil(session?.result)
        XCTAssertNil(session?.completedAt)
    }

    private func makeReadOnlyFlow(missionStarted: Bool) async throws -> (WakeFlowController, CoreDataStore) {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("awero-save-retry-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let url = directory.appendingPathComponent("AWERO.sqlite")
        let writer = CoreDataStore(storeURL: url)
        let alarm = Alarm(hour: 7, minute: 30, missionType: .qr, difficulty: .easy)
        let saved = await writer.saveAlarm(alarm)
        XCTAssertTrue(saved)
        let sessions = WakeSessionManager(database: writer)
        let triggered = await sessions.trigger(alarm: alarm, scheduledAt: .now)
        XCTAssertTrue(triggered)
        if missionStarted {
            let started = await sessions.startMission()
            XCTAssertTrue(started)
        }
        let reader = CoreDataStore(storeURL: url, readOnly: true)
        let flow = WakeFlowController(sessionManager: WakeSessionManager(database: reader), database: reader)
        await flow.restore()
        return (flow, reader)
    }
}
