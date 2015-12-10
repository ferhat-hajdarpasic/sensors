package com.whitespider.impact.ble.sensortag;

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
                //mSensorTagIoProfile.LED(impactLevel);
                fixForDemo(impactLevel);
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

    public void correctCode(byte impactLevel) {
        mSensorTagIoProfile.LED(impactLevel);
    }

    public void fixForDemo(byte impactLevel) {
        if(impactLevel >= 4) {
            allLED(true);
        }
    }

    public void allLED(boolean on) {
        byte temp = on ? (byte) 7 : (byte) 0;
        mSensorTagIoProfile.writeDataValue(new byte[]{temp});
    }
}
