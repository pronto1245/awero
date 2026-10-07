import Foundation
import UserNotifications
@MainActor final class AlarmRuntime:NSObject,UNUserNotificationCenterDelegate {
 private let store:AlarmStore;let wakeFlow=WakeFlowController()
 init(store:AlarmStore){self.store=store;super.init();UNUserNotificationCenter.current().delegate=self}
 func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse,withCompletionHandler completionHandler:@escaping()->Void){
  let id=response.notification.request.identifier
  let parts=id.split(separator:":")
  if parts.count >= 5 && parts[0]=="awero" && parts[1]=="alarm",let uuid=UUID(uuidString:String(parts[2])),let alarm=store.alarms.first(where:{$0.id==uuid}){
   if parts[3]=="v\(alarm.version)" {wakeFlow.start(alarm: alarm, scheduledAt: Date())}
  }
  completionHandler()
 }
}