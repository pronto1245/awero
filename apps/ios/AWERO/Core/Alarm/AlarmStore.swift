import Foundation
@MainActor final class AlarmStore: ObservableObject {
 @Published private(set) var alarms:[Alarm]=[]
 private let key="awero.alarms.v1"
 init(){load()}
 func load(){ guard let d=UserDefaults.standard.data(forKey:key),let v=try? JSONDecoder().decode([Alarm].self,from:d) else {alarms=[];return};alarms=v }
 func save(_ a:Alarm){if let i=alarms.firstIndex(where:{$0.id==a.id}){alarms[i]=a}else{alarms.append(a)};persist()}
 func update(_ a:Alarm){var n=a;n.version += 1;save(n)}
 func delete(_ a:Alarm){alarms.removeAll{$0.id==a.id};persist()}
 private func persist(){if let d=try? JSONEncoder().encode(alarms){UserDefaults.standard.set(d,forKey:key)}}
}