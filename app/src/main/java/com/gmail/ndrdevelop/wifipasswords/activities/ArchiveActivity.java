package com.gmail.ndrdevelop.wifipasswords.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.dialogs.AboutDialogFragment;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;
import com.gmail.ndrdevelop.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListAdapter;
import com.gmail.ndrdevelop.wifipasswords.task.TaskCheckPasscode;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;


public class ArchiveActivity extends AppCompatActivity {

    static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label

    //onSavedInstance Keys
    static final String STATE_HIDDEN_ENTRIES = "state_hidden_entries";
    static final String STATE_RESTORED_ENTRIES = "state_restored_entries";
    static final String STATE_ACTION_MODE = "state_action_mode";
    static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";

    ArrayList<WifiEntry> mListWifi;
    ArrayList<WifiEntry> mEntriesRestored; //track restored entries to add to main list adapter

    @Bind(R.id.activity_hidden_wifi_container) CoordinatorLayout mRoot;
    @Bind(R.id.app_bar) Toolbar mToolbar;
    @Bind(R.id.hidden_wifi_list_recycler) RecyclerView mRecyclerView;
    WifiListAdapter mAdapter;

    //Context Action Mode
    boolean mActionModeEnabled = false;
    boolean mAnimateChanges = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations
    ActionMode mActionMode;
    ArrayList<Integer> mActionModeSelections;
    ActionMode.Callback mActionModeCallback;


    public ArchiveActivity() {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.sIsDark) {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);

        //Set Activity Transition - Lollipop+
        if (Build.VERSION.SDK_INT >= 21) {

            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition slideFromRight = transitionInflater.inflateTransition(R.transition.activity_slide_right);
            Transition slideFromLeft = transitionInflater.inflateTransition(R.transition.activity_slide_left);

            getWindow().setEnterTransition(slideFromLeft);
            getWindow().setExitTransition(slideFromRight);

        }
        setContentView(R.layout.activity_archive);
        ButterKnife.bind(this);

        mListWifi = new ArrayList<>();

        setSupportActionBar(mToolbar);

        ActionBar sBar;
        if ((sBar = getSupportActionBar()) != null) {
            sBar.setDisplayHomeAsUpEnabled(true);
        }

        setupRecyclerView();

        setupActionModeCallback();

        if (savedInstanceState != null) {

            mRecyclerView.setLayoutAnimation(null);
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_HIDDEN_ENTRIES);
            mActionModeEnabled = savedInstanceState.getBoolean(STATE_ACTION_MODE);
            mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);
            mEntriesRestored = savedInstanceState.getParcelableArrayList(STATE_RESTORED_ENTRIES);

        } else {

            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(true);
            MyApplication.closeDatabase();

            mEntriesRestored = new ArrayList<>();
        }



        mAdapter.setWifiList(mListWifi);


        //Restore Context Action Bar state
        if (mActionModeEnabled) {
            mActionMode = startSupportActionMode(mActionModeCallback);
            for (int i = 0; i < mActionModeSelections.size(); i++) {
                mAdapter.toggleSelection(mActionModeSelections.get(i));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        if(MyApplication.mPasscodeActivated && MyApplication.mAppWentBackground) {
            startActivity(new Intent(this, PasscodeActivity.class));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();


        MyApplication.getWritableDatabase().deleteWifiEntries(mEntriesRestored, true);
        MyApplication.closeDatabase();

        if(MyApplication.mPasscodeActivated && !isFinishing()) {

            new TaskCheckPasscode(getApplicationContext()).execute();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(STATE_HIDDEN_ENTRIES, mListWifi);
        outState.putBoolean(STATE_ACTION_MODE, mActionModeEnabled);
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
                AboutDialogFragment dialog = AboutDialogFragment.getInstance();
                dialog.show(getFragmentManager(), getString(R.string.dialog_about_key));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /***************************************************************/
    /******************** Layout Setup Methods *********************/
    /***************************************************************/

    private void setupRecyclerView() {

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(this, null);
        mRecyclerView.setAdapter(mAdapter);

        RecyclerView.OnItemTouchListener recyclerTouchListener = new RecyclerTouchListener(this, mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {

                //while in ActionMode - regular clicks will also select items
                if (mActionModeEnabled) {
                    mAdapter.toggleSelection(position);
                    mRecyclerView.smoothScrollToPosition(position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {

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

                if (mActionModeEnabled) {
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

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_context_archive, menu);

                mActionModeEnabled = true;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {

                final ArrayList<WifiEntry> selectedEntries = new ArrayList<>();
                final ArrayList<Integer> selectedItems = mAdapter.getSelectedItems();

                if(selectedItems.size() == 0) {
                    Toast.makeText(ArchiveActivity.this, R.string.toast_nothing_selected, Toast.LENGTH_SHORT).show();
                    return false;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedEntries.add(mListWifi.get(selectedItems.get(i)));
                }

                switch (item.getItemId()) {

                    case R.id.menu_context_copy:
                        String textToCopy = "";

                        for (WifiEntry entry : selectedEntries) {
                            textToCopy += "Wifi Name: " + entry.getTitle() + "\n"
                                    + "Password: " + entry.getPassword() + "\n\n";
                        }

                        copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, getString(R.string.snackbar_wifi_copy));
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

                        AlertDialog.Builder builder;

                        if (MyApplication.sIsDark) {
                            builder = new AlertDialog.Builder(ArchiveActivity.this, R.style.DeleteDialogTheme_Dark);
                        } else {
                            builder = new AlertDialog.Builder(ArchiveActivity.this, R.style.DeleteDialogTheme);
                        }

                        builder.setMessage(R.string.dialog_delete_message)
                                .setTitle(R.string.dialog_delete_title)
                                .setPositiveButton(buttons[0], (dialog, which) -> {
                                    deleteItems(selectedItems, selectedEntries);
                                    mode.finish();
                                })
                                .setNegativeButton(buttons[1], (dialog, which) -> {
                                    //Dismiss Dialog
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
                mActionModeEnabled = false;
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
