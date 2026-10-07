import SwiftUI
@main struct AWEROApp:App {
 @StateObject private var alarmStore=AlarmStore()
 private let scheduler=AlarmScheduler()
 var body:some Scene{WindowGroup{ContentView().environmentObject(alarmStore).task{try? await scheduler.requestAuthorization();await AlarmRecovery(scheduler:scheduler,store:alarmStore).reconcile()}}}
}