package com.gmail.ndraiman.wifipasswords.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;


public class HiddenWifiActivity extends AppCompatActivity {

    private static final String TAG = "HiddenWifiActivity";

    private static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";
    private static final String STATE_RESTORED_ENTRIES = "state_restored_entries";

    private ArrayList<WifiEntry> mListWifi;

    private Toolbar mToolbar;
    private CoordinatorLayout mRoot;

    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelper.Callback mTouchHelperCallback;

    private ArrayList<WifiEntry> mEntriesDeleted;

    //TODO show dialog to indicate deleting items will return them to main list
    //TODO dialog will show only on user first time seeing this activity

    //TODO add way to permanently delete entries???

    public HiddenWifiActivity() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //Set Activity Transition - Lollipop+
        if(Build.VERSION.SDK_INT >= 21) {

            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition slideFromRight = transitionInflater.inflateTransition(R.transition.activity_slide_right);
            Transition slideFromLeft = transitionInflater.inflateTransition(R.transition.activity_slide_left);

            getWindow().setEnterTransition(slideFromLeft);
            getWindow().setExitTransition(slideFromRight);

        }
        setContentView(R.layout.activity_hidden_wifi);

        setResult(RESULT_CANCELED, null);

        mListWifi = new ArrayList<>();

        mRoot = (CoordinatorLayout) findViewById(R.id.activity_hidden_wifi_container);
        mRecyclerView = (RecyclerView) findViewById(R.id.hidden_wifi_list_recycler);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

        ActionBar sBar;
        if((sBar = getSupportActionBar()) != null) {
            sBar.setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();


        if(savedInstanceState != null) {
            Log.d(TAG, "extracting hidden list from parcelable");
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_HIDDEN_ENTRIES);

        } else {
            Log.d(TAG, "getting hidden list from database");
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(true);
            MyApplication.closeDatabase();
        }

        mEntriesDeleted = new ArrayList<>();

        mAdapter.setWifiList(mListWifi);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");

        outState.putParcelableArrayList(STATE_HIDDEN_ENTRIES, mListWifi);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        //Setup ItemTouchHelper
//        mTouchHelperCallback = new MyTouchHelperCallback(mAdapter);

        mTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                Log.d(TAG, "onSwiped");

                WifiEntry deleted = mAdapter.onItemDismiss(viewHolder.getAdapterPosition());

                mEntriesDeleted.add(deleted);
                Intent data = getIntent();
                data.putParcelableArrayListExtra(STATE_RESTORED_ENTRIES, mEntriesDeleted);
                setResult(RESULT_OK, data);


                ArrayList<WifiEntry> deletions = new ArrayList<>();
                deletions.add(deleted);
                MyApplication.getWritableDatabase().deleteWifiEntries(deletions, true);
                MyApplication.closeDatabase();

                Snackbar.make(mRoot,
                        deleted.getTitle() + " " + getString(R.string.snackbar_wifi_restore),
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        };

        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

    }

}
