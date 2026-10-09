import SwiftUI

struct WakeScreen: View {
    @ObservedObject var flow: WakeFlowController
    @ScaledMetric(relativeTo: .largeTitle) private var wakeTitleSize: CGFloat = 54

    var body: some View {
        ZStack {
            WakePalette.ivory.ignoresSafeArea()
            VStack(spacing: 24) {
                Text("AWERO").font(.caption.weight(.bold)).foregroundStyle(WakePalette.navy.opacity(0.45))

                switch flow.state {
                case .ringing:
                    Text("wake.title").font(.system(size: wakeTitleSize, weight: .black, design: .rounded)).foregroundStyle(WakePalette.navy)
                    Button("wake.start") { Task { await flow.beginMission() } }.buttonStyle(PrimaryWakeButton())
                    Button("wake.snooze") { Task { await flow.snooze() } }.foregroundStyle(WakePalette.navy.opacity(0.7))
                    Button("wake.emergency_stop") { Task { await flow.emergencyStop() } }.font(.caption).foregroundStyle(.red.opacity(0.9))
                case .mission:
                    if let alarm = flow.currentAlarm {
                        MissionView(alarm: missionAlarm(from: alarm), onSuccess: { Task { await flow.completeMission() } }, onFailure: { Task { await flow.fallbackToMath() } })
                    }
                    Button("wake.emergency_stop") { Task { await flow.emergencyStop() } }
                        .font(.caption).foregroundStyle(.red.opacity(0.9))
                case .completed:
                    Text("wake.completed_title").font(.title.bold()).foregroundStyle(WakePalette.navy)
                    Text("wake.completed_body").foregroundStyle(WakePalette.navy.opacity(0.55))
                case .emergencyStopped:
                    Text("wake.emergency_stop").font(.title2.bold()).foregroundStyle(WakePalette.navy)
                    Text("wake.stopped_body").foregroundStyle(WakePalette.navy.opacity(0.55))
                case .idle:
                    Text("wake.snoozed_title").font(.title.bold()).foregroundStyle(WakePalette.navy)
                    Text("wake.snoozed_body").foregroundStyle(WakePalette.navy.opacity(0.55))
                }
                if let error = flow.actionError, flow.state == .ringing || flow.state == .mission {
                    Text(error).foregroundStyle(WakePalette.navy.opacity(0.7))
                    Button("wake.retry") { Task { await flow.retryPendingAction() } }
                        .buttonStyle(PrimaryWakeButton())
                }
            }.padding(28)
        }
        .alert("wake.snooze_error_title", isPresented: Binding(
            get: { flow.snoozeError != nil },
            set: { if !$0 { flow.clearSnoozeError() } }
        )) {
            Button("home.ok", role: .cancel) { flow.clearSnoozeError() }
        } message: {
            Group { if let error = flow.snoozeError { Text(error) } else { Text("wake.snooze_error_body") } }
        }
    }

    private func missionAlarm(from alarm: Alarm) -> Alarm {
        var value = alarm
        value.missionType = flow.currentMission
        return value
    }
}

private struct PrimaryWakeButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline.bold()).frame(maxWidth: .infinity).padding(.vertical, 18)
            .background(WakePalette.coral.opacity(configuration.isPressed ? 0.78 : 1))
            .foregroundStyle(.white).clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
