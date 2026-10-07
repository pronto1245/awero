import UIKit
import UserNotifications
final class AWEROAppDelegate:NSObject,UIApplicationDelegate,UNUserNotificationCenterDelegate{
 private let wakeFlow=WakeFlowController()
 func application(_ application:UIApplication,didFinishLaunchingWithOptions options:[UIApplication.LaunchOptionsKey:Any]?=nil)->Bool{UNUserNotificationCenter.current().delegate=self;return true}
 func userNotificationCenter(_ center:UNUserNotificationCenter,didReceive response:UNNotificationResponse,withCompletionHandler completionHandler:@escaping()->Void){
  let p=response.notification.request.identifier.split(separator:":")
  guard p.count>=5,p[0]=="awero",p[1]=="alarm",let id=UUID(uuidString:String(p[2])),let v=Int(p[3].replacingOccurrences(of:"v",with:"")) else{completionHandler();return}
  guard let d=UserDefaults.standard.data(forKey:"awero.alarms.v1"),let a=try? JSONDecoder().decode([Alarm].self,from:d),let alarm=a.first(where:{$0.id==id&&$0.version==v}) else{completionHandler();return}
  Task{@MainActor in wakeFlow.start(alarm: alarm, scheduledAt: Date())};completionHandler()
 }
 func userNotificationCenter(_ center:UNUserNotificationCenter,willPresent notification:UNNotification,withCompletionHandler completionHandler:@escaping(UNNotificationPresentationOptions)->Void){completionHandler([.banner,.sound,.badge])}
}