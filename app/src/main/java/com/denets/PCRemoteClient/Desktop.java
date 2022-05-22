package com.denets.PCRemoteClient;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Desktop extends Fragment {
    public Desktop() {}

    @Override
    public void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_desktop, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity.mainActivity.setDesktop();
    }
}