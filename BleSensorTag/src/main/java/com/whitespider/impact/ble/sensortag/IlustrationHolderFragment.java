package com.whitespider.impact.ble.sensortag;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

public class IlustrationHolderFragment extends AndroidFragmentApplication {

    HeadGearActivity headGearActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        headGearActivity = (HeadGearActivity)getActivity();
        View view = inflater.inflate(R.layout.fragment_debug, container, false);

        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        return initializeForView(new MyGdxGame(), cfg);
    }
}
