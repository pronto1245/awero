import Foundation

@MainActor
final class AlarmStore: ObservableObject {
    @Published private(set) var alarms: [Alarm] = []
    private let database = CoreDataStore.shared

    init() {
        Task { await load() }
    }

    func load() async {
        alarms = await database.fetchAlarms()
        await migrateLegacyIfNeeded()
    }

    func save(_ alarm: Alarm) async {
        await database.saveAlarm(alarm)
        if let index = alarms.firstIndex(where: { $0.id == alarm.id }) {
            alarms[index] = alarm
        } else {
            alarms.append(alarm)
        }
    }

    func update(_ alarm: Alarm) async {
        var next = alarm
        next.version += 1
        await save(next)
    }

    func delete(_ alarm: Alarm) async {
        await database.deleteAlarm(alarm)
        alarms.removeAll { $0.id == alarm.id }
    }

    private func migrateLegacyIfNeeded() async {
        let key = "awero.coredata.migrated.v1"
        guard !UserDefaults.standard.bool(forKey: key) else { return }
        let legacyKey = "awero.alarms.v1"
        if let data = UserDefaults.standard.data(forKey: legacyKey),
           let legacy = try? JSONDecoder().decode([Alarm].self, from: data) {
            for alarm in legacy {
                await database.saveAlarm(alarm)
            }
            alarms = await database.fetchAlarms()
        }
        UserDefaults.standard.set(true, forKey: key)
    }
}
