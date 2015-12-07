package com.whitespider.impact.ble.sensortag;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

public class DebugFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    HeadGearActivity headGearActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        headGearActivity = (HeadGearActivity)getActivity();

        Switch redSwitch = (Switch) view.findViewById(R.id.redLed);
        Switch greenSwitch = (Switch) view.findViewById(R.id.greenLed);
        Switch buzzerSwitch = (Switch) view.findViewById(R.id.buzzer);

        redSwitch.setOnCheckedChangeListener(this);
        greenSwitch.setOnCheckedChangeListener(this);
        buzzerSwitch.setOnCheckedChangeListener(this);

        return view;

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        final SensorTagIoProfile sensorTagIoProfile = headGearActivity.getSensorTagIoProfile();
        Switch btnSwitch = (Switch)compoundButton;
        switch(btnSwitch.getId()) {
            case R.id.redLed:
                sensorTagIoProfile.red(isChecked);
                break;
            case R.id.greenLed:
                sensorTagIoProfile.green(isChecked);
                break;
            case R.id.buzzer:
                sensorTagIoProfile.buzzer(isChecked);
                break;
            default:
                break;
        }

    }
}
