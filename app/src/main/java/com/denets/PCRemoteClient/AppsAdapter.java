package com.denets.PCRemoteClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class AppsAdapter extends FragmentStateAdapter {
    public List<Fragment> fragments;

    public AppsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragments = this.getFragments();
    }

    public List<Fragment> getFragments()  {
        Fragment f1 = new Favorites();
        Fragment f2 = new Desktop();

        List<Fragment> list = new ArrayList<>();
        list.add(f1);
        list.add(f2);

        return list;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return this.fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return this.fragments.size();
    }
}
