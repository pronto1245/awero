import Foundation
import AVFoundation

@MainActor
final class QRMissionRuntime: NSObject, ObservableObject, AVCaptureMetadataOutputObjectsDelegate {
    @Published private(set) var scannedCode: String?
    @Published private(set) var cameraUnavailable = false
    let session = AVCaptureSession()
    private var output: AVCaptureMetadataOutput?
    private var configured = false

    deinit {
        session.stopRunning()
    }

    func configure() {
        guard !configured else { return }
        guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else {
            cameraUnavailable = true
            return
        }
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else {
            cameraUnavailable = true
            return
        }
        session.beginConfiguration()
        session.addInput(input)
        let metadata = AVCaptureMetadataOutput()
        guard session.canAddOutput(metadata) else {
            session.commitConfiguration()
            cameraUnavailable = true
            return
        }
        session.addOutput(metadata)
        metadata.setMetadataObjectsDelegate(self, queue: .main)
        let supportedTypes: [AVMetadataObject.ObjectType] = [
            .qr, .ean8, .ean13, .upce, .code39, .code93, .code128, .pdf417, .aztec, .dataMatrix
        ]
        let availableTypes = supportedTypes.filter { metadata.availableMetadataObjectTypes.contains($0) }
        guard !availableTypes.isEmpty else {
            session.commitConfiguration()
            cameraUnavailable = true
            return
        }
        metadata.metadataObjectTypes = availableTypes
        output = metadata
        configured = true
        session.commitConfiguration()
    }

    func start() {
        if !session.isRunning { session.startRunning() }
    }

    func stop() {
        if session.isRunning { session.stopRunning() }
    }

    nonisolated func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput objects: [AVMetadataObject], from connection: AVCaptureConnection) {
        guard let code = objects.compactMap({ $0 as? AVMetadataMachineReadableCodeObject }).first?.stringValue else { return }
        Task { @MainActor [weak self] in
            guard let self else { return }
            scannedCode = code
            stop()
        }
    }

    func matches(expected: String) -> Bool {
        scannedCode == expected
    }
}
