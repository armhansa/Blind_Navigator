package com.armhansa.app.blindnavigator.tool;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;

public class Accelerometer {

    private static Accelerometer accelInstance;

    SensorManager sensorManager;
    Sensor sensor;

    ArrayList<SensorEventListener> listeners;

    public static Accelerometer getInstance() {
        if(accelInstance == null) {
            accelInstance = new Accelerometer();
        }
        return accelInstance;
    }

    private Accelerometer() {
        listeners = new ArrayList<>();
    }

    public void setContext(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void addListener(SensorEventListener listener) {
        listeners.add(listener);
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void deleteListener(SensorEventListener listener) {
        listeners.remove(listener);
    }

    public void onResume() {
        for(SensorEventListener i: listeners) {
            sensorManager.registerListener(i, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void onStop() {
        for(SensorEventListener i: listeners) {
            sensorManager.unregisterListener(i);
        }
    }




}
