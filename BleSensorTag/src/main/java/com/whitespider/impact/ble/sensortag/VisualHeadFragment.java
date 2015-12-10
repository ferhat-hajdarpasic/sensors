package com.whitespider.impact.ble.sensortag;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;

/**
 * Created by ferhat on 8/12/2015.
 */
public class VisualHeadFragment extends AndroidFragmentApplication implements AndroidFragmentApplication.Callbacks {
    // 5. Add the initializeForView() code in the Fragment's onCreateView method.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return initializeForView(new VisualHeadRenderer());
    }
}
