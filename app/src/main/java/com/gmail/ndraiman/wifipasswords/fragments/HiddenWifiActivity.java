package com.gmail.ndraiman.wifipasswords.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.CustomItemTouchHelper;
import com.gmail.ndraiman.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;


public class HiddenWifiActivity extends AppCompatActivity implements ItemDragListener {

    private static final String TAG = "HiddenWifiActivity";
    private static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";
    private ArrayList<WifiEntry> mListWifi;

    private Toolbar mToolbar;

    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelper.Callback mTouchHelperCallback;




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
        

        mListWifi = new ArrayList<>();

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

        //TODO show dialog to indicate deleting items will return them to main list
        //TODO add way to delete entries
        
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
        mTouchHelperCallback = new CustomItemTouchHelper(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        //Setup Context Action Mode
//        setupActionModeCallback();

        //Setup OnItemTouchListener
        mRecyclerTouchListener = new RecyclerTouchListener(this, mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onClick " + position);
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onLongClick " + position);

                //Invoking Context Action Mode
//                mAdapter.toggleSelection(position);
//                mRecyclerView.scrollToPosition(position);
//
//                if (mActionMode != null || mIsSortModeOn) {
//                    return;
//                }
//                mActionMode = ((AppCompatActivity) this).startSupportActionMode(mActionModeCallback);
            }
        });

        mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);
    }
}
