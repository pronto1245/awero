import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var alarms: AlarmStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var showingCreate = false
    @State private var editingAlarm: Alarm?
    @State private var testAlarmError: String?
    @State private var readiness: [UUID: AlarmReadiness] = [:]

    private var refreshKey: String {
        alarms.alarms.map { "\($0.id.uuidString):\($0.version):\($0.enabled)" }.joined(separator: "|")
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 28) {
                        Text("AWERO")
                            .font(.system(size: 36, weight: .black))
                            .foregroundStyle(.white)
                        Text("Wake up. Stay up.")
                            .font(.headline)
                            .foregroundStyle(.white.opacity(0.6))

                        if alarms.alarms.isEmpty {
                            Text("No alarms")
                                .font(.title2.bold())
                                .foregroundStyle(.white)
                                .padding(.top, 80)
                        } else {
                            ForEach(alarms.alarms) { alarm in
                                AlarmCard(
                                    alarm: alarm,
                                    readiness: readiness[alarm.id],
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
                            Text("Create alarm")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 18)
                                .background(Color.white)
                                .foregroundStyle(.black)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .padding(24)
                }
            }
            .sheet(isPresented: $showingCreate) { CreateAlarmView() }
            .sheet(item: $editingAlarm) { alarm in CreateAlarmView(alarm: alarm) }
            .alert("alarm.status.errorTitle", isPresented: Binding(
                get: { testAlarmError != nil },
                set: { if !$0 { testAlarmError = nil } }
            )) {
                Button("OK", role: .cancel) { testAlarmError = nil }
            } message: {
                Text(testAlarmError ?? "alarm.status.errorBody")
            }
            .task(id: refreshKey) {
                await refreshReadiness()
            }
            .onChange(of: scenePhase) { _, phase in
                if phase == .active {
                    Task { await refreshReadiness() }
                }
            }
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
    let onEdit: () -> Void
    let onTest: () -> Void
    let onRetry: () -> Void
    let onOpenSettings: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(String(format: "%02d:%02d", alarm.hour, alarm.minute))
                .font(.system(size: 42, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
            Text(alarm.weekdays.sorted().map(String.init).joined(separator: " · "))
                .foregroundStyle(.white.opacity(0.5))
            Text(alarm.missionType.rawValue)
                .font(.caption.bold())
                .foregroundStyle(.white.opacity(0.7))
            if alarm.enabled, let readiness, readiness != .disabled {
                Text(LocalizedStringKey(readiness.localizationKey))
                    .font(.caption)
                    .foregroundStyle(readiness == .scheduled ? .green : .orange)
                if readiness == .notScheduled {
                    Button("alarm.retry") { onRetry() }
                } else if readiness == .actionRequired {
                    Button("alarm.openSettings") { onOpenSettings() }
                    Button("alarm.retry") { onRetry() }
                }
            }
            HStack {
                Button("Test", action: onTest)
                Button("Edit", action: onEdit)
                Button("Delete", action: onDelete).foregroundStyle(.red)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
