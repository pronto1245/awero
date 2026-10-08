import Foundation

@MainActor
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []
    private let database: CoreDataStore

    init(database: CoreDataStore = .shared) {
        self.database = database
        Task { await load() }
    }

    func load() async {
        alarms = await database.fetchAlarms()
        await migrateLegacyIfNeeded()
    }

    @discardableResult
    func save(_ alarm: Alarm) async -> Bool {
        guard await database.saveAlarm(alarm) else { return false }
        if let index = alarms.firstIndex(where: { $0.id == alarm.id }) {
            alarms[index] = alarm
        } else {
            alarms.append(alarm)
        }
        return true
    }

    func update(_ alarm: Alarm) async {
        var next = alarm
        next.version += 1
        await save(next)
    }

    @discardableResult
    func delete(_ alarm: Alarm) async -> Bool {
        guard await database.deleteAlarm(alarm) else { return false }
        alarms.removeAll { $0.id == alarm.id }
        return true
    }

    private func migrateLegacyIfNeeded() async {
        let key = "awero.coredata.migrated.v1"
        guard !UserDefaults.standard.bool(forKey: key) else { return }

        let legacyKey = "awero.alarms.v1"
        guard
            let data = UserDefaults.standard.data(forKey: legacyKey),
            let legacy = try? JSONDecoder().decode([Alarm].self, from: data)
        else {
            UserDefaults.standard.set(true, forKey: key)
            return
        }

        for alarm in legacy {
            await database.saveAlarm(alarm)
        }

        let persistedIds = Set((await database.fetchAlarms()).map(\.id))
        guard legacy.allSatisfy({ persistedIds.contains($0.id) }) else {
            return
        }

        alarms = await database.fetchAlarms()
        UserDefaults.standard.set(true, forKey: key)
    }
}
