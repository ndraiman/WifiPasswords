package com.gmail.ndraiman.wifipasswords.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;

import java.util.ArrayList;


public class HiddenWifiActivity extends AppCompatActivity {

    private static final String TAG = "HiddenWifiActivity";
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label

    private static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";
    private static final String STATE_RESTORED_ENTRIES = "state_restored_entries";
    private static final String STATE_ACTION_MODE = "state_action_mode";
    private static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";

    private ArrayList<WifiEntry> mListWifi;

    private Toolbar mToolbar;
    private CoordinatorLayout mRoot;

    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    private ItemTouchHelper mItemTouchHelper;
    private ItemTouchHelper.Callback mTouchHelperCallback;

    private ArrayList<WifiEntry> mEntriesDeleted;

    //Context Action Mode
    private ActionMode mActionMode;
    private ArrayList<Integer> mActionModeSelections;
    private ActionMode.Callback mActionModeCallback;
    private boolean isActionModeOn = false;
    private boolean mActionModeRestorePressed = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations

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
        if (Build.VERSION.SDK_INT >= 21) {

            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition slideFromRight = transitionInflater.inflateTransition(R.transition.activity_slide_right);
            Transition slideFromLeft = transitionInflater.inflateTransition(R.transition.activity_slide_left);

            getWindow().setEnterTransition(slideFromLeft);
            getWindow().setExitTransition(slideFromRight);

        }
        setContentView(R.layout.activity_hidden_wifi);

        mListWifi = new ArrayList<>();

        mRoot = (CoordinatorLayout) findViewById(R.id.activity_hidden_wifi_container);
        mRecyclerView = (RecyclerView) findViewById(R.id.hidden_wifi_list_recycler);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

        ActionBar sBar;
        if ((sBar = getSupportActionBar()) != null) {
            sBar.setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();

        setupActionModeCallback();

        if (savedInstanceState != null) {
            Log.d(TAG, "extracting hidden list from parcelable");
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_HIDDEN_ENTRIES);
            isActionModeOn = savedInstanceState.getBoolean(STATE_ACTION_MODE);
            mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);

        } else {
            Log.d(TAG, "getting hidden list from database");
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(true);
            MyApplication.closeDatabase();
        }

        mEntriesDeleted = new ArrayList<>();

        mAdapter.setWifiList(mListWifi);


        //Restore Context Action Bar state
        if (isActionModeOn) {
            mActionMode = startSupportActionMode(mActionModeCallback);
            for (int i = 0; i < mActionModeSelections.size(); i++) {
                mAdapter.toggleSelection(mActionModeSelections.get(i));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");

        MyApplication.getWritableDatabase().deleteWifiEntries(mEntriesDeleted, true);
        MyApplication.closeDatabase();
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
        outState.putBoolean(STATE_ACTION_MODE, isActionModeOn);
        outState.putIntegerArrayList(STATE_ACTION_MODE_SELECTIONS, mAdapter.getSelectedItems());

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /***************************************************************/
    /******************** Layout Setup Methods *********************/
    /***************************************************************/

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(this, false, null);
        mRecyclerView.setAdapter(mAdapter);

        //Setup ItemTouchHelper
//        mTouchHelperCallback = new MyTouchHelperCallback(mAdapter);

//        mTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, 0) {
//            @Override
//            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
//                return false;
//            }
//
//            @Override
//            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                Log.d(TAG, "onSwiped");
//
//                WifiEntry deleted = mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
//
//                mEntriesDeleted.add(deleted);
//                Intent data = getIntent();
//                data.putParcelableArrayListExtra(STATE_RESTORED_ENTRIES, mEntriesDeleted);
//                setResult(RESULT_OK, data);
//
//                Snackbar.make(mRoot,
//                        deleted.getTitle() + " " + getString(R.string.snackbar_wifi_restore),
//                        Snackbar.LENGTH_SHORT)
//                        .show();
//            }
//        };
//
//        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
//        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerTouchListener = new RecyclerTouchListener(this, mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onClick " + position);

                //while in ActionMode - regular clicks will also select items
                if (isActionModeOn) {
                    mAdapter.toggleSelection(position);
                    mRecyclerView.smoothScrollToPosition(position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onLongClick " + position);

                //Invoking Context Action Mode
                mAdapter.toggleSelection(position);
                mRecyclerView.smoothScrollToPosition(position);

                if (mActionMode != null) {
                    return;
                }
                mActionMode = startSupportActionMode(mActionModeCallback);
            }

            @Override
            public void onDoubleTap(View view, int position) {
                Log.d(TAG, "RecyclerView - onDoubleTap " + position);

                if (isActionModeOn) {
                    return;
                }
                WifiEntry entry = mListWifi.get(position);

                String textToCopy = "Wifi Name: " + entry.getTitle() + "\n"
                        + "Password: " + entry.getPassword() + "\n";

                copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, getString(R.string.snackbar_wifi_copy));
            }
        });

        mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);

    }

    public void setupActionModeCallback() {
        Log.d(TAG, "setupActionModeCallback");

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_context_archive, menu);

                isActionModeOn = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
                Log.d(TAG, "onActionItemClicked");
                final ArrayList<WifiEntry> selectedEntries = new ArrayList<>();
                final ArrayList<Integer> selectedItems = mAdapter.getSelectedItems();

                if(selectedItems.size() == 0) {
                    return false;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedEntries.add(mListWifi.get(selectedItems.get(i)));
                }

                switch (item.getItemId()) {

                    case R.id.menu_context_copy:
                        StringBuilder textToCopy = new StringBuilder();

                        for (WifiEntry entry : selectedEntries) {
                            textToCopy.append("Wifi Name: " + entry.getTitle() + "\n"
                                    + "Password: " + entry.getPassword() + "\n\n");
                        }

                        copyToClipboard(COPIED_WIFI_ENTRY, textToCopy.toString(), getString(R.string.snackbar_wifi_copy));
                        mode.finish();
                        return true;


                    case R.id.menu_context_restore:
                        mActionModeRestorePressed = true;

                        for (int i = selectedItems.size() - 1; i >= 0; i--) {
                            //Starting removal from end of list so Indexes wont change when item is removed
                            mAdapter.removeItem(selectedItems.get(i));
                        }

                        for (int i = 0; i < selectedEntries.size(); i++) {
                            mEntriesDeleted.add(selectedEntries.get(i));
                        }

                        Intent data = getIntent();
                        data.putParcelableArrayListExtra(STATE_RESTORED_ENTRIES, mEntriesDeleted);
                        setResult(RESULT_OK, data);

                        Snackbar.make(mRoot,
                                getString(R.string.snackbar_wifi_restore),
                                Snackbar.LENGTH_SHORT)
                                .show();

                        MyApplication.getWritableDatabase().deleteWifiEntries(selectedEntries, true);
                        MyApplication.closeDatabase();

                        mode.finish();
                        return true;

                    case R.id.menu_context_delete:
                        //TODO should "add from file" get deleted items?
                        //show Delete confirmation Dialog
                        String[] buttons = getResources().getStringArray(R.array.dialog_delete_buttons);

                        AlertDialog.Builder builder = new AlertDialog.Builder(HiddenWifiActivity.this, R.style.DeleteDialogTheme);

                        builder.setMessage(R.string.dialog_delete_message)
                                .setTitle(R.string.dialog_delete_title)
                                .setPositiveButton(buttons[0], new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteItems(selectedItems, selectedEntries);
                                        mode.finish();
                                    }
                                })
                                .setNegativeButton(buttons[1], new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        //Dismiss Dialog
                                    }
                                });

                        builder.create().show();
                        return true;

                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mAdapter.clearSelection();
                isActionModeOn = false;
                mActionMode = null;
            }
        };
    }


    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/

    //Copy to Clipboard Method
    private void copyToClipboard(String copiedLabel, String copiedText, String snackbarMessage) {

        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(copiedLabel, copiedText);
        clipboardManager.setPrimaryClip(clipData);

        Snackbar.make(mRoot, snackbarMessage, Snackbar.LENGTH_SHORT).show();
        Log.d(TAG, "copyToClipboard:\n" + clipData.toString());
    }


    private void deleteItems(ArrayList<Integer> selectedItems, ArrayList<WifiEntry> selectedEntries) {

        for (int i = selectedItems.size() - 1; i >= 0; i--) {
            //Starting removal from end of list so Indexes wont change when item is removed
            mAdapter.removeItem(selectedItems.get(i));
        }
        //TODO Currently Add from File should return deleted items.
        PasswordDB db = MyApplication.getWritableDatabase();
        db.deleteWifiEntries(selectedEntries, true);
        db.deleteWifiEntries(selectedEntries, false);
        MyApplication.closeDatabase();
    }

}
