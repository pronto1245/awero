import SwiftUI

struct WakeScreen: View {
    @ObservedObject var flow: WakeFlowController

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            VStack(spacing: 28) {
                Text("AWERO")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.white.opacity(0.45))

                Text("GET UP")
                    .font(.system(size: 54, weight: .black, design: .rounded))
                    .foregroundStyle(.white)

                if flow.state == .ringing {
                    Text("The alarm is active.")
                        .foregroundStyle(.white.opacity(0.6))

                    Button("Start mission") {
                        flow.beginMission()
                    }
                    .buttonStyle(PrimaryWakeButton())

                    Button("Snooze") {
                        flow.snooze()
                    }
                    .foregroundStyle(.white.opacity(0.7))

                    Button("Emergency stop") {
                        flow.emergencyStop()
                    }
                    .font(.caption)
                    .foregroundStyle(.red.opacity(0.9))
                } else if flow.state == .mission {
                    Text(flow.currentMission.rawValue.uppercased())
                        .font(.title2.bold())
                        .foregroundStyle(.white.opacity(0.7))

                    Text("Mission runtime is next.")
                        .foregroundStyle(.white.opacity(0.55))

                    Button("Complete mission") {
                        flow.completeMission()
                    }
                    .buttonStyle(PrimaryWakeButton())
                } else if flow.state == .completed {
                    Text("YOU'RE UP")
                        .font(.title.bold())
                        .foregroundStyle(.white)

                    Text("Wake session completed.")
                        .foregroundStyle(.white.opacity(0.55))
                } else if flow.state == .emergencyStopped {
                    Text("Emergency stop")
                        .font(.title2.bold())
                        .foregroundStyle(.white)

                    Text("The session was recorded.")
                        .foregroundStyle(.white.opacity(0.55))
                }
            }
            .padding(28)
        }
    }
}

private struct PrimaryWakeButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.bold())
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(Color.white.opacity(configuration.isPressed ? 0.75 : 1))
            .foregroundStyle(.black)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
