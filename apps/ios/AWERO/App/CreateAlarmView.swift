import SwiftUI
import AVFoundation


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
                daysSection
                missionSection
                timezoneSection
                permissionSection
            }
            .scrollContentBackground(.hidden)
            .background(AweroDesign.ivory)
            .tint(AweroDesign.coral)
            .navigationTitle(alarm == nil ? "create.title" : "create.edit_title")
            .alert("permission.ios_alarm_title", isPresented: $showingPermissionIntro) {
                Button("permission.continue") {
                    didExplainAlarmPermission = true
                    Task {
                        do {
                            let timezoneMode: AlarmTimezoneMode = followsDeviceTimezone ? .deviceLocal : .fixed
                            try await AlarmScheduler().requestAuthorization(for: timezoneMode)
                            saveAlarmNow()
                        } catch {
                            saveError = error.localizedDescription
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
                    .background(AweroDesign.coral)
                    .foregroundStyle(AweroDesign.navy)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    .padding(.horizontal, 20)
                    .padding(.bottom, 8)
                    .background(AweroDesign.ivory.opacity(0.96))
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
                .datePickerStyle(.wheel)
                .labelsHidden()
                .accessibilityLabel(Text("create.time"))
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
                    .foregroundStyle(AweroDesign.navy.opacity(0.65))
            } else {
                Text(
                    String.localizedStringWithFormat(
                        NSLocalizedString("create.timezone_fixed_hint", comment: "Fixed alarm time zone description"),
                        fixedTimezone
                    )
                )
                .font(.footnote)
                .foregroundStyle(AweroDesign.navy.opacity(0.65))
            }
        } header: {
            Text("create.timezone")
        }
        .listRowBackground(Color.white)
    }

    private var daysSection: some View {
        Section {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 92))], spacing: 8) {
                ForEach([2, 3, 4, 5, 6, 7, 1], id: \.self) { day in
                    Button {
                        if selectedDays.contains(day) { selectedDays.remove(day) }
                        else { selectedDays.insert(day) }
                    } label: {
                        Text(weekdayKey(day))
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 48)
                            .foregroundStyle(AweroDesign.navy)
                            .background(selectedDays.contains(day) ? AweroDesign.coral.opacity(0.25) : AweroDesign.ivory)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(selectedDays.contains(day) ? .isSelected : [])
                }
            }
            if selectedDays.isEmpty {
                Text("create.no_weekdays").font(.footnote).foregroundStyle(AweroDesign.coral)
            }
        } header: {
            Text("create.repeat")
        }
        .listRowBackground(Color.white)
    }

    private var missionSection: some View {
        Section {
            ForEach([MissionType.math, .steps, .qr], id: \.self) { type in
                Button {
                    mission = type
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: type == .math ? "plus.forwardslash.minus" : type == .steps ? "figure.walk" : "qrcode")
                            .frame(width: 28)
                            .accessibilityHidden(true)
                        Text(type == .math ? LocalizedStringKey("home.mission.math") : type == .steps ? LocalizedStringKey("home.mission.steps") : LocalizedStringKey("home.mission.qr"))
                        Spacer()
                        if mission == type {
                            Image(systemName: "checkmark.circle.fill")
                                .accessibilityHidden(true)
                        }
                    }
                    .font(.headline)
                    .foregroundStyle(AweroDesign.navy)
                    .padding(12)
                    .frame(minHeight: 48)
                    .background(mission == type ? AweroDesign.coral.opacity(0.25) : AweroDesign.ivory)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(mission == type ? .isSelected : [])
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
                saveError = error.localizedDescription
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
