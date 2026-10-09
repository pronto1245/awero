import SwiftUI

private enum AweroStyle {
    static let ivory = Color(red: 1.0, green: 0.973, blue: 0.937)
    static let navy = Color(red: 0.078, green: 0.161, blue: 0.294)
    static let coral = Color(red: 1.0, green: 0.408, blue: 0.294)
    static let sage = Color(red: 0.31, green: 0.545, blue: 0.40)
}

struct HomeView: View {
    @EnvironmentObject private var alarms: AlarmStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var showingCreate = false
    @State private var editingAlarm: Alarm?
    @State private var testAlarmError: String?
    @State private var readiness: [UUID: AlarmReadiness] = [:]
    @State private var now = Date()
    @ScaledMetric(relativeTo: .largeTitle) private var wordmarkSize: CGFloat = 28
    @ScaledMetric(relativeTo: .largeTitle) private var greetingSize: CGFloat = 34
    @ScaledMetric(relativeTo: .largeTitle) private var nextAlarmSize: CGFloat = 46

    private var refreshKey: String {
        alarms.alarms.map { "\($0.id.uuidString):\($0.version):\($0.enabled)" }.joined(separator: "|")
    }

    private var upcomingAlarms: [(alarm: Alarm, date: Date)] {
        alarms.alarms.compactMap { alarm in
            guard alarm.enabled, let date = nextOccurrence(for: alarm, after: now) else { return nil }
            return (alarm, date)
        }.sorted { $0.date < $1.date }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AweroStyle.ivory.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        Text("AWERO")
                            .font(.system(size: wordmarkSize, weight: .black))
                            .foregroundStyle(AweroStyle.navy)
                        Text("home.greeting")
                            .font(.system(size: greetingSize, weight: .bold, design: .rounded))
                            .foregroundStyle(AweroStyle.navy)
                        Text("home.subtitle")
                            .font(.subheadline)
                            .foregroundStyle(AweroStyle.navy.opacity(0.65))

                        if let next = upcomingAlarms.first {
                            VStack(alignment: .leading, spacing: 6) {
                                Label("home.next_alarm", systemImage: "sun.max.fill")
                                    .font(.headline)
                                    .foregroundStyle(AweroStyle.coral)
                                Text(formatted(next.date, for: next.alarm, dateStyle: .none, timeStyle: .short))
                                    .font(.system(size: nextAlarmSize, weight: .bold, design: .rounded))
                                    .foregroundStyle(AweroStyle.navy)
                                Text(formatted(next.date, for: next.alarm, dateStyle: .full, timeStyle: .short))
                                    .font(.subheadline)
                                    .foregroundStyle(AweroStyle.navy.opacity(0.65))
                                Text(missionKey(next.alarm.missionType))
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(AweroStyle.navy.opacity(0.8))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(20)
                            .background(
                                LinearGradient(
                                    colors: [Color(red: 1, green: 0.90, blue: 0.77), Color.white],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 24))
                        }

                        Text("home.alarms")
                            .font(.title3.bold())
                            .foregroundStyle(AweroStyle.navy)

                        if alarms.loadState == .loading {
                            ProgressView("home.loading_alarms")
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(22)
                                .background(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 22))
                        } else if alarms.loadState == .failed {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("home.alarms_load_error_title").font(.title3.bold())
                                Text("home.alarms_load_error_body")
                                    .foregroundStyle(AweroStyle.navy.opacity(0.7))
                                Button("home.retry_loading_alarms") {
                                    Task { await alarms.load() }
                                }
                                .font(.headline)
                                .foregroundStyle(AweroStyle.coral)
                            }
                            .foregroundStyle(AweroStyle.navy)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(22)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 22))
                        } else if alarms.alarms.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("home.empty_title").font(.title3.bold())
                                Text("home.empty_body").foregroundStyle(AweroStyle.navy.opacity(0.7))
                            }
                            .foregroundStyle(AweroStyle.navy)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(22)
                            .background(.white)
                            .clipShape(RoundedRectangle(cornerRadius: 22))
                            .frame(maxHeight: .infinity, alignment: .top)
                        } else {
                            ForEach(alarms.alarms) { alarm in
                                AlarmCard(
                                    alarm: alarm,
                                    readiness: readiness[alarm.id],
                                    onToggle: { enabled in toggle(alarm, enabled: enabled) },
                                    onEdit: { editingAlarm = alarm },
                                    onTest: {
                                        Task {
                                            do {
                                                try await AlarmCoordinator(store: alarms).test(alarm)
                                            } catch {
                                                testAlarmError = error.localizedDescription
                                            }
                                        }
                                    },
                                    onRetry: {
                                        Task {
                                            do {
                                                try await AlarmScheduler().repair(alarm)
                                                readiness[alarm.id] = await AlarmScheduler().readiness(for: alarm)
                                            } catch {
                                                testAlarmError = error.localizedDescription
                                                readiness[alarm.id] = await AlarmScheduler().readiness(for: alarm)
                                            }
                                        }
                                    },
                                    onOpenSettings: {
                                        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                                        UIApplication.shared.open(url)
                                    },
                                    onDelete: { Task { await AlarmCoordinator(store: alarms).delete(alarm) } }
                                )
                            }
                        }

                        Button {
                            showingCreate = true
                        } label: {
                            Label("home.add_alarm", systemImage: "plus.circle.fill")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(AweroStyle.coral)
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: 18))
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.vertical, 18)
                }
            }
            .sheet(isPresented: $showingCreate) { CreateAlarmView() }
            .sheet(item: $editingAlarm) { alarm in CreateAlarmView(alarm: alarm) }
            .alert("alarm.status.errorTitle", isPresented: Binding(
                get: { testAlarmError != nil },
                set: { if !$0 { testAlarmError = nil } }
            )) {
                Button("home.ok", role: .cancel) { testAlarmError = nil }
            } message: {
                Text(testAlarmError ?? "alarm.status.errorBody")
            }
            .task(id: refreshKey) {
                now = Date()
                await refreshReadiness()
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    now = Date()
                    Task { await refreshReadiness() }
                }
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    NavigationLink {
                        SettingsView()
                    } label: {
                        Image(systemName: "gearshape")
                            .foregroundStyle(AweroStyle.navy)
                    }
                    .accessibilityLabel(Text("settings.title"))
                }
            }
        }
    }

    private func toggle(_ alarm: Alarm, enabled: Bool) {
        Task {
            var updated = alarm
            updated.enabled = enabled
            do {
                try await AlarmCoordinator(store: alarms).update(updated)
                now = Date()
                await refreshReadiness()
            } catch {
                testAlarmError = error.localizedDescription
            }
        }
    }

    private func nextOccurrence(for alarm: Alarm, after date: Date) -> Date? {
        let zone: TimeZone
        if alarm.timezoneMode == .fixed, let identifier = alarm.fixedTimezone,
           let fixed = TimeZone(identifier: identifier) {
            zone = fixed
        } else {
            zone = .current
        }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone

        return alarm.weekdays.compactMap { weekday -> Date? in
            var matching = DateComponents()
            matching.weekday = weekday
            matching.hour = alarm.hour
            matching.minute = alarm.minute
            matching.second = 0
            return calendar.nextDate(
                after: date,
                matching: matching,
                matchingPolicy: .nextTime,
                repeatedTimePolicy: .first,
                direction: .forward
            )
        }.min()
    }

    private func formatted(
        _ date: Date,
        for alarm: Alarm,
        dateStyle: DateFormatter.Style,
        timeStyle: DateFormatter.Style
    ) -> String {
        let formatter = DateFormatter()
        formatter.locale = .current
        if alarm.timezoneMode == .fixed, let identifier = alarm.fixedTimezone,
           let timezone = TimeZone(identifier: identifier) {
            formatter.timeZone = timezone
        } else {
            formatter.timeZone = .current
        }
        formatter.dateStyle = dateStyle
        formatter.timeStyle = timeStyle
        return formatter.string(from: date)
    }

    private func missionKey(_ type: MissionType) -> LocalizedStringKey {
        switch type {
        case .math: "home.mission.math"
        case .steps: "home.mission.steps"
        case .qr: "home.mission.qr"
        case .photo: "home.mission.photo"
        case .mixed: "home.mission.mixed"
        }
    }

    @MainActor
    private func refreshReadiness() async {
        let scheduler = AlarmScheduler()
        var current: [UUID: AlarmReadiness] = [:]
        for alarm in alarms.alarms {
            current[alarm.id] = await scheduler.readiness(for: alarm)
        }
        readiness = current
    }
}

private struct AlarmCard: View {
    let alarm: Alarm
    let readiness: AlarmReadiness?
    let onToggle: (Bool) -> Void
    let onEdit: () -> Void
    let onTest: () -> Void
    let onRetry: () -> Void
    let onOpenSettings: () -> Void
    let onDelete: () -> Void
    @ScaledMetric(relativeTo: .largeTitle) private var alarmTimeSize: CGFloat = 38

    private var alarmTimeZone: TimeZone {
        if alarm.timezoneMode == .fixed,
           let identifier = alarm.fixedTimezone,
           let timeZone = TimeZone(identifier: identifier) {
            return timeZone
        }
        return .current
    }

    private var alarmTimeText: String {
        AlarmTimeFormatter.string(
            hour: alarm.hour,
            minute: alarm.minute,
            timeZone: alarmTimeZone
        )
    }

    private var weekdays: String {
        var calendar = Calendar(identifier: .gregorian)
        if alarm.timezoneMode == .fixed, let identifier = alarm.fixedTimezone,
           let timezone = TimeZone(identifier: identifier) {
            calendar.timeZone = timezone
        }
        return alarm.weekdays.sorted().compactMap { weekday in
            let index = weekday - 1
            guard calendar.shortWeekdaySymbols.indices.contains(index) else { return nil }
            return calendar.shortWeekdaySymbols[index]
        }.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(alarmTimeText)
                        .font(.system(size: alarmTimeSize, weight: .bold, design: .rounded))
                        .foregroundStyle(AweroStyle.navy)
                    Text(weekdays)
                        .foregroundStyle(AweroStyle.navy.opacity(0.55))
                }
                Spacer()
                Toggle(isOn: Binding(get: { alarm.enabled }, set: onToggle)) {
                    Text(alarm.enabled ? "home.enabled" : "home.disabled")
                }
                .labelsHidden()
                .tint(AweroStyle.coral)
                .accessibilityLabel(
                    Text(
                        String.localizedStringWithFormat(
                            NSLocalizedString("home.toggle_alarm", comment: "VoiceOver label for an alarm switch"),
                            alarmTimeText
                        )
                    )
                )
                .accessibilityValue(alarm.enabled ? Text("home.enabled") : Text("home.disabled"))
            }
            Text(missionKey(alarm.missionType))
                .font(.caption.weight(.semibold))
                .foregroundStyle(AweroStyle.navy.opacity(0.72))
            if alarm.enabled, let readiness, readiness != .disabled {
                Text(LocalizedStringKey(readiness.localizationKey))
                    .font(.caption)
                    .foregroundStyle(readiness == .scheduled ? AweroStyle.sage : AweroStyle.coral)
                if readiness == .notScheduled {
                    Button("alarm.retry", action: onRetry)
                } else if readiness == .actionRequired {
                    Button("alarm.openSettings", action: onOpenSettings)
                    Button("alarm.retry", action: onRetry)
                }
            }
            HStack {
                Button("home.test", action: onTest)
                Button("home.edit", action: onEdit)
                Button("home.delete", action: onDelete).foregroundStyle(AweroStyle.coral)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func missionKey(_ type: MissionType) -> LocalizedStringKey {
        switch type {
        case .math: "home.mission.math"
        case .steps: "home.mission.steps"
        case .qr: "home.mission.qr"
        case .photo: "home.mission.photo"
        case .mixed: "home.mission.mixed"
        }
    }
}
