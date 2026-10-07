import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var alarms: AlarmStore
    @State private var showingCreate = false

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
                            VStack(alignment: .leading, spacing: 12) {
                                Text("No alarms")
                                    .font(.title2.bold())
                                    .foregroundStyle(.white)
                                Text("Create your first wake-up.")
                                    .foregroundStyle(.white.opacity(0.55))
                            }
                            .padding(.top, 80)
                        } else {
                            ForEach(alarms.alarms) { alarm in
                                AlarmCard(alarm: alarm)
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
            .sheet(isPresented: $showingCreate) {
                CreateAlarmView()
            }
        }
    }
}

private struct AlarmCard: View {
    let alarm: Alarm
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(format: "%02d:%02d", alarm.hour, alarm.minute))
                .font(.system(size: 42, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
            Text(alarm.weekdays.sorted().map(String.init).joined(separator: " · "))
                .foregroundStyle(.white.opacity(0.5))
            Text(alarm.missionType.rawValue)
                .font(.caption.bold())
                .foregroundStyle(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }
}
