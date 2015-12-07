package com.whitespider.impact.ble.sensortag;

import android.content.SharedPreferences;
import android.util.Log;

import com.whitespider.impact.util.Point3D;

import java.util.ArrayDeque;

/**
 * Created by ferhat on 18/11/2015.
 */
public class ConcussionDetector {
    private static final String TAG = ConcussionDetector.class.getName();

    public ConcussionDetector(HeadGearActivity activity, SharedPreferences prefs) {
        keep_track_level = DeviceActivity.getNumber(activity, prefs, R.string.keep_track_level, 10);
        yellow_minor_shock = DeviceActivity.getNumber(activity, prefs, R.string.yellow_minor_shock, 10);
        orange_medium_shock = DeviceActivity.getNumber(activity, prefs, R.string.orange_medium_shock, 20);
        red_important_shock = DeviceActivity.getNumber(activity, prefs, R.string.red_important_shock, 30);
        purple_severe_shock = DeviceActivity.getNumber(activity, prefs, R.string.purple_severe_shock, 40);
    }

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

    private float keep_track_level;
    private float yellow_minor_shock;
    private float orange_medium_shock;
    private float red_important_shock;
    private float purple_severe_shock;
    private ArrayDeque<Double> samples = new ArrayDeque<Double>();

    public byte concussionSeverity(MotionSensor sensor) {
        final Motion reading = sensor.getReading();
        final Double totalAcceleration = ConcussionDetector.getTotalAcceleration(reading);
        final Double direction = ConcussionDetector.getDirection(reading);
        final Double angularAcceleration = ConcussionDetector.getAngularAcceleration(reading);

        samples.add(totalAcceleration);
        if(samples.size() > 10) {
            samples.remove();
        }

        if (totalAcceleration >= keep_track_level) {
            recordShock(totalAcceleration, direction, angularAcceleration);
        }
        byte concussionSeverity = 0x00;
        if (totalAcceleration >= purple_severe_shock) {
            concussionSeverity = (byte)0x04;
        } else if(totalAcceleration >= red_important_shock) {
            concussionSeverity = (byte)0x03;
        } else if(totalAcceleration >= orange_medium_shock) {
            concussionSeverity = (byte)0x02;
        } else if(totalAcceleration >= yellow_minor_shock) {
            concussionSeverity = (byte)0x01;
        }
        if(concussionSeverity > 0 ) {
            Log.i(TAG, "totalAcceleration=" + totalAcceleration);
            Log.i(TAG, "concussionSeverity=" + concussionSeverity);
        }
        return concussionSeverity;
    }

    private void recordShock(Double totalAcceleration, Double direction, Double angularAcceleration) {

    }

    public Double[] getSamples() {
        return samples.toArray(new Double[0]);
    }
}
