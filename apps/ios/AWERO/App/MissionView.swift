import SwiftUI
import AVFoundation

enum WakePalette {
    static let ivory = Color(red: 1.0, green: 0.973, blue: 0.937)
    static let navy = Color(red: 0.078, green: 0.161, blue: 0.294)
    static let coral = Color(red: 1.0, green: 0.408, blue: 0.294)
}

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

    init(difficulty: Difficulty, onSuccess: @escaping () -> Void) {
        self.difficulty = difficulty
        self.onSuccess = onSuccess
        let value = MathMission(difficulty: difficulty)
        value.start()
        _mission = State(initialValue: value)
    }

    var body: some View {
        VStack(spacing: 22) {
            Text("mission.title").font(.caption.bold()).foregroundStyle(WakePalette.navy.opacity(0.5))
            if let p = mission.problem {
                Text("\(p.left) \(String(p.operation)) \(p.right) = ?")
                    .font(.system(size: 42, weight: .black, design: .rounded))
                    .foregroundStyle(WakePalette.navy)
            }
            TextField("mission.answer", text: $answer)
                .keyboardType(.numbersAndPunctuation)
                .textFieldStyle(.roundedBorder)
                .multilineTextAlignment(.center)
            if invalid { Text("mission.try_again").foregroundStyle(.red) }
            Button("mission.check") {
                if let value = Int(answer), mission.validate(answer: value) {
                    onSuccess()
                } else {
                    invalid = true
                    answer = ""
                }
            }
            .buttonStyle(WakeMissionButton())
        }
        .padding(28)
    }
}

private struct StepsMissionView: View {
    @StateObject private var runtime = StepsMissionRuntime()
    let onSuccess: () -> Void
    let onFailure: () -> Void
    let timeout: Duration

    var body: some View {
        VStack(spacing: 22) {
            Text("mission.steps_title")
                .font(.system(size: 34, weight: .black, design: .rounded))
                .foregroundStyle(WakePalette.navy)
            Text(String.localizedStringWithFormat(NSLocalizedString("mission.steps_progress", comment: ""), runtime.steps))
                .font(.system(size: 50, weight: .bold, design: .rounded))
                .foregroundStyle(WakePalette.navy)
            if runtime.unavailable {
                Text("mission.motion_unavailable")
                    .foregroundStyle(WakePalette.navy.opacity(0.6))
            } else if runtime.completed {
                Button("mission.continue", action: onSuccess)
                    .buttonStyle(WakeMissionButton())
            } else {
                Text("mission.keep_walking")
                    .foregroundStyle(WakePalette.navy.opacity(0.6))
            }
            Button("mission.use_math", action: onFailure)
                .buttonStyle(WakeMissionButton())
            Text("mission.timeout")
                .font(.caption).foregroundStyle(WakePalette.navy.opacity(0.6))
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

    var body: some View {
        VStack(spacing: 18) {
            Text("mission.qr_title")
                .font(.system(size: 32, weight: .black, design: .rounded))
                .foregroundStyle(WakePalette.navy)

            if expected == nil || expected?.isEmpty == true {
                Text("mission.qr_unconfigured")
                    .foregroundStyle(WakePalette.navy.opacity(0.6))
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if runtime.cameraUnavailable {
                Text("mission.camera_unavailable")
                    .foregroundStyle(WakePalette.navy.opacity(0.6))
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if let scannedCode = runtime.scannedCode, scannedCode != expected {
                Text("mission.qr_mismatch")
                    .foregroundStyle(WakePalette.navy.opacity(0.7))
            } else {
                QRPreview(session: runtime.session)
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                Text(runtime.scannedCode == nil ? "Point the camera at your saved QR or barcode." : "Code detected.")
                    .foregroundStyle(WakePalette.navy.opacity(0.7))
                Text("mission.timeout")
                    .font(.caption).foregroundStyle(WakePalette.navy.opacity(0.6))
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
                    if scannedCode == expected {
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
            Text("mission.unavailable").foregroundStyle(WakePalette.navy)
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
            .background(WakePalette.coral.opacity(configuration.isPressed ? 0.78 : 1))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
