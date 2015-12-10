package com.whitespider.impact.ble.sensortag;

import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.whitespider.impact.ble.common.GenericBluetoothProfile;
import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;

public class HeadGearActivity extends DeviceActivity {
    private HeadGearSectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ArrayList<BluetoothGattService> mServiceList;
    private SampleChart liveStreamingChart;
    private SampleChart concussionChart;
    private ConcussionDetector concussionDetector;
    private ConcussionLedInidicator concussionLedInidicator;
    private TextView accelerationDetails;
    private TextView xyzAccelerationDetails;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_head_gear);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSectionsPagerAdapter = new HeadGearSectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        concussionDetector = new ConcussionDetector(this, prefs);
    }

    @Override
    public void setSensorTagIoProfile(SensorTagIoProfile sensorTagIoProfile) {
        super.setSensorTagIoProfile(sensorTagIoProfile);
        concussionLedInidicator = new ConcussionLedInidicator(this, sensorTagIoProfile);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_head_gear, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void observeAcceleration(MotionSensor p) {
        liveStreamingChart.observeAcceleration(p);
        byte concussionSeverity = concussionDetector.concussionSeverity(p);
        if(concussionSeverity != 0x00) {
            if(concussionChart != null) {
                concussionChart.startRecording(concussionDetector.getSamples());
                concussionChart.indicateSeverity(concussionSeverity);
                concussionLedInidicator.headGearLED(concussionSeverity);
                final Motion reading = p.getReading();
                this.accelerationDetails.setText(
                        String.format("Total %2.2fG.", ConcussionDetector.getTotalAcceleration(reading)));
                this.xyzAccelerationDetails.setText(String.format("X = %2.2fdG, Y = %2.2fG, Z = %2.2fG.",
                                            reading.getAccelerometer().getReading().x,
                                            reading.getAccelerometer().getReading().y,
                                            reading.getAccelerometer().getReading().z));
            }
        }
        if(concussionChart != null) {
            if (concussionChart.isRecording()) {
                concussionChart.observeAcceleration(p);
            }
        }
    }

    protected void setBusy(boolean b) {
        //mDeviceView.setBusy(b);
    }

    public void enableService(final GenericBluetoothProfile p) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                p.enableService();
            }
        });
    }

    public void createStreamingChart(View rootView) {
        LineChart chart = (LineChart) rootView.findViewById(R.id.line_chart);
        liveStreamingChart = new SampleChart(chart, this);
        liveStreamingChart.onCreate();
    }
    public void createConcussionChart(View rootView) {
        LineChart chart = (LineChart) rootView.findViewById(R.id.line_chart);
        accelerationDetails = (TextView) rootView.findViewById(R.id.textViewAcceleration);
        xyzAccelerationDetails = (TextView) rootView.findViewById(R.id.xyzAcceleration);
        concussionChart = new SampleChart(chart, this);
        concussionChart.onCreate();
    }
}
