package com.gmail.ndraiman.wifipasswords.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

public class MainActivity extends AppCompatActivity implements WifiListLoadedListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries";
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry";
    private Toolbar toolbar;
    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private ArrayList<WifiEntry> mListWifi = new ArrayList<>();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressBar mProgressBar;
    private CoordinatorLayout mRoot;
    private Snackbar mSnackbar;

    public static TextView textNoRoot;


    //is this App being started for the very first time?
    private boolean mFromSavedInstanceState;

    //TODO Implement "Hidden" table.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_coordinator);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        textNoRoot = (TextView) findViewById(R.id.text_no_root);
        mRoot = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        //backward compatible MaterialProgressBar - https://github.com/DreaminginCodeZH/MaterialProgressBar
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        IndeterminateProgressDrawable progressDrawable = new IndeterminateProgressDrawable(this);
        progressDrawable.setTint(ContextCompat.getColor(this, R.color.colorPrimary)); //Change Color
        mProgressBar.setIndeterminateDrawable(progressDrawable);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeWifiList);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary, R.color.colorPrimaryDark);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setDistanceToTriggerSync(500);

        //Setup RecyclerView & Adapter
        mRecyclerView = (RecyclerView) findViewById(R.id.wifiList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new WifiListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);


        //Setup RecyclerTouchListener
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Toast.makeText(MainActivity.this, "onClick " + position, Toast.LENGTH_SHORT).show();  //placeholder
            }

            @Override
            public void onLongClick(View view, int position) {
                L.m("ReyclerView - onLongClick " + position);

                WifiEntry entry = mListWifi.get(position);
                String textToCopy = "Wifi Name: " + entry.getTitle() + "\n"
                        + "Password: " + entry.getPassword();
                String snackbarMessage = getResources().getString(R.string.text_wifi_copy);

                copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, snackbarMessage);
            }
        }));


        if (savedInstanceState != null) {
            L.m("extracting mListWifi from Parcelable");
            //if this fragment starts after a rotation or configuration change, load the existing Wifi list from a parcelable
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);

        } else {
            //if this fragment starts for the first time, load the list of wifi from a database
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
            //if the database is empty, trigger an AsyncTask to get wifi list from the wpa_supplicant
            if (mListWifi.isEmpty()) {

                mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
                L.m("executing task from onCreate");
                new TaskLoadWifiEntries(this).execute();
            }
        }

        mAdapter.setWifiList(mListWifi);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES, mListWifi);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
//        L.T(this, "Destroying Database");
//        MyApplication.getWritableDatabase().deleteAll(false);
        super.onDestroy();
    }

    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi) {

        if (mSwipeRefreshLayout.isRefreshing()) {
            mSwipeRefreshLayout.setRefreshing(false);
        }

        //Hide Progress Bar
        if(mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        L.m("onWifiListLoaded");
        mListWifi = listWifi;
        mAdapter.setWifiList(listWifi);
    }

    //Swipe to Refresh
    @Override
    public void onRefresh() {
        L.t(this, "onRefresh");
        //load the whole feed again on refresh.
        new TaskLoadWifiEntries(this).execute();
    }

    //Copy to Clipboard Method
    private void copyToClipboard(String copiedLabel, String copiedText, String snackbarMessage) {

        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(copiedLabel, copiedText);
        clipboardManager.setPrimaryClip(clipData);

        makeSnackbar(snackbarMessage);
        L.m("copyToClipboard:\n" + clipData.toString());
    }

    //Custom Snackbar
    private void makeSnackbar(String message) {

        Snackbar mSnackbar = Snackbar.make(mRoot, message, Snackbar.LENGTH_SHORT);
        View snackbarView = mSnackbar.getView();

        //snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

        TextView snackbarText = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        snackbarText.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        snackbarText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        mSnackbar.show();

    }
}
