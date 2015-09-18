package com.gmail.ndraiman.wifipasswords.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.L;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.extras.TaskLoadWifiEntries;
import com.gmail.ndraiman.wifipasswords.extras.WifiListLoadedListener;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;

import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;


public class MainWifiFragment extends Fragment implements WifiListLoadedListener {

    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries";
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry";
    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private ArrayList<WifiEntry> mListWifi = new ArrayList<>();
    private ProgressBar mProgressBar;

    public static TextView textNoRoot;



    public static MainWifiFragment newInstance() {
        MainWifiFragment fragment = new MainWifiFragment();
        Bundle args = new Bundle();
        //put any extra arguments that you may want to supply to this fragment
        fragment.setArguments(args);
        return fragment;
    }


    public MainWifiFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_main_wifi, container, false);
        setHasOptionsMenu(true);

        textNoRoot = (TextView) layout.findViewById(R.id.text_no_root);

        //backward compatible MaterialProgressBar - https://github.com/DreaminginCodeZH/MaterialProgressBar
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
        IndeterminateProgressDrawable progressDrawable = new IndeterminateProgressDrawable(getActivity());
        progressDrawable.setTint(ContextCompat.getColor(getActivity(), R.color.colorPrimary)); //Change Color
        mProgressBar.setIndeterminateDrawable(progressDrawable);

        //Setup RecyclerView & Adapter
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.wifiList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new WifiListAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);


        //Setup RecyclerTouchListener
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Toast.makeText(getActivity(), "onClick " + position, Toast.LENGTH_SHORT).show();  //placeholder
            }

            @Override
            public void onLongClick(View view, int position) {
                L.m("RecyclerView - onLongClick " + position);

                WifiEntry entry = mListWifi.get(position);
                String textToCopy = "Wifi Name: " + entry.getTitle() + "\n"
                        + "Password: " + entry.getPassword();
                String snackbarMessage = getResources().getString(R.string.text_wifi_copy);

                copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, snackbarMessage);
            }
        }));

        //Determine if Activity runs for first time
        if (savedInstanceState != null) {
            L.m("extracting mListWifi from Parcelable");
            //if starts after a rotation or configuration change, load the existing Wifi list from a parcelable
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);

        } else {
            //if starts for the first time, load the list of wifi from a database
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
            //if the database is empty, trigger an AsyncTask to get wifi list from the wpa_supplicant
            if (mListWifi.isEmpty()) {

                loadFromFile();
                L.m("executing task from onCreate");
            }
        }

        mAdapter.setWifiList(mListWifi);

        return layout;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES, mListWifi);
    }


    //WifiListLoadedListener method - called from TaskLoadWifiEntries
    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi) {

        //Hide Progress Bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        L.m("onWifiListLoaded");
        mListWifi = listWifi;
        mAdapter.setWifiList(listWifi);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wifi_list_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_refresh_from_file) {
            loadFromFile();
        }
        return true;
    }


    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/

    //Copy wpa_supplicant and extract data from it via AsyncTask
    private void loadFromFile() {

        mAdapter.setWifiList(new ArrayList<WifiEntry>());
        mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
        L.m("loadFromFile");
        MainActivity.makeSnackbar("Loading Data From File");
        new TaskLoadWifiEntries(this).execute();

    }

    //Copy to Clipboard Method
    private void copyToClipboard(String copiedLabel, String copiedText, String snackbarMessage) {

        ClipboardManager clipboardManager =
                (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(copiedLabel, copiedText);
        clipboardManager.setPrimaryClip(clipData);

        MainActivity.makeSnackbar(snackbarMessage);
        L.m("copyToClipboard:\n" + clipData.toString());
    }
}
