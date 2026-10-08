import SwiftUI
import AVFoundation

struct MissionView: View {
    let alarm: Alarm
    let onSuccess: () -> Void
    let onFailure: () -> Void

    var body: some View {
        Group {
            switch alarm.missionType {
            case .math:
                MathMissionView(difficulty: alarm.difficulty, onSuccess: onSuccess)
            case .steps:
                StepsMissionView(onSuccess: onSuccess, onFailure: onFailure)
            case .qr:
                QRMissionView(expected: alarm.qrExpectedCode, onSuccess: onSuccess, onFailure: onFailure)
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
            Text("WAKE MISSION").font(.caption.bold()).foregroundStyle(.white.opacity(0.5))
            if let p = mission.problem {
                Text("\(p.left) \(String(p.operation)) \(p.right) = ?")
                    .font(.system(size: 42, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
            }
            TextField("Answer", text: $answer)
                .keyboardType(.numbersAndPunctuation)
                .textFieldStyle(.roundedBorder)
                .multilineTextAlignment(.center)
            if invalid { Text("Try again").foregroundStyle(.red) }
            Button("CHECK") {
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

    var body: some View {
        VStack(spacing: 22) {
            Text("WALK 30 STEPS")
                .font(.system(size: 34, weight: .black, design: .rounded))
                .foregroundStyle(.white)
            Text("\(runtime.steps) / 30")
                .font(.system(size: 50, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
            if runtime.unavailable {
                Text("Motion data is unavailable.")
                    .foregroundStyle(.white.opacity(0.6))
                Button("USE FALLBACK", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if runtime.completed {
                Button("CONTINUE", action: onSuccess)
                    .buttonStyle(WakeMissionButton())
            } else {
                Text("Keep walking until the target is reached.")
                    .foregroundStyle(.white.opacity(0.6))
            }
        }
        .padding(28)
        .onAppear { runtime.start(target: 30) }
        .onDisappear { runtime.stop() }
    }
}

private struct QRMissionView: View {
    let expected: String?
    let onSuccess: () -> Void
    let onFailure: () -> Void
    @StateObject private var runtime = QRMissionRuntime()

    var body: some View {
        VStack(spacing: 18) {
            Text("SCAN YOUR QR")
                .font(.system(size: 32, weight: .black, design: .rounded))
                .foregroundStyle(.white)

            if expected == nil || expected?.isEmpty == true {
                Text("QR mission is not configured.")
                    .foregroundStyle(.white.opacity(0.6))
                Button("USE FALLBACK", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if runtime.cameraUnavailable {
                Text("Camera is unavailable.")
                    .foregroundStyle(.white.opacity(0.6))
                Button("USE FALLBACK", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if let scannedCode = runtime.scannedCode, scannedCode != expected {
                Text("QR code does not match. Switching to the safe fallback.")
                    .foregroundStyle(.white.opacity(0.7))
            } else {
                QRPreview(session: runtime.session)
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                Text(runtime.scannedCode == nil ? "Point the camera at your wake-up QR." : "Code detected.")
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
        .padding(28)
        .task {
            guard let expected, !expected.isEmpty else { return }
            if AVCaptureDevice.authorizationStatus(for: .video) == .notDetermined {
                _ = await AVCaptureDevice.requestAccess(for: .video)
            }
            runtime.configure()
            runtime.start()
            while !Task.isCancelled {
                if let scannedCode = runtime.scannedCode {
                    if scannedCode == expected {
                        onSuccess()
                    } else {
                        onFailure()
                    }
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
            Text("MISSION UNAVAILABLE").foregroundStyle(.white)
            Button("USE FALLBACK", action: onFailure).buttonStyle(WakeMissionButton())
        }
    }
}

private struct QRPreview: UIViewRepresentable {
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
            .background(Color.white.opacity(configuration.isPressed ? 0.75 : 1))
            .foregroundStyle(.black)
            .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
