import SwiftUI

struct WakeScreen: View {
    @ObservedObject var flow: WakeFlowController
    @ScaledMetric(relativeTo: .largeTitle) private var wakeTitleSize: CGFloat = 34

    private static let missionBottomID = "wake.missionBottom"

    private var isActive: Bool { flow.state == .ringing || flow.state == .mission }

    var body: some View {
        ZStack(alignment: .top) {
            AweroDesign.ivory.ignoresSafeArea()
            AweroSceneBackground(scene: .dawn)
                .ignoresSafeArea()
                .allowsHitTesting(false)

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 14) {
                        HStack {
                            Text("AWERO")
                                .font(.system(size: 24, weight: .black))
                                .foregroundStyle(AweroDesign.navy)
                            Spacer()
                            if isActive {
                                Button { Task { await flow.emergencyStop() } } label: {
                                    Image(systemName: "xmark")
                                        .font(.headline.weight(.semibold))
                                        .foregroundStyle(AweroDesign.navy)
                                        .frame(width: 42, height: 42)
                                        .background(AweroDesign.surface.opacity(0.72), in: RoundedRectangle(cornerRadius: 12))
                                }
                                .accessibilityLabel(Text("wake.emergency_stop"))
                                .accessibilityIdentifier("wake.close")
                            }
                        }
                        .padding(.top, 8)

                        Text(isActive ? LocalizedStringKey("wake.heading") : LocalizedStringKey("wake.title"))
                            .font(.system(size: wakeTitleSize, weight: .bold))
                            .foregroundStyle(AweroDesign.navy)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)

                        if flow.state == .ringing {
                            VStack(spacing: 14) {
                                Text("wake.active")
                                    .font(.body)
                                    .foregroundStyle(AweroDesign.navy)
                                    .multilineTextAlignment(.center)
                                Button("wake.start") { Task { await flow.beginMission() } }
                                    .buttonStyle(PrimaryWakeButton())
                                Button("wake.snooze") { Task { await flow.snooze() } }
                                    .foregroundStyle(AweroDesign.navy.opacity(0.74))
                                    .frame(minHeight: 44)
                            }
                            .padding(18)
                            .background(AweroDesign.surface.opacity(0.94))
                            .clipShape(RoundedRectangle(cornerRadius: 22))
                        } else if flow.state == .mission, let alarm = flow.currentAlarm {
                            if flow.currentMission == .math {
                                Text("wake.instruction")
                                    .font(.body)
                                    .foregroundStyle(AweroDesign.navy)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 18)
                            }
                            MissionView(
                                alarm: missionAlarm(from: alarm),
                                onSuccess: { Task { await flow.completeMission() } },
                                onFailure: { Task { await flow.fallbackToMath() } }
                            )
                            .padding(.horizontal, 8)
                            // With very large text the mission is taller than the screen. Bring the
                            // answer controls into view first; the heading stays reachable by scrolling.
                            Color.clear.frame(height: 1).id(Self.missionBottomID)
                        } else {
                            terminalState
                        }

                        if let error = flow.actionError, isActive {
                            VStack(alignment: .leading, spacing: 8) {
                                Text(error).foregroundStyle(AweroDesign.navy)
                                Button("wake.retry") { Task { await flow.retryPendingAction() } }
                                    .buttonStyle(PrimaryWakeButton())
                            }
                            .padding(16)
                            .background(AweroDesign.surface.opacity(0.94))
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 12)
                    .frame(maxWidth: .infinity)
                }
                .scrollIndicators(.hidden)
                .task(id: flow.state == .mission ? flow.currentMission.rawValue : "") {
                    guard flow.state == .mission else { return }
                    try? await Task.sleep(for: .milliseconds(150))
                    withAnimation(nil) { proxy.scrollTo(Self.missionBottomID, anchor: .bottom) }
                }
            }
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if isActive {
                Button { Task { await flow.emergencyStop() } } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.headline)
                        VStack(spacing: 2) {
                            Text("wake.emergency_stop").font(.subheadline.weight(.semibold))
                            Text("wake.emergency_stop_hint").font(.caption).foregroundStyle(AweroDesign.navy.opacity(0.58))
                        }
                    }
                    .foregroundStyle(AweroDesign.warning)
                    .frame(maxWidth: .infinity, minHeight: 62)
                    .background(AweroDesign.warningSoft, in: RoundedRectangle(cornerRadius: 22))
                    .overlay(RoundedRectangle(cornerRadius: 22).stroke(AweroDesign.warningLine, lineWidth: 1))
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("wake.emergencyStop")
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 6)
                .background(AweroDesign.ivory.opacity(0.94))
            }
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

    @ViewBuilder
    private var terminalState: some View {
        VStack(spacing: 10) {
            switch flow.state {
            case .completed:
                Image(systemName: "checkmark.circle.fill").font(.system(size: 46)).foregroundStyle(AweroDesign.sage)
                Text("wake.completed_title").accessibilityIdentifier("wake.completed").font(.title2.bold()).foregroundStyle(AweroDesign.navy)
                Text("wake.completed_body").foregroundStyle(AweroDesign.navy.opacity(0.65))
            case .emergencyStopped:
                Image(systemName: "exclamationmark.circle.fill").font(.system(size: 46)).foregroundStyle(AweroDesign.coral)
                Text("wake.emergency_stop").font(.title2.bold()).foregroundStyle(AweroDesign.navy)
                Text("wake.stopped_body").accessibilityIdentifier("wake.stopped").foregroundStyle(AweroDesign.navy.opacity(0.65))
            case .storageError:
                Image(systemName: "externaldrive.badge.exclamationmark").font(.system(size: 42)).foregroundStyle(AweroDesign.coral)
                Text("persistence.read_error_title").font(.title2.bold()).foregroundStyle(AweroDesign.navy)
                Text(flow.storageError ?? String(localized: "persistence.read_error")).foregroundStyle(AweroDesign.navy.opacity(0.7))
                Button("persistence.retry_read") { Task { await flow.restore() } }.buttonStyle(PrimaryWakeButton())
            case .idle:
                Text("wake.snoozed_title").font(.title2.bold()).foregroundStyle(AweroDesign.navy)
                Text("wake.snoozed_body").foregroundStyle(AweroDesign.navy.opacity(0.65))
            default: EmptyView()
            }
        }
        .frame(maxWidth: .infinity)
        .padding(22)
        .background(AweroDesign.surface.opacity(0.94), in: RoundedRectangle(cornerRadius: 22))
    }

    private func missionAlarm(from alarm: Alarm) -> Alarm {
        var value = alarm
        value.missionType = flow.currentMission
        return value
    }
}

private struct PrimaryWakeButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline.bold()).frame(maxWidth: .infinity).padding(.vertical, 16)
            .background(AweroDesign.coralStrong.opacity(configuration.isPressed ? 0.78 : 1))
            .foregroundStyle(.white).clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
    }
}
