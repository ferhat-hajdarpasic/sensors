package com.whitespider.impact.ble.sensortag;

import android.app.Activity;
import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayDeque;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ferhat on 3/12/2015.
 */
public class SampleChart implements OnChartValueSelectedListener {
    private final Activity activity;
    private LineChart mChart;
    private ArrayDeque<Double> fifo = new ArrayDeque<Double>();
    private boolean isRecording = false;

    public SampleChart(LineChart mChart, Activity activity) {
        this.mChart = mChart;
        this.activity = activity;
    }
    public void addSamples(Double[] fifo, int dataSetIndex) {

        LineData data = mChart.getData();

        if (data != null) {
            data.removeDataSet(dataSetIndex);
            LineDataSet set = createSet();
            data.addDataSet(set);
            for (int i = 0; i < fifo.length; i++) {
                data.addXValue("" + i);
                data.addEntry(new Entry(fifo[i].floatValue(), i + 1), dataSetIndex);
            }
            mChart.notifyDataSetChanged();
            mChart.setVisibleXRangeMaximum(fifo.length + 2);
           // mChart.moveViewToX(data.getXValCount() - 121);
        }
        mChart.invalidate();
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Acceleration");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleSize(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }
    public void onCreate() {
        mChart.setOnChartValueSelectedListener(this);

        // no description text
        mChart.setDescription("");
        mChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.LTGRAY);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);

        //Typeface tf = Typeface.createFromAsset(context.getAssets(), "OpenSans-Regular.ttf");

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        // l.setPosition(LegendPosition.LEFT_OF_CHART);
        l.setForm(Legend.LegendForm.LINE);
        //l.setTypeface(tf);
        l.setTextColor(Color.WHITE);

        XAxis xl = mChart.getXAxis();
        //xl.setTypeface(tf);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setSpaceBetweenLabels(5);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        //leftAxis.setTypeface(tf);
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaxValue(20f);
        leftAxis.setAxisMinValue(-20f);
        leftAxis.setStartAtZero(false);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);    }

    @Override
    public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    public byte observeAcceleration(MotionSensor sensor) {
        final Motion reading = sensor.getReading();
        final Double totalAcceleration = ConcussionDetector.getTotalAcceleration(reading);
        final Double direction = ConcussionDetector.getDirection(reading);
        final Double angularAcceleration = ConcussionDetector.getAngularAcceleration(reading);
        fifo.add(totalAcceleration);
        if(isRecording) {
            addSamples(fifo.toArray(new Double[0]), 0);
        } else {
            if (fifo.size() > 10) {
                fifo.remove();
            }
            addSamples(fifo.toArray(new Double[0]), 0);
        }
        return 0;
    }

    public void indicateSeverity(byte concussionSeverity) {
        switch(concussionSeverity) {
            case 1:
                mChart.setBackgroundColor(Color.parseColor("#FFFF33"));
                break;
            case 2:
                mChart.setBackgroundColor(Color.parseColor("#FF6633")); //Orange
                break;
            case 3:
                mChart.setBackgroundColor(Color.parseColor("#FF0000"));
                break;
            case 4:
                mChart.setBackgroundColor(Color.parseColor("#9900CC"));
                break;
            default:
                mChart.setBackgroundColor(Color.LTGRAY);
                break;
        }
    }

    public void startRecording(Double[] samples) {
        if(isRecording) {
            return;
        }
        isRecording = true;
        fifo.clear();
        for(int i = 0; i < samples.length; i++) {
            fifo.add(samples[i]);
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isRecording = false;
                    }
                });
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 500);
    }

    public boolean isRecording() {
        return isRecording;
    }
}
