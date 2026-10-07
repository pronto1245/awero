package app.awero.core.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepsMission(private val context: Context) : Mission, SensorEventListener {
    private var target = 30
    private var baseline = 0
    var steps: Int = 0
        private set
    var available: Boolean = false
        private set

    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun start() {
        target = 30
        steps = 0
        baseline = 0
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        available = sensor != null
        if (sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun start(target: Int) {
        this.target = target.coerceAtLeast(1)
        steps = 0
        baseline = 0
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        available = sensor != null
        if (sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val total = event.values.firstOrNull()?.toInt() ?: return
        if (baseline == 0) baseline = total
        steps = (total - baseline).coerceAtLeast(0)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun validate(): Boolean = available && steps >= target
    override fun retry() {
        baseline = 0
        steps = 0
    }
    fun stop() { manager.unregisterListener(this) }
}
