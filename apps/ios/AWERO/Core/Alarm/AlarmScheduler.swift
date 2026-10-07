import Foundation
import UserNotifications
final class AlarmScheduler {
 private let center=UNUserNotificationCenter.current()
 func requestAuthorization() async throws {try await center.requestAuthorization(options:[.alert,.sound,.badge])}
 func schedule(_ alarm:Alarm) async throws {
  await cancel(alarm); guard alarm.enabled && !alarm.weekdays.isEmpty else{return}
  for day in alarm.weekdays.sorted(){
   var c=DateComponents();c.calendar=Calendar(identifier:.gregorian);c.weekday=day;c.hour=alarm.hour;c.minute=alarm.minute
   if alarm.timezoneMode == .fixed {c.timeZone=TimeZone(identifier:alarm.fixedTimezone ?? "")}
   let content=UNMutableNotificationContent();content.title="AWERO";content.body="Wake up. Stay up.";content.sound=.default
   let id="awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w\(day)"
   try await center.add(UNNotificationRequest(identifier:id,content:content,trigger:UNCalendarNotificationTrigger(dateMatching:c,repeats:true)))
  }
 }
 func scheduleTest(for alarm:Alarm,after seconds:TimeInterval=30) async throws {
  let c=UNMutableNotificationContent();c.title="AWERO — Test Alarm";c.body="Your alarm test is working.";c.sound=.default
  try await center.add(UNNotificationRequest(identifier:"awero:test:\(alarm.id.uuidString):\(UUID().uuidString)",content:c,trigger:UNTimeIntervalNotificationTrigger(timeInterval:max(5,seconds),repeats:false)))
 }
 func cancel(_ alarm:Alarm) async {
  let p="awero:alarm:\(alarm.id.uuidString):";let ids=(await center.pendingNotificationRequests()).map(\.identifier).filter{$0.hasPrefix(p)}
  center.removePendingNotificationRequests(withIdentifiers:ids)
 }
 func isScheduled(_ alarm:Alarm) async -> Bool {
  guard alarm.enabled && !alarm.weekdays.isEmpty else{return false}
  let ids=Set((await center.pendingNotificationRequests()).map(\.identifier))
  return alarm.weekdays.allSatisfy{ids.contains("awero:alarm:\(alarm.id.uuidString):v\(alarm.version):w\($0)")}
 }
 func repair(_ alarm:Alarm) async throws {try await schedule(alarm)}
}