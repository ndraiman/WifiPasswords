package com.gmail.ndraiman.wifipasswords.fragments;

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
import com.gmail.ndraiman.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;


public class HiddenWifiActivity extends AppCompatActivity implements ItemDragListener {

    private static final String TAG = "HiddenWifiActivity";

    private static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";

    private ArrayList<WifiEntry> mListWifi;

    private Toolbar mToolbar;
    private CoordinatorLayout mRoot;

    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelper.Callback mTouchHelperCallback;


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

        setResult(RESULT_CANCELED);

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


        mAdapter.setWifiList(mListWifi);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");

        outState.putParcelableArrayList(STATE_HIDDEN_ENTRIES, mListWifi);
    }

    //Sort Mode Method - sort via drag
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onStartDrag");
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(this, this);
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

                setResult(RESULT_OK);

                WifiEntry deleted = mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
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
