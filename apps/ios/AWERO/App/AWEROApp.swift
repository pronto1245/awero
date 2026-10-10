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
                    #if DEBUG
                    if ProcessInfo.processInfo.arguments.contains("-awero.uiTestWake") {
                        let requestedMission = ProcessInfo.processInfo.arguments.firstIndex(of: "-awero.uiTestMission")
                            .flatMap { ProcessInfo.processInfo.arguments.indices.contains($0 + 1) ? ProcessInfo.processInfo.arguments[$0 + 1] : nil }
                            .flatMap { MissionType(rawValue: $0.uppercased()) }
                        var alarm = alarmStore.alarms.first ?? Alarm(hour: 7, minute: 0, weekdays: Set(1...7), missionType: requestedMission ?? .math)
                        if let requestedMission { alarm.missionType = requestedMission }
                        await wakeFlow.start(alarm: alarm, isTestAlarm: true)
                        await wakeFlow.beginMission()
                    }
                    #endif
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
