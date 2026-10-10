import SwiftUI


struct HomeView: View {
    @Binding var showingCreate: Bool
    let onOpenSettings: () -> Void
    @EnvironmentObject private var alarms: AlarmStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var editingAlarm: Alarm?
    @State private var testAlarmError: String?
    @State private var deleteCandidate: Alarm?
    @State private var readiness: [UUID: AlarmReadiness] = [:]
    @State private var now = Date()
    @ScaledMetric(relativeTo: .largeTitle) private var wordmarkSize: CGFloat = 24
    @ScaledMetric(relativeTo: .largeTitle) private var greetingSize: CGFloat = 30
    @ScaledMetric(relativeTo: .largeTitle) private var nextAlarmSize: CGFloat = 40

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
        ZStack {
                AweroDesign.ivory.ignoresSafeArea()
                AweroSceneBackground(scene: .current(at: now))
                    .ignoresSafeArea()
                    .allowsHitTesting(false)
                VStack(spacing: 0) {
                // Header stays pinned so Settings is always reachable, whatever the scroll position.
                HStack {
                    Text("AWERO")
                        .font(.system(size: wordmarkSize, weight: .black))
                        .foregroundStyle(AweroDesign.navy)
                    Spacer()
                    Button(action: onOpenSettings) {
                        Image(systemName: "gearshape")
                            .font(.title3)
                            .foregroundStyle(AweroDesign.navy)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .accessibilityLabel(Text("settings.title"))
                    .accessibilityIdentifier("home.settings")
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("home.greeting")
                                .font(.system(size: greetingSize, weight: .bold))
                                .foregroundStyle(AweroDesign.navy)
                            HStack(spacing: 5) {
                                Text("home.subtitle")
                                    .font(.subheadline)
                                    .foregroundStyle(AweroDesign.navy.opacity(0.68))
                                Image(systemName: "sun.max.fill")
                                    .font(.subheadline)
                                    .foregroundStyle(AweroDesign.sun)
                                    .accessibilityHidden(true)
                            }
                        }
                        .padding(.top, 4)

                        // Space for the scene's sun and peaks; the next-alarm card overlaps them.
                        Color.clear.frame(height: 150)
                            .padding(.horizontal, -20)

                        if let next = upcomingAlarms.first {
                            Button { editingAlarm = next.alarm } label: {
                                VStack(alignment: .leading, spacing: 7) {
                                    Label("home.next_alarm", systemImage: "sun.max.fill")
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(AweroDesign.coral)
                                    HStack(alignment: .center) {
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(formatted(next.date, for: next.alarm, dateStyle: .none, timeStyle: .short))
                                                .font(.system(size: nextAlarmSize, weight: .bold))
                                                .foregroundStyle(AweroDesign.navy)
                                            Text(formatted(next.date, for: next.alarm, dateStyle: .full, timeStyle: .none) + " · " + missionName(next.alarm.missionType))
                                                .font(.subheadline)
                                                .foregroundStyle(AweroDesign.navy.opacity(0.68))
                                                .lineLimit(1)
                                                .minimumScaleFactor(0.75)
                                        }
                                        Spacer(minLength: 8)
                                        Image(systemName: "chevron.right")
                                            .font(.headline.weight(.semibold))
                                            .foregroundStyle(AweroDesign.coral)
                                            .frame(width: 40, height: 40)
                                            .background(AweroDesign.coral.opacity(0.12), in: Circle())
                                    }
                                    .accessibilityElement(children: .combine)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(18)
                                .background(AweroDesign.surface.opacity(0.96))
                                .clipShape(RoundedRectangle(cornerRadius: 22))
                            }
                            .buttonStyle(.plain)
                            .padding(.horizontal, 12)
                            .padding(.top, -28)
                            .accessibilityIdentifier("home.nextAlarm")
                        } else {
                            Spacer().frame(height: 4)
                        }

                        if alarms.loadState == .loaded, !alarms.alarms.isEmpty, upcomingAlarms.isEmpty {
                            VStack(alignment: .leading, spacing: 5) {
                                Label("home.no_enabled_title", systemImage: "alarm")
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(AweroDesign.navy)
                                Text("home.no_enabled_body")
                                    .font(.caption)
                                    .foregroundStyle(AweroDesign.navy.opacity(0.68))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(14)
                            .background(AweroDesign.surfaceWarm, in: RoundedRectangle(cornerRadius: 18))
                        }

                        Text("home.alarms")
                            .font(.title3.bold())
                            .foregroundStyle(AweroDesign.navy)

                        if alarms.loadState == .loading {
                            ProgressView("home.loading_alarms")
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(22)
                                .background(AweroDesign.surface)
                                .clipShape(RoundedRectangle(cornerRadius: 22))
                        } else if alarms.loadState == .failed {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("home.alarms_load_error_title").font(.title3.bold())
                                Text("home.alarms_load_error_body")
                                    .foregroundStyle(AweroDesign.navy.opacity(0.7))
                                Button("home.retry_loading_alarms") {
                                    Task { await alarms.load() }
                                }
                                .font(.headline)
                                .foregroundStyle(AweroDesign.coral)
                            }
                            .foregroundStyle(AweroDesign.navy)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(22)
                            .background(AweroDesign.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 22))
                        } else if alarms.alarms.isEmpty {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("home.empty_title").font(.title3.bold())
                                Text("home.empty_body").foregroundStyle(AweroDesign.navy.opacity(0.7))
                            }
                            .foregroundStyle(AweroDesign.navy)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(22)
                            .background(AweroDesign.surface)
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
                                                testAlarmError = NSLocalizedString("home.error_body", comment: "Alarm action error")
                                            }
                                        }
                                    },
                                    onRetry: {
                                        Task {
                                            do {
                                                try await AlarmScheduler().repair(alarm)
                                                readiness[alarm.id] = await AlarmScheduler().readiness(for: alarm)
                                            } catch {
                                                testAlarmError = NSLocalizedString("home.error_body", comment: "Alarm action error")
                                                readiness[alarm.id] = await AlarmScheduler().readiness(for: alarm)
                                            }
                                        }
                                    },
                                    onOpenSettings: {
                                        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                                        UIApplication.shared.open(url)
                                    },
                                    onDelete: { deleteCandidate = alarm }
                                )
                            }
                        }


                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 18)
                }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button { showingCreate = true } label: {
                    Label("home.add_alarm", systemImage: "plus.circle.fill")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .foregroundStyle(.white)
                        .background(AweroDesign.coralStrong)
                        .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
                }
                .accessibilityIdentifier("home.createAlarm")
                .padding(.horizontal, 20)
                .padding(.vertical, 8)
                .background(AweroDesign.ivory)
            }
            .sheet(item: $editingAlarm) { alarm in CreateAlarmView(alarm: alarm) }
            .confirmationDialog(
                "home.delete_confirm_title",
                isPresented: Binding(
                    get: { deleteCandidate != nil },
                    set: { if !$0 { deleteCandidate = nil } }
                ),
                titleVisibility: .visible
            ) {
                Button("home.delete", role: .destructive) {
                    guard let alarm = deleteCandidate else { return }
                    deleteCandidate = nil
                    Task {
                        do {
                            try await AlarmCoordinator(store: alarms).delete(alarm)
                        } catch {
                            testAlarmError = NSLocalizedString("home.error_body", comment: "Alarm action error")
                        }
                    }
                }
                Button("create.cancel", role: .cancel) { deleteCandidate = nil }
            } message: {
                Text("home.delete_confirm_body")
            }
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
                testAlarmError = NSLocalizedString("home.error_body", comment: "Alarm action error")
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

    private func missionName(_ type: MissionType) -> String {
        let key: String
        switch type {
        case .math: key = "home.mission.math"
        case .steps: key = "home.mission.steps"
        case .qr: key = "home.mission.qr"
        case .photo: key = "home.mission.photo"
        case .mixed: key = "home.mission.mixed"
        }
        return NSLocalizedString(key, comment: "Alarm mission name")
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
                                    .font(.system(size: alarmTimeSize, weight: .bold))
                        .foregroundStyle(AweroDesign.navy)
                        .accessibilityIdentifier("home.alarm.time")
                    Text(weekdays)
                        .foregroundStyle(AweroDesign.navy.opacity(0.55))
                }
                Spacer()
                Toggle(isOn: Binding(get: { alarm.enabled }, set: onToggle)) {
                    Text(alarm.enabled ? "home.enabled" : "home.disabled")
                }
                .labelsHidden()
                .tint(AweroDesign.coral)
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
            HStack(spacing: 6) {
                Image(systemName: missionIcon(alarm.missionType))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AweroDesign.coral)
                Text(missionKey(alarm.missionType))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AweroDesign.navy.opacity(0.72))
            }
            if alarm.enabled, let readiness, readiness != .disabled {
                Text(LocalizedStringKey(readiness.localizationKey))
                    .font(.caption)
                    .foregroundStyle(readiness == .scheduled ? AweroDesign.sage : AweroDesign.coral)
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
                Button("home.delete", action: onDelete).foregroundStyle(AweroDesign.coral)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .background(AweroDesign.surface)
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

    private func missionIcon(_ type: MissionType) -> String {
        switch type {
        case .math: "calculator"
        case .steps: "figure.walk"
        case .qr: "qrcode"
        case .photo: "camera"
        case .mixed: "square.grid.2x2"
        }
    }
}
