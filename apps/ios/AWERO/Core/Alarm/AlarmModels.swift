import Foundation
enum AlarmTimezoneMode: String, Codable { case deviceLocal = "DEVICE_LOCAL", fixed = "FIXED" }
enum MissionType: String, Codable { case math = "MATH", qr = "QR", steps = "STEPS", photo = "PHOTO", mixed = "MIXED" }
enum Difficulty: String, Codable { case easy = "EASY", medium = "MEDIUM", hard = "HARD" }
struct Alarm: Identifiable, Codable {
 let id: UUID; var version:Int; var hour:Int; var minute:Int; var enabled:Bool; var weekdays:Set<Int>
 var timezoneMode:AlarmTimezoneMode; var fixedTimezone:String?; var missionType:MissionType; var difficulty:Difficulty
 var maxSnoozes:Int; var snoozeMinutes:Int
 init(id:UUID=UUID(),version:Int=1,hour:Int,minute:Int,enabled:Bool=true,weekdays:Set<Int>=Set(1...7),timezoneMode:AlarmTimezoneMode=.deviceLocal,fixedTimezone:String?=nil,missionType:MissionType=.math,difficulty:Difficulty=.medium,maxSnoozes:Int=3,snoozeMinutes:Int=10) {
  self.id=id;self.version=version;self.hour=hour;self.minute=minute;self.enabled=enabled;self.weekdays=weekdays;self.timezoneMode=timezoneMode;self.fixedTimezone=fixedTimezone;self.missionType=missionType;self.difficulty=difficulty;self.maxSnoozes=max(0,maxSnoozes);self.snoozeMinutes=max(1,snoozeMinutes)
 }
}