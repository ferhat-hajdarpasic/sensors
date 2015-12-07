package com.whitespider.impact.ble.sensortag;

import com.whitespider.impact.util.Point3D;

import java.util.Date;

/**
 * Created by ferhat on 22/11/2015.
 */
public class Motion {
    public Motion() {

    }

    public Motion(Date date, Point3D accelerometer, Point3D gyroscope, Point3D compass) {
        this.accelerometer = new Accelerometer(accelerometer);
        this.gyroscope = new Gyroscope(gyroscope);
        this.compass = new Compass(compass);
        this.timeOfReading = date;
    }

    public Accelerometer getAccelerometer() {
        return accelerometer;
    }

    public void setAccelerometer(Accelerometer accelerometer) {
        this.accelerometer = accelerometer;
    }

    public Gyroscope getGyroscope() {
        return gyroscope;
    }

    public void setGyroscope(Gyroscope gyroscope) {
        this.gyroscope = gyroscope;
    }

    public Compass getCompass() {
        return compass;
    }

    public void setCompass(Compass compass) {
        this.compass = compass;
    }

    public Date getTimeOfReading() {
        return timeOfReading;
    }

    public void setTimeOfReading(Date timeOfReading) {
        this.timeOfReading = timeOfReading;
    }

    private Accelerometer accelerometer;
    private Gyroscope gyroscope;
    private Compass compass;
    private Date timeOfReading;
}
