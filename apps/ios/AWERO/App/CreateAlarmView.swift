import SwiftUI
import AVFoundation


struct CreateAlarmView: View {
    let alarm: Alarm?
    @EnvironmentObject private var store: AlarmStore
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
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
    @State private var pendingTest = false
    /// Set once a new alarm is saved, so a retry after a failed test updates it instead of creating another.
    @State private var savedAlarm: Alarm?
    @State private var showAdvanced = false

    init(alarm: Alarm? = nil) {
        self.alarm = alarm
        let calendar = Calendar.current
        let base = calendar.date(from: DateComponents(hour: alarm?.hour ?? 7, minute: alarm?.minute ?? 0)) ?? Date()
        _wakeDate = State(initialValue: base)
        _followsDeviceTimezone = State(initialValue: alarm?.timezoneMode != .fixed)
        _fixedTimezone = State(initialValue: alarm?.fixedTimezone ?? TimeZone.current.identifier)
        _selectedDays = State(initialValue: alarm?.weekdays ?? Set(2...6))
        _mission = State(initialValue: alarm?.missionType ?? .math)
        _difficulty = State(initialValue: alarm?.difficulty ?? .medium)
        _qrExpectedCode = State(initialValue: alarm?.qrExpectedCode ?? "")
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AweroDesign.ivory.ignoresSafeArea()
                VStack(spacing: 0) {
                    ZStack {
                        Text(alarm == nil ? "create.title" : "create.edit_title")
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(AweroDesign.navy)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                            .padding(.horizontal, 88)
                        HStack {
                            Button { dismiss() } label: {
                                Image(systemName: "chevron.left")
                                    .font(.title3.weight(.semibold))
                                    .foregroundStyle(AweroDesign.navy)
                                    .frame(width: 44, height: 44)
                            }
                            .accessibilityLabel(Text("create.cancel"))
                            .accessibilityIdentifier("alarm.cancel")
                            Spacer()
                            Button { saveAlarm(test: false) } label: {
                                Text("create.done")
                                    .font(.headline)
                                    .foregroundStyle(canSave ? AweroDesign.coralStrong : AweroDesign.textSecondary)
                                    .frame(minWidth: 44, minHeight: 44)
                            }
                            .disabled(!canSave)
                            .accessibilityIdentifier("alarm.save")
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 4)

                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            wakeTimeSection
                            daysSection
                            missionSection
                            advancedSection
                        }
                        .padding(.horizontal, 18)
                        .padding(.top, 10)
                        .padding(.bottom, 18)
                    }
                    .scrollIndicators(.hidden)
                }
            }
            .alert("permission.ios_alarm_title", isPresented: $showingPermissionIntro) {
                Button("permission.continue") {
                    didExplainAlarmPermission = true
                    Task {
                        do {
                            let timezoneMode: AlarmTimezoneMode = followsDeviceTimezone ? .deviceLocal : .fixed
                            try await AlarmScheduler().requestAuthorization(for: timezoneMode)
                            saveAlarmNow(test: pendingTest)
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
            .safeAreaInset(edge: .bottom) {
                VStack(spacing: 6) {
                    Button { saveAlarm(test: true) } label: {
                        Text("create.save_and_test")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(canSave ? AweroDesign.coralStrong : AweroDesign.chip)
                            .foregroundStyle(canSave ? Color.white : AweroDesign.textSecondary)
                            .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
                    }
                    .disabled(!canSave)
                    .accessibilityHint(Text("create.save_and_test_hint"))
                    .accessibilityIdentifier("alarm.saveAndTest")
                    Text("create.save_and_test_hint")
                        .font(.caption)
                        .foregroundStyle(AweroDesign.textSecondary)
                        .multilineTextAlignment(.center)
                        .accessibilityHidden(true)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 8)
                .background(AweroDesign.ivory.opacity(0.96))
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
        VStack(alignment: .leading, spacing: 8) {
            Text("create.time")
                .font(.headline)
                .foregroundStyle(AweroDesign.navy)
            DatePicker("create.time", selection: $wakeDate, displayedComponents: .hourAndMinute)
                .datePickerStyle(.wheel)
                .frame(height: dynamicTypeSize.isAccessibilitySize ? 230 : 166)
                .labelsHidden()
                .accessibilityLabel(Text("create.time"))
                .frame(maxWidth: .infinity)
                .background(AweroDesign.surfaceMuted, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
                .clipped()
        }
        .padding(.top, 4)
        .padding(.bottom, 2)
    }

    private var canSave: Bool {
        !selectedDays.isEmpty && !(mission == .qr && qrExpectedCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
    }

    /// Difficulty and time zone, collapsed by default as in the reference screen.
    private var advancedSection: some View {
        DisclosureGroup(isExpanded: $showAdvanced) {
            VStack(alignment: .leading, spacing: 16) {
                if mission == .math {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("create.difficulty")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(AweroDesign.navy)
                        Picker("create.difficulty", selection: $difficulty) {
                            Text("difficulty.easy").tag(Difficulty.easy)
                            Text("difficulty.medium").tag(Difficulty.medium)
                            Text("difficulty.hard").tag(Difficulty.hard)
                        }
                        .pickerStyle(.segmented)
                    }
                }
                timezoneSection
            }
            .padding(.top, 12)
        } label: {
            Text("create.advanced")
                .font(.headline)
                .foregroundStyle(AweroDesign.navy)
        }
        .tint(AweroDesign.navy)
        .padding(14)
        .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
    }

    private var timezoneSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("create.timezone")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AweroDesign.navy)
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
        }
        .padding(.vertical, 2)
    }

    private var daysSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("create.repeat")
                .font(.headline)
                .foregroundStyle(AweroDesign.navy)
            LazyVGrid(columns: [GridItem(.adaptive(minimum: dynamicTypeSize.isAccessibilitySize ? 80 : 36))], spacing: 6) {
                ForEach([2, 3, 4, 5, 6, 7, 1], id: \.self) { day in
                    Button {
                        if selectedDays.contains(day) { selectedDays.remove(day) }
                        else { selectedDays.insert(day) }
                    } label: {
                        Text(Calendar.current.shortWeekdaySymbols[day - 1])
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .foregroundStyle(selectedDays.contains(day) ? Color.white : AweroDesign.navy.opacity(0.68))
                            .background(selectedDays.contains(day) ? AweroDesign.coralStrong : AweroDesign.chip)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(weekdayKey(day)))
                    .accessibilityAddTraits(selectedDays.contains(day) ? .isSelected : [])
                }
            }
            .frame(maxWidth: .infinity)
            if selectedDays.isEmpty {
                Text("create.no_weekdays").font(.footnote).foregroundStyle(AweroDesign.coral)
            }
        }
        .padding(.vertical, 2)
    }

    private var missionSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("create.mission")
                .font(.headline)
                .foregroundStyle(AweroDesign.navy)
            Group {
                if dynamicTypeSize.isAccessibilitySize {
                    VStack(spacing: 8) {
                        ForEach([MissionType.math, .steps, .qr], id: \.self) { missionOption($0) }
                    }
                } else {
                    HStack(alignment: .top, spacing: 8) {
                        ForEach([MissionType.math, .steps, .qr], id: \.self) { missionOption($0) }
                    }
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
        }
        .padding(.vertical, 2)
    }

    private func missionOption(_ type: MissionType) -> some View {
        Button { mission = type } label: {
            VStack(spacing: 8) {
                Image(systemName: type == .math ? "calculator" : type == .steps ? "shoe.fill" : "qrcode")
                    .font(.system(size: 27, weight: .semibold))
                    .foregroundStyle(mission == type ? AweroDesign.coral : AweroDesign.navy)
                    .accessibilityHidden(true)
                Text(type == .math ? LocalizedStringKey("home.mission.math") : type == .steps ? LocalizedStringKey("home.mission.steps") : LocalizedStringKey("home.mission.qr"))
                    .font(.subheadline.weight(.semibold))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
                Text(type == .math ? "create.mission_math_body" : type == .steps ? "create.mission_steps_body" : "create.mission_qr_body")
                    .font(.caption)
                    .foregroundStyle(AweroDesign.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .accessibilityIdentifier(type == .math ? "create.mission_math_body" : type == .steps ? "create.mission_steps_body" : "create.mission_qr_body")
            }
            .foregroundStyle(AweroDesign.navy)
            .padding(8)
            .frame(maxWidth: .infinity, minHeight: 148)
            .background(mission == type ? AweroDesign.coralSoft : AweroDesign.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(mission == type ? AweroDesign.coral : AweroDesign.navy.opacity(0.10), lineWidth: mission == type ? 1.5 : 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(mission == type ? .isSelected : [])
    }

    private func saveAlarm(test: Bool) {
        guard canSave else { return }
        pendingTest = test
        if alarm == nil && savedAlarm == nil && !didExplainAlarmPermission {
            showingPermissionIntro = true
        } else {
            saveAlarmNow(test: test)
        }
    }

    /// Saves the alarm; with `test`, also schedules a test ring (30 s) that does not count as a wake.
    private func saveAlarmNow(test: Bool) {
        let components = Calendar.current.dateComponents([.hour, .minute], from: wakeDate)
        let hour = components.hour ?? 7
        let minute = components.minute ?? 30
        let existing = savedAlarm ?? alarm
        var next = existing ?? Alarm(hour: hour, minute: minute, weekdays: selectedDays, missionType: mission)
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
                if existing == nil { try await coordinator.create(next) }
                else { try await coordinator.update(next) }
                savedAlarm = next
                if test { try await coordinator.test(next) }
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
