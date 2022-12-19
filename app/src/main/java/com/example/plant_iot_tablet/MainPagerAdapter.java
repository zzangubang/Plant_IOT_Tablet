package com.example.plant_iot_tablet;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class MainPagerAdapter extends FragmentPagerAdapter {
    private ArrayList<Fragment> mData;
    public MainPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);

        mData = new ArrayList<>();
        mData.add(new MainText());
        mData.add(new MainGraph());

    }

    @NonNull
    @Override
    public CharSequence getPageTitle(int position) {
        if(position == 0)
            return "TEXT";
        else if(position == 1)
            return "GRAPH";
        return null;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) { return mData.get(position); }

    @Override
    public int getCount() { return mData.size(); }

    @Override
    public int getItemPosition(@NonNull Object object) { return POSITION_NONE; }
}
