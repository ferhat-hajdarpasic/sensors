package com.example.ti.ble.sensortag;

import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ferhat on 4/12/2015.
 */
public class ConcussionLedInidicator {
    private HeadGearActivity headGearActivity;
    private SensorTagIoProfile mSensorTagIoProfile;
    private byte currentImpactLevel = 0;
    private Timer currentTimer;
    public ConcussionLedInidicator(HeadGearActivity headGearActivity, SensorTagIoProfile mSensorTagIoProfile) {
        this.headGearActivity = headGearActivity;
        this.mSensorTagIoProfile = mSensorTagIoProfile;
    }
    public void headGearLED(final byte impactLevel) {
        if(impactLevel > currentImpactLevel) {
            if(mSensorTagIoProfile != null) {
                mSensorTagIoProfile.LED(impactLevel);
            }
            if(currentTimer != null) {
                currentTimer.cancel();
                currentTimer = null; //Not in the game any more.
            }
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    headGearActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            currentImpactLevel = 0;
                            Toast.makeText(headGearActivity.getApplicationContext(), "Concussion severity=" + impactLevel, Toast.LENGTH_SHORT).show();
                            if (mSensorTagIoProfile != null) {
                                mSensorTagIoProfile.LED((byte) currentImpactLevel);
                            }
                        }
                    });
                }
            };
            Timer timer = new Timer();
            timer.schedule(timerTask, 2000);
            currentTimer = timer; //Remember the new timer.
        }
    }
}
