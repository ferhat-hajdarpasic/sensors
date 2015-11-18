package com.example.ti.ble.sensortag;

import com.example.ti.util.Point3D;

import java.util.Date;

/**
 * Created by ferhat on 18/11/2015.
 */
public class AccelerometerReading {
    private Point3D reading;
    private Date timeOfReading;
    public AccelerometerReading() {
    }
    public AccelerometerReading(Date d, Point3D p) {
        reading = p;
        timeOfReading = d;
    }

    public Point3D getReading() {
        return reading;
    }

    public void setReading(Point3D reading) {
        this.reading = reading;
    }

    public Date getTimeOfReading() {
        return timeOfReading;
    }

    public void setTimeOfReading(Date timeOfReading) {
        this.timeOfReading = timeOfReading;
    }
}
