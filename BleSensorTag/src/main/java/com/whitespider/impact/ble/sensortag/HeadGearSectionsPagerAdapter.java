package com.whitespider.impact.ble.sensortag;

import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.Fragment;

public class HeadGearSectionsPagerAdapter extends FragmentPagerAdapter {
    private Fragment[] fragments = new Fragment[] {
            new DebugFragment(),
            new LiveDataFragment(),
            new ConcussionEventFragment(),
            new ImpactIllustration()
    };

    public HeadGearSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public android.app.Fragment getItem(int position) {
        return null;
    }

    @Override
    public int getCount() {
        return fragments.length;
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
