package com.example.ti.ble.sensortag;


import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.example.ti.ble.common.GenericBluetoothProfile;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class HeadGearActivity extends DeviceActivity {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ArrayList<BluetoothGattService> mServiceList;
    private SampleChart sampleChart;
    private float keep_track_level;
    private float yellow_minor_shock;
    private float orange_medium_shock;
    private float red_important_shock;
    private float purple_severe_shock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_gear);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        getResources().getString(R.string.keep_track_level);
        getResources().getString(R.string.yellow_minor_shock);
        getResources().getString(R.string.orange_medium_shock);
        getResources().getString(R.string.red_important_shock);
        getResources().getString(R.string.purple_severe_shock);

        keep_track_level = DeviceActivity.getNumber(this, prefs, R.string.keep_track_level, 10);
        yellow_minor_shock = DeviceActivity.getNumber(this, prefs, R.string.yellow_minor_shock, 10);
        orange_medium_shock = DeviceActivity.getNumber(this, prefs, R.string.orange_medium_shock, 20);
        red_important_shock = DeviceActivity.getNumber(this, prefs, R.string.red_important_shock, 30);
        purple_severe_shock = DeviceActivity.getNumber(this, prefs, R.string.purple_severe_shock, 40);
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

    private ArrayDeque<Double> fifo = new ArrayDeque<Double>();
    @Override
    protected void observeAcceleration(MotionSensor p) {
        final Motion reading = p.getReading();
        final Double totalAcceleration = ConcussionDetector.getTotalAcceleration(reading);
        final Double direction = ConcussionDetector.getDirection(reading);
        final Double angularAcceleration = ConcussionDetector.getAngularAcceleration(reading);
        fifo.add(totalAcceleration);
        if(fifo.size() > 10) {
            fifo.remove();
        }
        if (totalAcceleration >= keep_track_level) {
            recordShock(totalAcceleration, direction, angularAcceleration);
        }
        byte ledState = 0x00;
        if (totalAcceleration >= yellow_minor_shock) {
            sampleChart.addSamples(fifo.toArray(new Double[0]), 0);
            ledState = (byte)0x01;
        } else if(totalAcceleration >= orange_medium_shock) {
            sampleChart.addSamples(fifo.toArray(new Double[0]), 0);
            ledState = (byte)0x02;
        } else if(totalAcceleration >= red_important_shock) {
            sampleChart.addSamples(fifo.toArray(new Double[0]), 0);
            ledState = (byte)0x03;
        } else if(totalAcceleration >= purple_severe_shock) {
            sampleChart.addSamples(fifo.toArray(new Double[0]), 0);
            ledState = (byte)0x04;
        }
        if(ledState != 0x00) {
            headGearLED(ledState);
        }
    }

    public void headGearLED(byte b) {
        if(mSensorTagIoProfile != null) {
            mSensorTagIoProfile.LED(b);
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "HABA", Toast.LENGTH_SHORT).show();
                        if (mSensorTagIoProfile != null) {
                            mSensorTagIoProfile.LED((byte) 0);
                        }
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 2000);
    }

    private void recordShock(Double totalAcceleration, Double direction, Double angularAcceleration) {

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

    public void doChart(View rootView) {
        LineChart chart = (LineChart) rootView.findViewById(R.id.line_chart);
        sampleChart = new SampleChart(chart, rootView.getContext());
        sampleChart.onCreate();
    }
    public static class PlaceholderFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_head_gear, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

            final HeadGearActivity mDeviceActivity = (HeadGearActivity)getActivity();
            mDeviceActivity.doChart(rootView);
            mDeviceActivity.onViewInflated(rootView);

            return rootView;
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }
}
