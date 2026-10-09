import Foundation

enum AlarmStoreLoadState: Equatable {
    case loading
    case loaded
    case failed
}

@MainActor
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []
    @Published private(set) var loadState: AlarmStoreLoadState = .loading
    private let database: CoreDataStore

    init(database: CoreDataStore = .shared) {
        self.database = database
        Task { await load() }
    }

    func load() async {
        loadState = .loading
        guard case let .success(loadedAlarms) = await database.fetchAlarms() else {
            loadState = .failed
            return
        }
        alarms = loadedAlarms
        await migrateLegacyIfNeeded()
        loadState = .loaded
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

        guard case let .success(persistedAlarms) = await database.fetchAlarms() else {
            return
        }
        let persistedIds = Set(persistedAlarms.map(\.id))
        guard legacy.allSatisfy({ persistedIds.contains($0.id) }) else {
            return
        }

        alarms = persistedAlarms
        UserDefaults.standard.set(true, forKey: key)
    }
}
