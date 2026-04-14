package com.example.gamefromzero

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorController(context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var tiltX = 0f
    
    private val sensitivity = 2.0f
    private val maxTilt = 10.0f
    
    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }
    
    fun unregister() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            var rawTilt = -event.values[0]
            
            if (rawTilt > maxTilt) rawTilt = maxTilt
            if (rawTilt < -maxTilt) rawTilt = -maxTilt
            
            tiltX = rawTilt / maxTilt
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }
    
    fun getTiltX(): Float {
        return tiltX * sensitivity
    }
    
    fun hasAccelerometer(): Boolean {
        return accelerometer != null
    }
}
