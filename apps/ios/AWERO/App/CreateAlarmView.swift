import SwiftUI
import AVFoundation

struct CreateAlarmView: View {
    let alarm: Alarm?
    @EnvironmentObject private var store: AlarmStore
    @Environment(\.dismiss) private var dismiss

    @State private var wakeDate: Date
    @State private var selectedDays: Set<Int>
    @State private var mission: MissionType
    @State private var difficulty: Difficulty
    @State private var qrExpectedCode: String
    @State private var showCodeScanner = false
    @State private var saveError: String?

    init(alarm: Alarm? = nil) {
        self.alarm = alarm
        let calendar = Calendar.current
        let base = calendar.date(from: DateComponents(
            hour: alarm?.hour ?? 7,
            minute: alarm?.minute ?? 30
        )) ?? Date()
        _wakeDate = State(initialValue: base)
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
                saveSection
            }
            .navigationTitle(alarm == nil ? "Create Alarm" : "Edit Alarm")
            .alert("Alarm not scheduled", isPresented: Binding(
                get: { saveError != nil },
                set: { if !$0 { saveError = nil } }
            )) {
                Button("OK", role: .cancel) { saveError = nil }
            } message: {
                Text(saveError ?? "Please try again.")
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
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
        DatePicker(
            "Wake time",
            selection: $wakeDate,
            displayedComponents: .hourAndMinute
        )
    }

    private var daysSection: some View {
        Section("Days") {
            ForEach(1...7, id: \.self) { day in
                Toggle(
                    "Day \(day)",
                    isOn: Binding(
                        get: { selectedDays.contains(day) },
                        set: { enabled in
                            if enabled {
                                selectedDays.insert(day)
                            } else {
                                selectedDays.remove(day)
                            }
                        }
                    )
                )
            }
        }
    }

    private var missionSection: some View {
        Section("Wake mission") {
            Picker("Mission", selection: $mission) {
                Text("Math").tag(MissionType.math)
                Text("Steps").tag(MissionType.steps)
                Text("QR/barcode").tag(MissionType.qr)
            }
            if mission == .math {
                Picker("Difficulty", selection: $difficulty) {
                    Text("Easy").tag(Difficulty.easy)
                    Text("Medium").tag(Difficulty.medium)
                    Text("Hard").tag(Difficulty.hard)
                }
            }
            if mission == .qr {
                Button("Scan QR/barcode") { showCodeScanner = true }
                TextField("QR/barcode content", text: $qrExpectedCode)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                Text("Scan a code where you want to wake up, or enter its exact content.")
                    .font(.caption)
            }
        }
    }

    private var saveSection: some View {
        Button(alarm == nil ? "Save alarm" : "Save changes") {
            saveAlarm()
        }
        .disabled(mission == .qr && qrExpectedCode.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
    }

    private func saveAlarm() {
        let components = Calendar.current.dateComponents([.hour, .minute], from: wakeDate)
        let hour = components.hour ?? 7
        let minute = components.minute ?? 30

        var next = alarm ?? Alarm(
            hour: hour,
            minute: minute,
            weekdays: selectedDays,
            missionType: mission
        )
        next.hour = hour
        next.minute = minute
        next.weekdays = selectedDays
        next.missionType = mission
        next.difficulty = difficulty
        next.qrExpectedCode = qrExpectedCode.isEmpty ? nil : qrExpectedCode

        Task {
            let coordinator = AlarmCoordinator(store: store)
            do {
                if alarm == nil {
                    try await coordinator.create(next)
                } else {
                    try await coordinator.update(next)
                }
                dismiss()
            } catch {
                saveError = error.localizedDescription
            }
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
                    Text("Camera is unavailable. You can enter the code content manually.")
                } else {
                    QRPreview(session: runtime.session)
                        .frame(height: 300)
                    Text("Point the camera at the QR or barcode you will use in the morning.")
                }
            }
            .padding()
            .navigationTitle("Scan QR/barcode")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
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
