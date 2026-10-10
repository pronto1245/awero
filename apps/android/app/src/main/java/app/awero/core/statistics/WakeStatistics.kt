package app.awero.core.statistics

data class WakeStatistics(
    val planned:Int=0,
    val completed:Int=0,
    val snoozes:Int=0,
    val fallback:Int=0,
    val emergencyStops:Int=0,
    val totalCompletionSeconds:Long=0
) {
    val successfulWakeRate:Double get() = if (planned == 0) 0.0 else completed.toDouble()/planned.toDouble()
    val averageCompletionSeconds:Double get() = if (completed == 0) 0.0 else totalCompletionSeconds.toDouble()/completed.toDouble()
}