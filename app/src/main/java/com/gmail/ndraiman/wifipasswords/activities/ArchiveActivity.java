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
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.dialogs.HelpDialogFragment;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;
import com.gmail.ndraiman.wifipasswords.task.TaskCheckPasscode;

import java.util.ArrayList;


public class ArchiveActivity extends AppCompatActivity {

    private static final String TAG = "ArchiveActivity";
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label

    private static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";
    private static final String STATE_RESTORED_ENTRIES = "state_restored_entries";
    private static final String STATE_ACTION_MODE = "state_action_mode";
    private static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";

    private ArrayList<WifiEntry> mListWifi;

    private CoordinatorLayout mRoot;

    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;

    private ArrayList<WifiEntry> mEntriesRestored; //track restored entries to add to main list adapter

    //Context Action Mode
    private ActionMode mActionMode;
    private ArrayList<Integer> mActionModeSelections;
    private ActionMode.Callback mActionModeCallback;
    private boolean mActionModeOn = false;
    private boolean mAnimateChanges = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations


    public ArchiveActivity() {

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
        setContentView(R.layout.activity_archive);

        mListWifi = new ArrayList<>();

        mRoot = (CoordinatorLayout) findViewById(R.id.activity_hidden_wifi_container);
        mRecyclerView = (RecyclerView) findViewById(R.id.hidden_wifi_list_recycler);
        Toolbar toolbar  = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        ActionBar sBar;
        if ((sBar = getSupportActionBar()) != null) {
            sBar.setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();

        setupActionModeCallback();

        if (savedInstanceState != null) {
            Log.d(TAG, "extracting hidden list from parcelable");
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_HIDDEN_ENTRIES);
            mActionModeOn = savedInstanceState.getBoolean(STATE_ACTION_MODE);
            mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);
            mEntriesRestored = savedInstanceState.getParcelableArrayList(STATE_RESTORED_ENTRIES);

        } else {
            Log.d(TAG, "getting hidden list from database");
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(true);
            MyApplication.closeDatabase();

            mEntriesRestored = new ArrayList<>();
        }



        mAdapter.setWifiList(mListWifi);


        //Restore Context Action Bar state
        if (mActionModeOn) {
            mActionMode = startSupportActionMode(mActionModeCallback);
            for (int i = 0; i < mActionModeSelections.size(); i++) {
                mAdapter.toggleSelection(mActionModeSelections.get(i));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        if(MyApplication.mPasscodeActivated && MyApplication.mAppWentBackground) {
            startActivity(new Intent(this, PasscodeActivity.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");

        MyApplication.getWritableDatabase().deleteWifiEntries(mEntriesRestored, true);
        MyApplication.closeDatabase();

        if(MyApplication.mPasscodeActivated && !isFinishing()) {
            Log.e(TAG, "executing TaskCheckPasscode()");
            new TaskCheckPasscode(getApplicationContext(), this).execute();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");

        outState.putParcelableArrayList(STATE_HIDDEN_ENTRIES, mListWifi);
        outState.putBoolean(STATE_ACTION_MODE, mActionModeOn);
        outState.putIntegerArrayList(STATE_ACTION_MODE_SELECTIONS, mAdapter.getSelectedItems());
        outState.putParcelableArrayList(STATE_RESTORED_ENTRIES, mEntriesRestored);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_help:
                HelpDialogFragment dialog = HelpDialogFragment.getInstance();
                dialog.show(getFragmentManager(), getString(R.string.dialog_about_key));
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

        RecyclerView.OnItemTouchListener recyclerTouchListener = new RecyclerTouchListener(this, mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onClick " + position);

                //while in ActionMode - regular clicks will also select items
                if (mActionModeOn) {
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

                if (mActionModeOn) {
                    return;
                }
                WifiEntry entry = mListWifi.get(position);

                String textToCopy = "Wifi Name: " + entry.getTitle() + "\n"
                        + "Password: " + entry.getPassword() + "\n";

                copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, getString(R.string.snackbar_wifi_copy));
            }
        });

        mRecyclerView.addOnItemTouchListener(recyclerTouchListener);

    }

    public void setupActionModeCallback() {
        Log.d(TAG, "setupActionModeCallback");

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_context_archive, menu);

                mActionModeOn = true;
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
                        mAnimateChanges = true;

                        for (int i = selectedItems.size() - 1; i >= 0; i--) {
                            //Starting removal from end of list so Indexes wont change when item is removed
                            mAdapter.removeItem(selectedItems.get(i));
                        }

                        for (int i = 0; i < selectedEntries.size(); i++) {
                            mEntriesRestored.add(selectedEntries.get(i));
                        }

                        Intent data = getIntent();
                        data.putParcelableArrayListExtra(STATE_RESTORED_ENTRIES, mEntriesRestored);
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

                        mAnimateChanges = true;
                        //show Delete confirmation Dialog
                        String[] buttons = getResources().getStringArray(R.array.dialog_delete_buttons);

                        AlertDialog.Builder builder = new AlertDialog.Builder(ArchiveActivity.this, R.style.DeleteDialogTheme);

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

                if(!mAnimateChanges) {
                    mAdapter.clearSelection();
                }

                mAnimateChanges = false;
                mActionModeOn = false;
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

        PasswordDB db = MyApplication.getWritableDatabase();
        db.insertDeleted(selectedEntries);
        MyApplication.closeDatabase();
    }

}
