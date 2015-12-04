package com.example.ti.ble.sensortag;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LiveDataFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_head_gear, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.section_label);
        textView.setText(getString(R.string.live_data_fragment));

        final HeadGearActivity mDeviceActivity = (HeadGearActivity)getActivity();
        mDeviceActivity.createStreamingChart(rootView);
        mDeviceActivity.onViewInflated(rootView);

        return rootView;
    }
}

