import Foundation

private struct ServerAlarmSnapshot: Decodable {
    let id: UUID
    let version: Int
    let label: String
    let hour: Int
    let minute: Int
    let enabled: Bool
    let weekdays: [Int]
    let timezoneMode: AlarmTimezoneMode
    let fixedTimezone: String?
    let status: String
    let snoozeEnabled: Bool
    let maxSnoozes: Int
    let snoozeMinutes: Int
    let missionType: MissionType
    let difficulty: Difficulty
    let qrExpectedCode: String?

    var alarm: Alarm {
        Alarm(id: id, version: version, hour: hour, minute: minute, enabled: enabled,
              weekdays: Set(weekdays), timezoneMode: timezoneMode, fixedTimezone: fixedTimezone,
              missionType: missionType, difficulty: difficulty, maxSnoozes: maxSnoozes,
              snoozeMinutes: snoozeMinutes, qrExpectedCode: qrExpectedCode,
              label: label, snoozeEnabled: snoozeEnabled)
    }
}

private struct ServerAlarmList: Decodable { let items: [ServerAlarmSnapshot] }

struct AlarmSnapshotSyncClient {
    private let configuration: SyncAPIConfiguration
    private let transport: any SyncHTTPTransport
    private let database: CoreDataStore
    private let scheduler: AlarmScheduler

    init(
        configuration: SyncAPIConfiguration = SyncAPIConfiguration(),
        transport: any SyncHTTPTransport = URLSessionSyncHTTPTransport(),
        database: CoreDataStore = .shared,
        scheduler: AlarmScheduler = AlarmScheduler()
    ) {
        self.configuration = configuration
        self.transport = transport
        self.database = database
        self.scheduler = scheduler
    }

    func pullAndApply(bearerToken: String, protectedAlarmIDs: Set<UUID>) async {
        guard let url = configuration.endpoint("sync/alarms"),
              let localResult = await database.fetchAlarms().successValue else { return }
        do {
            let response = try await transport.get(url: url, bearerToken: bearerToken)
            guard (200..<300).contains(response.statusCode) else { return }
            let server = try JSONDecoder().decode(ServerAlarmList.self, from: response.body)
            let localByID = Dictionary(uniqueKeysWithValues: localResult.map { ($0.id, $0) })
            for snapshot in server.items where !protectedAlarmIDs.contains(snapshot.id) {
                let local = localByID[snapshot.id]
                if snapshot.status == "DELETED" {
                    guard let local else { continue }
                    await scheduler.cancel(local)
                    if !(await database.deleteAlarm(local)) { try? await scheduler.schedule(local) }
                    continue
                }
                guard local.map({ snapshot.version > $0.version }) ?? true else { continue }
                let remote = snapshot.alarm
                do {
                    if remote.enabled { try await scheduler.schedule(remote) }
                    else { await scheduler.cancel(local ?? remote) }
                    guard await database.saveAlarm(remote) else { throw SyncAPIError.invalidResponse }
                } catch {
                    await scheduler.cancel(remote)
                    if let local, local.enabled { try? await scheduler.schedule(local) }
                }
            }
        } catch {
            // The server snapshot is optional; local alarms remain authoritative until a complete response arrives.
        }
    }
}

private extension Result where Failure == PersistenceError {
    var successValue: Success? {
        guard case let .success(value) = self else { return nil }
        return value
    }
}
