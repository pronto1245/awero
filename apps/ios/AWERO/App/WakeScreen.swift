import SwiftUI

struct WakeScreen: View {
    @ObservedObject var flow: WakeFlowController

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 24) {
                Text("AWERO").font(.caption.weight(.bold)).foregroundStyle(.white.opacity(0.45))

                switch flow.state {
                case .ringing:
                    Text("GET UP").font(.system(size: 54, weight: .black, design: .rounded)).foregroundStyle(.white)
                    Button("Start mission") { flow.beginMission() }.buttonStyle(PrimaryWakeButton())
                    Button("Snooze") { flow.snooze() }.foregroundStyle(.white.opacity(0.7))
                    Button("Emergency stop") { flow.emergencyStop() }.font(.caption).foregroundStyle(.red.opacity(0.9))
                case .mission:
                    if let alarm = flow.currentAlarm {
                        MissionView(alarm: alarm.copy(missionType: flow.currentMission), onSuccess: { flow.completeMission() }, onFailure: { flow.fallbackToMath() })
                    }
                case .completed:
                    Text("YOU'RE UP").font(.title.bold()).foregroundStyle(.white)
                    Text("Wake session completed.").foregroundStyle(.white.opacity(0.55))
                case .emergencyStopped:
                    Text("Emergency stop").font(.title2.bold()).foregroundStyle(.white)
                    Text("The session was recorded.").foregroundStyle(.white.opacity(0.55))
                case .idle:
                    Text("SNOOZED").font(.title.bold()).foregroundStyle(.white)
                    Text("Your next wake-up is scheduled.").foregroundStyle(.white.opacity(0.55))
                }
            }.padding(28)
        }
    }
}

private struct PrimaryWakeButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline.bold()).frame(maxWidth: .infinity).padding(.vertical, 18)
            .background(Color.white.opacity(configuration.isPressed ? 0.75 : 1))
            .foregroundStyle(.black).clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
