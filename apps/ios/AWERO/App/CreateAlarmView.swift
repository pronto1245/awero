import SwiftUI

struct CreateAlarmView: View {
    @EnvironmentObject private var store: AlarmStore
    @Environment(\.dismiss) private var dismiss
    @State private var hour = 7
    @State private var minute = 30
    @State private var selectedDays = Set(1...7)
    @State private var mission: MissionType = .math

    var body: some View {
        NavigationStack {
            Form {
                DatePicker("Wake time", selection: Binding(
                    get: { Calendar.current.date(from: DateComponents(hour: hour, minute: minute)) ?? Date() },
                    set: {
                        let c = Calendar.current.dateComponents([.hour, .minute], from: $0)
                        hour = c.hour ?? 7
                        minute = c.minute ?? 30
                    }
                ), displayedComponents: .hourAndMinute)

                Section("Days") {
                    ForEach(1...7, id: \.self) { day in
                        Toggle("Day \(day)", isOn: Binding(
                            get: { selectedDays.contains(day) },
                            set: { $0 ? selectedDays.insert(day) : selectedDays.remove(day) }
                        ))
                    }
                }

                Section("Wake mission") {
                    Picker("Mission", selection: $mission) {
                        Text("Math").tag(MissionType.math)
                        Text("Steps").tag(MissionType.steps)
                        Text("QR").tag(MissionType.qr)
                    }
                }

                Button("Save alarm") {
                    let alarm = Alarm(hour: hour, minute: minute, weekdays: selectedDays, missionType: mission)
                    Task {
                        await AlarmCoordinator(store: store).create(alarm)
                        dismiss()
                    }
                }
            }
            .navigationTitle("Create Alarm")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
        }
    }
}
