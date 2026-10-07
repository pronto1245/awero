package app.awero.core.ai

import app.awero.core.missions.MissionType
import app.awero.core.statistics.WakeStatistics

data class Recommendation(val mission:MissionType,val difficulty:String,val reasonCode:String,val confidence:Double)

class AdaptivePolicy {
    fun recommend(stats:WakeStatistics,current:MissionType):Recommendation {
        return when {
            stats.snoozes >= maxOf(3, stats.planned/2) -> Recommendation(MissionType.QR,"HARD","HIGH_SNOOZE_RATE",0.9)
            stats.successfulWakeRate < 0.7 -> Recommendation(MissionType.STEPS,"MEDIUM","LOW_WAKE_SUCCESS",0.82)
            else -> Recommendation(current,"MEDIUM","STABLE",0.7)
        }
    }
}