import SwiftUI
import AVFoundation


struct MissionView: View {
    let alarm: Alarm
    let onSuccess: () -> Void
    let onFailure: () -> Void
    var timeout: Duration = .seconds(120)

    var body: some View {
        Group {
            switch alarm.missionType {
            case .math:
                MathMissionView(difficulty: alarm.difficulty, onSuccess: onSuccess)
            case .steps:
                StepsMissionView(onSuccess: onSuccess, onFailure: onFailure, timeout: timeout)
            case .qr:
                QRMissionView(expected: alarm.qrExpectedCode, onSuccess: onSuccess, onFailure: onFailure, timeout: timeout)
            default:
                FallbackMissionView(onFailure: onFailure)
            }
        }
    }
}

private struct MathMissionView: View {
    let difficulty: Difficulty
    let onSuccess: () -> Void
    @State private var mission: MathMission
    @State private var answer = ""
    @State private var invalid = false
    @ScaledMetric(relativeTo: .largeTitle) private var problemSize: CGFloat = 42

    init(difficulty: Difficulty, onSuccess: @escaping () -> Void) {
        self.difficulty = difficulty
        self.onSuccess = onSuccess
        let value = MathMission(difficulty: difficulty)
        value.start()
        _mission = State(initialValue: value)
    }

    var body: some View {
        VStack(spacing: 22) {
            Text("mission.title").font(.caption.bold()).foregroundStyle(AweroDesign.navy.opacity(0.5))
            if let p = mission.problem {
                Text("\(p.left) \(String(p.operation)) \(p.right) = ?")
                    .font(.system(size: problemSize, weight: .black, design: .rounded))
                    .foregroundStyle(AweroDesign.navy)
                    .accessibilityIdentifier("mission.math.problem")
            }
            TextField("mission.answer", text: $answer)
                .keyboardType(.numbersAndPunctuation)
                .textFieldStyle(.roundedBorder)
                .multilineTextAlignment(.center)
                .accessibilityIdentifier("mission.math.answer")
            if invalid { Text("mission.try_again").foregroundStyle(.red).accessibilityIdentifier("mission.math.invalid") }
            Button("±") {
                answer = answer.hasPrefix("-") ? String(answer.dropFirst()) : "-" + answer
                invalid = false
            }
            .foregroundStyle(AweroDesign.navy)
            .frame(minHeight: 44)
            .accessibilityLabel(Text("mission.change_sign"))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 8) {
                ForEach(1...9, id: \.self) { digit in
                    Button(String(digit)) {
                        if answer.count < 12 { answer += String(digit) }
                        invalid = false
                    }
                    .buttonStyle(MathKeyButton())
                }
                Button {
                    if !answer.isEmpty { answer.removeLast() }
                    invalid = false
                } label: { Image(systemName: "delete.left") }
                    .buttonStyle(MathKeyButton())
                    .accessibilityLabel(Text("mission.delete_digit"))
                    .accessibilityIdentifier("mission.math.delete")
                Button("0") {
                    if answer.count < 12 { answer += "0" }
                    invalid = false
                }
                .buttonStyle(MathKeyButton())
                Button {
                    if let value = Int(answer), mission.validate(answer: value) {
                        onSuccess()
                    } else {
                        invalid = true
                        answer = ""
                    }
                } label: { Image(systemName: "checkmark") }
                    .buttonStyle(MathKeyButton(primary: true))
                    .accessibilityLabel(Text("mission.check"))
                    .accessibilityIdentifier("mission.math.check")
            }
        }
        .padding(28)
    }
}

private struct StepsMissionView: View {
    @StateObject private var runtime = StepsMissionRuntime()
    let onSuccess: () -> Void
    let onFailure: () -> Void
    let timeout: Duration
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 34
    @ScaledMetric(relativeTo: .largeTitle) private var stepCountSize: CGFloat = 50

    var body: some View {
        VStack(spacing: 22) {
            Text("mission.steps_title")
                .font(.system(size: titleSize, weight: .black, design: .rounded))
                .foregroundStyle(AweroDesign.navy)
            Text(String.localizedStringWithFormat(NSLocalizedString("mission.steps_progress", comment: ""), runtime.steps))
                .font(.system(size: stepCountSize, weight: .bold, design: .rounded))
                .foregroundStyle(AweroDesign.navy)
            if runtime.unavailable {
                Text("mission.motion_unavailable")
                    .foregroundStyle(AweroDesign.navy.opacity(0.6))
            } else if runtime.completed {
                Button("mission.continue", action: onSuccess)
                    .buttonStyle(WakeMissionButton())
            } else {
                Text("mission.keep_walking")
                    .foregroundStyle(AweroDesign.navy.opacity(0.6))
            }
            Button("mission.use_math", action: onFailure)
                .buttonStyle(WakeMissionButton())
            Text("mission.timeout")
                .font(.caption).foregroundStyle(AweroDesign.navy.opacity(0.6))
        }
        .padding(28)
        .onAppear { runtime.start(target: 30) }
        .onDisappear { runtime.stop() }
        .task {
            do { try await Task.sleep(for: timeout) } catch { return }
            guard !runtime.completed else { return }
            runtime.stop()
            onFailure()
        }
    }
}

private struct QRMissionView: View {
    let expected: String?
    let onSuccess: () -> Void
    let onFailure: () -> Void
    let timeout: Duration
    @StateObject private var runtime = QRMissionRuntime()
    @ScaledMetric(relativeTo: .largeTitle) private var qrTitleSize: CGFloat = 32
    private var mission: QRMission? {
        guard let expected, !expected.isEmpty else { return nil }
        return QRMission(expectedPayload: expected)
    }

    var body: some View {
        VStack(spacing: 18) {
            Text("mission.qr_title")
                .font(.system(size: qrTitleSize, weight: .black, design: .rounded))
                .foregroundStyle(AweroDesign.navy)

            if expected == nil || expected?.isEmpty == true {
                Text("mission.qr_unconfigured")
                    .foregroundStyle(AweroDesign.navy.opacity(0.6))
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if runtime.cameraUnavailable {
                Text("mission.camera_unavailable")
                    .foregroundStyle(AweroDesign.navy.opacity(0.6))
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if let scannedCode = runtime.scannedCode, mission?.validate(payload: scannedCode) == false {
                Text("mission.qr_mismatch")
                    .foregroundStyle(AweroDesign.navy.opacity(0.7))
            } else {
                QRPreview(session: runtime.session)
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Text("mission.qr_instructions"))
                    .accessibilityAddTraits(.isImage)
                Group {
                    if runtime.scannedCode == nil {
                        Text("mission.qr_instructions")
                    } else {
                        Text("mission.qr_detected")
                    }
                }
                .foregroundStyle(AweroDesign.navy.opacity(0.7))
                Text("mission.timeout")
                    .font(.caption).foregroundStyle(AweroDesign.navy.opacity(0.6))
            }
        }
        .safeAreaInset(edge: .bottom) {
            Button("mission.use_math", action: onFailure)
                .buttonStyle(WakeMissionButton())
                .padding(.horizontal, 28)
        }
        .padding(28)
        .task {
            guard let expected, !expected.isEmpty else { return }
            if AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined {
                _ = await AVCaptureDevice.requestAccess(for: .video)
            }
            runtime.configure()
            guard !runtime.cameraUnavailable else { onFailure(); return }
            runtime.start()
            let deadline = ContinuousClock.now.advanced(by: timeout)
            while !Task.isCancelled {
                if let scannedCode = runtime.scannedCode {
                    if mission?.validate(payload: scannedCode) == true {
                        onSuccess()
                    } else {
                        onFailure()
                    }
                    break
                }
                if ContinuousClock.now >= deadline {
                    runtime.stop()
                    onFailure()
                    break
                }
                try? await Task.sleep(for: .milliseconds(200))
            }
        }
        .onDisappear { runtime.stop() }
    }
}

private struct FallbackMissionView: View {
    let onFailure: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            Text("mission.unavailable").foregroundStyle(AweroDesign.navy)
            Button("mission.use_fallback", action: onFailure).buttonStyle(WakeMissionButton())
        }
    }
}

struct QRPreview: UIViewRepresentable {
    let session: AVCaptureSession
    func makeUIView(context: Context) -> UIView {
        let view = UIView()
        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(layer)
        DispatchQueue.main.async { layer.frame = view.bounds }
        return view
    }
    func updateUIView(_ uiView: UIView, context: Context) {
        uiView.layer.sublayers?.first?.frame = uiView.bounds
    }
}

private struct WakeMissionButton: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline.bold())
            .frame(maxWidth: .infinity)
            .padding(.vertical, 17)
            .background(AweroDesign.coral.opacity(configuration.isPressed ? 0.78 : 1))
            .foregroundStyle(AweroDesign.navy)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct MathKeyButton: ButtonStyle {
    var primary = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title2.weight(.semibold))
            .foregroundStyle(AweroDesign.navy)
            .frame(maxWidth: .infinity, minHeight: 52)
            .padding(.vertical, 4)
            .background(primary ? AweroDesign.coral : Color.white)
            .opacity(configuration.isPressed ? 0.75 : 1)
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
