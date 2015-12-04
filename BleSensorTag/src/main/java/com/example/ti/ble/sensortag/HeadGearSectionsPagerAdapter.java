package com.example.ti.ble.sensortag;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class HeadGearSectionsPagerAdapter extends FragmentPagerAdapter {
    private Fragment[] fragments = new Fragment[] {
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
        }
        return null;
    }
}
