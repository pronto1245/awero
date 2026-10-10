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
        VStack(spacing: 12) {
            if let p = mission.problem {
                // The typed answer replaces "?" inside the problem card, as in the reference screen.
                (Text("\(p.left) \(String(p.operation)) \(p.right) = ")
                    .foregroundColor(AweroDesign.navy)
                 + Text(answer.isEmpty ? "?" : answer)
                    .foregroundColor(answer.isEmpty ? AweroDesign.navy : AweroDesign.coral))
                    .font(.system(size: problemSize, weight: .black))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                    .padding(.vertical, 16)
                    .padding(.horizontal, 12)
                    .frame(maxWidth: .infinity)
                    .background(AweroDesign.surface.opacity(0.96))
                    .clipShape(RoundedRectangle(cornerRadius: 22))
                    .accessibilityIdentifier("mission.math.problem")
            }
            if invalid {
                Text("mission.try_again")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AweroDesign.warning)
                    .frame(maxWidth: .infinity, minHeight: 24)
                    .accessibilityIdentifier("mission.math.invalid")
            }
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
        .padding(.vertical, 12)
    }
}

private struct StepsMissionView: View {
    @StateObject private var runtime = StepsMissionRuntime()
    let onSuccess: () -> Void
    let onFailure: () -> Void
    let timeout: Duration
    private let target = 30
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 32
    @ScaledMetric(relativeTo: .largeTitle) private var stepCountSize: CGFloat = 56
    @ScaledMetric(relativeTo: .largeTitle) private var ringSize: CGFloat = 220

    private var progress: Double { min(1, Double(runtime.steps) / Double(target)) }

    var body: some View {
        VStack(spacing: 18) {
            VStack(spacing: 8) {
                Text("mission.steps_heading")
                    .font(.system(size: titleSize, weight: .bold))
                    .foregroundStyle(AweroDesign.navy)
                    .multilineTextAlignment(.center)
                    .accessibilityIdentifier("mission.steps.title")
                Text("mission.steps_subtitle")
                    .font(.body)
                    .foregroundStyle(AweroDesign.textSecondary)
                    .multilineTextAlignment(.center)
            }
            ZStack {
                Circle().stroke(AweroDesign.chip, lineWidth: 22)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(AweroDesign.coral, style: StrokeStyle(lineWidth: 22, lineCap: .butt))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeOut(duration: 0.3), value: progress)
                VStack(spacing: 4) {
                    Image(systemName: "figure.walk")
                        .font(.title.weight(.semibold))
                        .foregroundStyle(AweroDesign.coral)
                    Text("\(runtime.steps)")
                        .font(.system(size: stepCountSize, weight: .heavy))
                        .monospacedDigit()
                        .foregroundStyle(AweroDesign.navy)
                    Text("mission.steps_of_target")
                        .font(.subheadline)
                        .foregroundStyle(AweroDesign.textSecondary)
                }
            }
            .frame(width: ringSize, height: ringSize)
            .padding(.vertical, 6)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(String.localizedStringWithFormat(NSLocalizedString("mission.steps_progress", comment: ""), runtime.steps)))

            if runtime.unavailable {
                Text("mission.motion_unavailable")
                    .foregroundStyle(AweroDesign.textSecondary)
            } else if runtime.completed {
                Button("mission.continue", action: onSuccess)
                    .buttonStyle(WakeMissionButton())
            } else {
                HStack(alignment: .top, spacing: 12) {
                    Image(systemName: "lightbulb.fill")
                        .foregroundStyle(AweroDesign.sage)
                    Text("mission.steps_tip")
                        .foregroundStyle(AweroDesign.navy)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(16)
                .background(AweroDesign.surface.opacity(0.94), in: RoundedRectangle(cornerRadius: 20))
            }
            Button("mission.use_math", action: onFailure)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(AweroDesign.coralStrong)
                .frame(minHeight: 44)
            Text("mission.timeout")
                .font(.caption).foregroundStyle(AweroDesign.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .onAppear { runtime.start(target: target) }
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

    @State private var torchOn = false

    var body: some View {
        VStack(spacing: 18) {
            VStack(spacing: 8) {
                Text("mission.qr_heading")
                    .font(.system(size: qrTitleSize, weight: .bold))
                    .foregroundStyle(AweroDesign.navy)
                    .multilineTextAlignment(.center)
                Text("mission.qr_subtitle")
                    .font(.body)
                    .foregroundStyle(AweroDesign.textSecondary)
                    .multilineTextAlignment(.center)
            }

            if expected == nil || expected?.isEmpty == true {
                Text("mission.qr_unconfigured")
                    .foregroundStyle(AweroDesign.textSecondary)
                    .accessibilityIdentifier("mission.qr.unconfigured")
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if runtime.cameraUnavailable {
                Text("mission.camera_unavailable")
                    .foregroundStyle(AweroDesign.textSecondary)
                Button("mission.use_fallback", action: onFailure)
                    .buttonStyle(WakeMissionButton())
            } else if let scannedCode = runtime.scannedCode, mission?.validate(payload: scannedCode) == false {
                Text("mission.qr_mismatch")
                    .foregroundStyle(AweroDesign.textSecondary)
            } else {
                ZStack(alignment: .topTrailing) {
                    QRPreview(session: runtime.session)
                        .frame(height: 280)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                        .overlay(ScannerFrame().padding(18))
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(Text("mission.qr_instructions"))
                        .accessibilityAddTraits(.isImage)
                    Button { toggleTorch() } label: {
                        Image(systemName: torchOn ? "flashlight.on.fill" : "flashlight.off.fill")
                            .font(.headline)
                            .foregroundStyle(.white)
                            .frame(width: 44, height: 44)
                            .background(Color.white.opacity(0.18), in: RoundedRectangle(cornerRadius: 12))
                    }
                    .accessibilityLabel(Text("mission.flashlight"))
                    .accessibilityValue(torchOn ? Text("home.enabled") : Text("home.disabled"))
                    .padding(10)
                }
                Text(runtime.scannedCode == nil ? "mission.qr_instructions" : "mission.qr_detected")
                    .foregroundStyle(AweroDesign.textSecondary)
                    .multilineTextAlignment(.center)
                Text("mission.timeout")
                    .font(.caption).foregroundStyle(AweroDesign.textSecondary)
                    .multilineTextAlignment(.center)
            }
            Button(action: onFailure) {
                Text("mission.qr_cant_scan")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 16)
                    .foregroundStyle(Color(hex: 0x1D2433))
                    .background(Color.white, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
            }
            .accessibilityHint(Text("mission.use_math"))
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 12)
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
        .onDisappear {
            setTorch(false)
            runtime.stop()
        }
    }

    private func toggleTorch() { setTorch(!torchOn) }

    private func setTorch(_ on: Bool) {
        guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else { return }
        do {
            try device.lockForConfiguration()
            device.torchMode = on ? .on : .off
            device.unlockForConfiguration()
            torchOn = on
        } catch {
            torchOn = false
        }
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

/// Coral corner brackets and a scan line over the camera preview.
private struct ScannerFrame: View {
    var body: some View {
        GeometryReader { geometry in
            let size = geometry.size
            let arm: CGFloat = 34
            Path { path in
                for (x, y, dx, dy) in [(0.0, 0.0, 1.0, 1.0), (size.width, 0.0, -1.0, 1.0), (0.0, size.height, 1.0, -1.0), (size.width, size.height, -1.0, -1.0)] {
                    path.move(to: CGPoint(x: x, y: y + dy * arm))
                    path.addLine(to: CGPoint(x: x, y: y))
                    path.addLine(to: CGPoint(x: x + dx * arm, y: y))
                }
            }
            .stroke(AweroDesign.coral, style: StrokeStyle(lineWidth: 4, lineCap: .round, lineJoin: .round))
            Rectangle()
                .fill(AweroDesign.coral)
                .frame(width: size.width - 28, height: 2)
                .shadow(color: AweroDesign.coral, radius: 6)
                .position(x: size.width / 2, y: size.height / 2)
        }
        .allowsHitTesting(false)
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
            .background(AweroDesign.coralStrong.opacity(configuration.isPressed ? 0.78 : 1))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
    }
}

private struct MathKeyButton: ButtonStyle {
    var primary = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title2.weight(.semibold))
            .foregroundStyle(primary ? Color.white : AweroDesign.navy)
            .frame(maxWidth: .infinity, minHeight: 52)
            .padding(.vertical, 4)
            .background(primary ? AweroDesign.coralStrong : AweroDesign.surface)
            .opacity(configuration.isPressed ? 0.75 : 1)
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}
