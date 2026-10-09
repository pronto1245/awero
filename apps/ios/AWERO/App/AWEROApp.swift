import SwiftUI

@main
struct AWEROApp: App {
    @UIApplicationDelegateAdaptor(AWEROAppDelegate.self) private var delegate
    @StateObject private var alarmStore = AlarmStore()
    @StateObject private var wakeFlow = WakeFlowController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(alarmStore)
                .environmentObject(wakeFlow)
                .task {
                    try? await AlarmScheduler().requestAuthorization()
                    await AlarmCoordinator(store: alarmStore).repairAll()
                    await wakeFlow.restore()
                    Task { await OfflineSyncCoordinator.shared.runOnce() }
                }
        }
    }
}
