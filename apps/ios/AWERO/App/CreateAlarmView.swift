import SwiftUI
import AVFoundation

private enum CreateAweroStyle {
    static let ivory = Color(red: 1.0, green: 0.973, blue: 0.937)
    static let navy = Color(red: 0.078, green: 0.161, blue: 0.294)
    static let coral = Color(red: 1.0, green: 0.408, blue: 0.294)
}

struct CreateAlarmView: View {
    let alarm: Alarm?
    @EnvironmentObject private var store: AlarmStore
    @Environment(\.dismiss) private var dismiss
    @AppStorage("awero.didExplainAlarmPermission.v1") private var didExplainAlarmPermission = false

    @State private var wakeDate: Date
    @State private var followsDeviceTimezone: Bool
    @State private var fixedTimezone: String
    @State private var selectedDays: Set<Int>
    @State private var mission: MissionType
    @State private var difficulty: Difficulty
    @State private var qrExpectedCode: String
    @State private var showCodeScanner = false
    @State private var showingPermissionIntro = false
    @State private var saveError: String?

    init(alarm: Alarm? = nil) {
        self.alarm = alarm
        let calendar = Calendar.current
        let base = calendar.date(from: DateComponents(hour: alarm?.hour ?? 7, minute: alarm?.minute ?? 30)) ?? Date()
        _wakeDate = State(initialValue: base)
        _followsDeviceTimezone = State(initialValue: alarm?.timezoneMode != .fixed)
        _fixedTimezone = State(initialValue: alarm?.fixedTimezone ?? TimeZone.current.identifier)
        _selectedDays = State(initialValue: alarm?.weekdays ?? Set(1...7))
        _mission = State(initialValue: alarm?.missionType ?? .math)
        _difficulty = State(initialValue: alarm?.difficulty ?? .medium)
        _qrExpectedCode = State(initialValue: alarm?.qrExpectedCode ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                wakeTimeSection
                timezoneSection
                daysSection
                missionSection
                permissionSection
            }
            .scrollContentBackground(.hidden)
            .background(CreateAweroStyle.ivory)
            .tint(CreateAweroStyle.coral)
            .navigationTitle(alarm == nil ? "create.title" : "create.edit_title")
            .alert("permission.ios_alarm_title", isPresented: $showingPermissionIntro) {
                Button("permission.continue") {
                    didExplainAlarmPermission = true
                    Task {
                        do {
                            _ = try await AlarmScheduler().requestAuthorization()
                            saveAlarmNow()
                        } catch {
                            saveError = NSLocalizedString("create.error_body", comment: "Alarm save error")
                        }
                    }
                }
                .accessibilityIdentifier("permission.continue")
                Button("create.cancel", role: .cancel) {}
            } message: {
                Text("permission.ios_alarm_body")
            }
            .alert("create.error_title", isPresented: Binding(
                get: { saveError != nil },
                set: { if !$0 { saveError = nil } }
            )) {
                Button("home.ok", role: .cancel) { saveError = nil }
            } message: {
                Text(saveError ?? "alarm.status.errorBody")
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("create.cancel") { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(alarm == nil ? "create.save" : "create.save_changes", action: saveAlarm)
                    .disabled((mission == .qr && qrExpectedCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) || selectedDays.isEmpty)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(CreateAweroStyle.coral)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
                    .background(CreateAweroStyle.ivory.opacity(0.96))
                    .accessibilityIdentifier("alarm.save")
            }
        }
        .sheet(isPresented: $showCodeScanner) {
            AlarmCodeScanner { code in
                qrExpectedCode = code
                showCodeScanner = false
            }
        }
    }

    private var wakeTimeSection: some View {
        Section {
            DatePicker("create.time", selection: $wakeDate, displayedComponents: .hourAndMinute)
        }
        .listRowBackground(Color.white)
    }

    private var timezoneSection: some View {
        Section {
            Picker("create.timezone", selection: $followsDeviceTimezone) {
                Text("create.timezone_device").tag(true)
                Text("create.timezone_fixed").tag(false)
            }
            .pickerStyle(.menu)

            if followsDeviceTimezone {
                Text("create.timezone_device_hint")
                    .font(.footnote)
                    .foregroundStyle(CreateAweroStyle.navy.opacity(0.65))
            } else {
                Text(
                    String.localizedStringWithFormat(
                        NSLocalizedString("create.timezone_fixed_hint", comment: "Fixed alarm time zone description"),
                        fixedTimezone
                    )
                )
                .font(.footnote)
                .foregroundStyle(CreateAweroStyle.navy.opacity(0.65))
            }
        } header: {
            Text("create.timezone")
        }
        .listRowBackground(Color.white)
    }

    private var daysSection: some View {
        Section {
            ForEach(1...7, id: \.self) { day in
                Toggle(
                    weekdayKey(day),
                    isOn: Binding(
                        get: { selectedDays.contains(day) },
                        set: { enabled in
                            if enabled { selectedDays.insert(day) }
                            else { selectedDays.remove(day) }
                        }
                    )
                )
            }
            if selectedDays.isEmpty {
                Text("create.no_weekdays").font(.footnote).foregroundStyle(CreateAweroStyle.coral)
            }
        } header: {
            Text("create.repeat")
        }
        .listRowBackground(Color.white)
    }

    private var missionSection: some View {
        Section {
            Picker("create.mission", selection: $mission) {
                Text("home.mission.math").tag(MissionType.math)
                Text("home.mission.steps").tag(MissionType.steps)
                Text("home.mission.qr").tag(MissionType.qr)
            }
            if mission == .math {
                Picker("create.difficulty", selection: $difficulty) {
                    Text("difficulty.easy").tag(Difficulty.easy)
                    Text("difficulty.medium").tag(Difficulty.medium)
                    Text("difficulty.hard").tag(Difficulty.hard)
                }
            }
            if mission == .qr {
                Text("permission.camera_body").font(.caption)
                Button("create.scan") { showCodeScanner = true }
                TextField("create.qr_content", text: $qrExpectedCode)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Text("create.qr_instructions").font(.caption)
            }
        } header: {
            Text("create.mission")
        }
        .listRowBackground(Color.white)
    }

    private var permissionSection: some View {
        Section {
            Text("permission.ios_alarm_body").font(.footnote)
        }
        .listRowBackground(Color(red: 1, green: 0.937, blue: 0.859))
    }

    private func saveAlarm() {
        if alarm == nil && !didExplainAlarmPermission {
            showingPermissionIntro = true
        } else {
            saveAlarmNow()
        }
    }

    private func saveAlarmNow() {
        let components = Calendar.current.dateComponents([.hour, .minute], from: wakeDate)
        let hour = components.hour ?? 7
        let minute = components.minute ?? 30
        var next = alarm ?? Alarm(hour: hour, minute: minute, weekdays: selectedDays, missionType: mission)
        next.hour = hour
        next.minute = minute
        next.weekdays = selectedDays
        next.timezoneMode = followsDeviceTimezone ? .deviceLocal : .fixed
        next.fixedTimezone = followsDeviceTimezone ? nil : fixedTimezone
        next.missionType = mission
        next.difficulty = difficulty
        let trimmedCode = qrExpectedCode.trimmingCharacters(in: .whitespacesAndNewlines)
        next.qrExpectedCode = trimmedCode.isEmpty ? nil : trimmedCode

        Task {
            let coordinator = AlarmCoordinator(store: store)
            do {
                if alarm == nil { try await coordinator.create(next) }
                else { try await coordinator.update(next) }
                dismiss()
            } catch {
                saveError = NSLocalizedString("create.error_body", comment: "Alarm save error")
            }
        }
    }

    private func weekdayKey(_ day: Int) -> LocalizedStringKey {
        switch day {
        case 1: "day.sunday"
        case 2: "day.monday"
        case 3: "day.tuesday"
        case 4: "day.wednesday"
        case 5: "day.thursday"
        case 6: "day.friday"
        default: "day.saturday"
        }
    }
}

private struct AlarmCodeScanner: View {
    let onCode: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @StateObject private var runtime = QRMissionRuntime()

    var body: some View {
        NavigationStack {
            VStack(spacing: 18) {
                if runtime.cameraUnavailable {
                    Text("permission.camera_unavailable")
                } else {
                    QRPreview(session: runtime.session).frame(height: 300)
                    Text("create.point_camera")
                }
            }
            .padding()
            .navigationTitle("create.scan")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("create.cancel") { dismiss() }
                }
            }
            .task {
                if AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined {
                    _ = await AVCaptureDevice.requestAccess(for: .video)
                }
                runtime.configure()
                if !runtime.cameraUnavailable { runtime.start() }
            }
            .onChange(of: runtime.scannedCode) { _, code in
                if let code { onCode(code) }
            }
            .onDisappear { runtime.stop() }
        }
    }
}
