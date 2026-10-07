import SwiftUI

@main
struct AWEROApp: App {
    @StateObject private var alarmStore = AlarmStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(alarmStore)
        }
    }
}
