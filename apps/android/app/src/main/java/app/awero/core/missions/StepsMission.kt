package app.awero.core.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepsMission(private val context: Context) : SensorEventListener {
    private var progress = StepCounterProgress(30)
    var steps: Int = 0
        private set
    var available: Boolean = false
        private set

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun start() {
        progress = StepCounterProgress(30)
        steps = 0
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        available = sensor != null
        if (sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun start(target: Int) {
        progress = StepCounterProgress(target)
        steps = 0
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        available = sensor != null
        if (sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values.firstOrNull()?.toInt() ?: return
        progress.update(total)
        steps = progress.steps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    fun validate(): Boolean = available && progress.completed
    fun stop() { manager.unregisterListener(this) }
}
