package com.example.ti.ble.sensortag;


import android.app.Activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothGattService;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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

import com.example.ti.ble.common.GenericBluetoothProfile;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

public class HeadGearActivity extends DeviceActivity {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private ArrayList<BluetoothGattService> mServiceList;
    private SampleChart sampleChart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_head_gear);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
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
        fifo.add(totalAcceleration);
        if(fifo.size() > 10) {
            fifo.remove();
        }
        if (totalAcceleration >= 0.5) {
            sampleChart.addSamples(fifo.toArray(new Double[0]), 0);
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
