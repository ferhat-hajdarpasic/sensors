package com.whitespider.impact.ble.sensortag;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class HeadGearSectionsPagerAdapter extends FragmentPagerAdapter {
    private Fragment[] fragments = new Fragment[] {
//            new VisualHeadFragment(),
            new DebugFragment(),
            new LiveDataFragment(),
            new ConcussionEventFragment()

    };

    public HeadGearSectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
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
            case 4:
                return "SECTION 4";
        }
        return null;
    }
}
