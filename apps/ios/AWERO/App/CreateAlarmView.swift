import SwiftUI

struct CreateAlarmView: View {
    let alarm: Alarm?
    @EnvironmentObject private var store: AlarmStore
    @Environment(\.dismiss) private var dismiss

    @State private var wakeDate: Date
    @State private var selectedDays: Set<Int>
    @State private var mission: MissionType

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
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
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
                Text("QR").tag(MissionType.qr)
            }
        }
    }

    private var saveSection: some View {
        Button(alarm == nil ? "Save alarm" : "Save changes") {
            saveAlarm()
        }
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

        Task {
            let coordinator = AlarmCoordinator(store: store)
            if alarm == nil {
                await coordinator.create(next)
            } else {
                await coordinator.update(next)
            }
            dismiss()
        }
    }
}
