package app.awero.core.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepsMission(context: Context, private val difficulty: Difficulty) : Mission {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var startSteps: Float? = null
    val targetSteps: Int = when (difficulty) {
        Difficulty.EASY -> 15
        Difficulty.MEDIUM -> 30
        Difficulty.HARD -> 60
    }

    override fun start() {
        // Sensor listener is attached by the platform wake-session controller.
    }

    override fun validate(): Boolean {
        return false
    }

    override fun retry() {}
}
