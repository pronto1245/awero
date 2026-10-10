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
                    HStack {
                        Button { dismiss() } label: {
                            Text("create.cancel")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundStyle(AweroDesign.navy)
                                .padding(.horizontal, 18)
                                .frame(height: 44)
                                .background(Color.white.opacity(0.88), in: Capsule())
                        }
                        .accessibilityLabel(Text("create.cancel"))
                        .accessibilityIdentifier("alarm.cancel")
                        Spacer(minLength: 4)
                        Text(alarm == nil ? "create.title" : "create.edit_title")
                            .font(.headline.weight(.semibold))
                            .foregroundStyle(AweroDesign.navy)
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                        Spacer(minLength: 4)
                        Color.clear.frame(width: 74, height: 44)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 4)

                    ScrollView {
                        VStack(alignment: .leading, spacing: 18) {
                            wakeTimeSection
                            daysSection
                            missionSection
                            timezoneSection
                            permissionSection
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
            .safeAreaInset(edge: .bottom) {
                Button(alarm == nil ? "create.save" : "create.save_changes", action: saveAlarm)
                    .disabled((mission == .qr && qrExpectedCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) || selectedDays.isEmpty)
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .background(AweroDesign.coral.gradient)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
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
                .background(AweroDesign.surfaceWarm.opacity(0.48), in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
                .clipped()
        }
        .padding(.top, 4)
        .padding(.bottom, 2)
    }

    private var timezoneSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("create.timezone")
                .font(.headline)
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
                            .background(selectedDays.contains(day) ? AweroDesign.coral : Color.white.opacity(0.72))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text(weekdayKey(day)))
                    .accessibilityAddTraits(selectedDays.contains(day) ? .isSelected : [])
                }
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity)
            .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
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
            if dynamicTypeSize.isAccessibilitySize {
                VStack(spacing: 8) {
                    ForEach([MissionType.math, .steps, .qr], id: \.self) { missionOption($0) }
                }
            } else {
                HStack(alignment: .top, spacing: 8) {
                    ForEach([MissionType.math, .steps, .qr], id: \.self) { missionOption($0) }
                }
            }
            .padding(8)
            .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
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
                    .foregroundStyle(AweroDesign.navy.opacity(0.64))
                    .multilineTextAlignment(.center)
                    .lineLimit(3)
                    .accessibilityIdentifier(type == .math ? "create.mission_math_body" : type == .steps ? "create.mission_steps_body" : "create.mission_qr_body")
            }
            .foregroundStyle(AweroDesign.navy)
            .padding(8)
            .frame(maxWidth: .infinity, minHeight: 148)
            .background(mission == type ? Color(red: 1, green: 0.93, blue: 0.87) : AweroDesign.ivory.opacity(0.58))
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(mission == type ? AweroDesign.coral : AweroDesign.navy.opacity(0.10), lineWidth: mission == type ? 1.5 : 1))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(mission == type ? .isSelected : [])
    }

    private var permissionSection: some View {
        VStack(alignment: .leading) {
            Text("permission.ios_alarm_body").font(.footnote)
        }
        .foregroundStyle(AweroDesign.navy.opacity(0.72))
        .padding(14)
        .background(Color(red: 1, green: 0.94, blue: 0.86))
        .clipShape(RoundedRectangle(cornerRadius: 18))
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
