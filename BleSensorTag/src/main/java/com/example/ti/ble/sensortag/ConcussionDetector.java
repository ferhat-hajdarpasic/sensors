package com.example.ti.ble.sensortag;

import com.example.ti.util.Point3D;

/**
 * Created by ferhat on 18/11/2015.
 */
public class ConcussionDetector {
    static double getTotalAcceleration(Motion motion) {
        Point3D p = motion.getAccelerometer().getReading();
        return Math.sqrt(p.x*p.x + p.y*p.y+ p.z*p.z);
    }

    public static Double getDirection(Motion reading) {
        return null;
    }

    public static Double getAngularAcceleration(Motion reading) {
        return null;
    }
}
