package com.gmail.ndraiman.wifipasswords.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;


public class HiddenWifiFragment extends Fragment {

    private WifiListAdapter mAdapter;
    private ArrayList<WifiEntry> mListWifi = new ArrayList<>();


    public static HiddenWifiFragment newInstance() {
        HiddenWifiFragment fragment = new HiddenWifiFragment();
        Bundle args = new Bundle();
        //put any extra arguments that you may want to supply to this fragment
        fragment.setArguments(args);
        return fragment;
    }


    public HiddenWifiFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_hidden_wifi, container, false);
//        setHasOptionsMenu(true);



        return layout;
    }
}
