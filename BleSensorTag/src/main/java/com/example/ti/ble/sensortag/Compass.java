package com.example.ti.ble.sensortag;

import com.example.ti.util.Point3D;

import java.util.Date;

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