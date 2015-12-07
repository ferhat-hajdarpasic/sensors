package com.whitespider.impact.ble.sensortag;

import com.whitespider.impact.util.Point3D;

/**
 * Created by ferhat on 18/11/2015.
 */
public class Compass {
    private Point3D reading;
    public Compass() {
    }
    public Compass(Point3D p) {
        reading = p;
    }
    public Point3D getReading() {
        return reading;
    }
    public void setReading(Point3D reading) {
        this.reading = reading;
    }
}
