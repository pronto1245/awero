import SwiftUI

@main
struct AWEROApp: App {
    @UIApplicationDelegateAdaptor(AWEROAppDelegate.self) private var delegate
    @StateObject private var alarmStore = AlarmStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(alarmStore)
                .task {
                    try? await AlarmScheduler().requestAuthorization()
                }
        }
    }
}
