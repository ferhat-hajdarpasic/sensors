package com.example.ti.ble.sensortag;

import com.example.ti.util.Point3D;

/**
 * Created by ferhat on 18/11/2015.
 */
public class ConcussionDetector {
    static double getTotalAcceleration(AccelerometerReading reading) {
        Point3D p = reading.getReading();
        return Math.sqrt(p.x*p.x + p.y*p.y+ p.z*p.z);
    }
}
