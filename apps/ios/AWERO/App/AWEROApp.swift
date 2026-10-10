import SwiftUI
import UIKit

@main
struct AWEROApp: App {
    @UIApplicationDelegateAdaptor(AWEROAppDelegate.self) private var delegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var alarmStore: AlarmStore
    @StateObject private var wakeFlow = WakeFlowController.shared
    @State private var recoveryTrigger: AlarmRecoveryTrigger

    init() {
        let store = AlarmStore()
        _alarmStore = StateObject(wrappedValue: store)
        _recoveryTrigger = State(initialValue: AlarmRecoveryTrigger(store: store))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(alarmStore)
                .environmentObject(wakeFlow)
                .task {
                    await alarmStore.load()
                    recoveryTrigger.appDidBecomeActive()
                    await wakeFlow.restore()
                }
                .onChange(of: scenePhase) { phase in
                    guard phase == .active else { return }
                    recoveryTrigger.appDidBecomeActive()
                    Task {
                        await OfflineSyncCoordinator.shared.runOnce()
                        await alarmStore.load()
                        recoveryTrigger.appDidBecomeActive()
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.significantTimeChangeNotification)) { _ in
                    recoveryTrigger.deviceTimeChanged()
                }
                .onReceive(NotificationCenter.default.publisher(for: .NSSystemTimeZoneDidChange)) { _ in
                    recoveryTrigger.deviceTimeChanged()
                }
        }
    }
}
