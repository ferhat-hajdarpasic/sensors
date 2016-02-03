package com.whitespider.impact.ble.sensortag;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class HeadGearSectionsPagerAdapter extends FragmentPagerAdapter {
    private Fragment[] fragments = new Fragment[] {
            new DebugFragment(),
            new HistoryItemFragment(),
            new LiveDataFragment(),
            new ConcussionEventFragment(),
            new HistoryItemFragment()
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
        }
        return null;
    }
}
