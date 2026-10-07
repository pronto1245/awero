package app.awero.core.missions

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepsMission(private val context: Context) : Mission, SensorEventListener {
    private var target = 30
    var steps: Int = 0
        private set
    var available: Boolean = false
        private set
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    override fun start() {
        target = 30
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        available = sensor != null
        if (sensor != null) manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun start(target:Int) {
        this.target = target.coerceAtLeast(1)
        start()
    }

    override fun onSensorChanged(event: SensorEvent) {
        steps = event.values.firstOrNull()?.toInt() ?: steps
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun validate(): Boolean = available && steps >= target
    override fun retry() { steps = 0 }
    fun stop() { manager.unregisterListener(this) }
}
