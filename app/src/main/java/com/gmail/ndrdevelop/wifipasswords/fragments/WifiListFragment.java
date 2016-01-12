package com.gmail.ndrdevelop.wifipasswords.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.activities.IntroActivity;
import com.gmail.ndrdevelop.wifipasswords.activities.MainActivity;
import com.gmail.ndrdevelop.wifipasswords.activities.SettingsActivity;
import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.dialogs.CustomAlertDialogFragment;
import com.gmail.ndrdevelop.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndrdevelop.wifipasswords.dialogs.InputDialogFragment;
import com.gmail.ndrdevelop.wifipasswords.dialogs.InputDialogListener;
import com.gmail.ndrdevelop.wifipasswords.dialogs.RateItDialogFragment;
import com.gmail.ndrdevelop.wifipasswords.extras.AutoUpdateList;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RequestCodes;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;
import com.gmail.ndrdevelop.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndrdevelop.wifipasswords.recycler.MyTouchHelperCallback;
import com.gmail.ndrdevelop.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListAdapter;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListLoadedListener;
import com.gmail.ndrdevelop.wifipasswords.task.TaskLoadWifiEntries;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;


public class WifiListFragment extends Fragment implements WifiListLoadedListener,
        SearchView.OnQueryTextListener, CustomAlertDialogListener, InputDialogListener,
        ItemDragListener {

    static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label
    static final String STATE_RESTORED_ENTRIES = "state_restored_entries"; //HiddenActivityWifi intent.extra key
    static final String ROOT_ACCESS = "root_access";

    //onSavedInstance Keys
    static final String STATE_WIFI_ENTRIES = "state_wifi_entries"; //Parcel key
    static final String STATE_ACTION_MODE = "state_action_mode";
    static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";
    static final String STATE_SORT_MODE = "state_sort_mode";

    ArrayList<WifiEntry> mListWifi;

    boolean mSortModeEnabled = false;

    //Layout
    @Bind(R.id.fragment_main_container) FrameLayout mRoot;
    @Bind(R.id.progress_bar) ProgressBar mProgressBar;
    FloatingActionButton mFAB;

    //wpa_supplicant file
    String mPath;
    String mFileName;
    boolean mCurrentlyLoading = false;

    //RecyclerView
    @Bind(R.id.main_wifi_list_recycler) RecyclerView mRecyclerView;
    WifiListAdapter mAdapter;
    RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    ItemTouchHelper mItemTouchHelper;

    //SearchView
    SearchView mSearchView;
    ArrayList<WifiEntry> mSearchSavedList; //saves list for SearchView Live Search
    String mSearchSavedQuery = ""; //saves query for configuration change

    //Context Action Mode
    boolean mActionModeEnabled = false;
    boolean mAnimateChanges = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations
    ActionMode mActionMode;
    ArrayList<Integer> mActionModeSelections;
    ActionMode.Callback mActionModeCallback;


    public static WifiListFragment newInstance() {

        return new WifiListFragment();
    }


    public WifiListFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_wifi_list, container, false);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mListWifi = new ArrayList<>();

        ButterKnife.bind(this, layout);
        mFAB = ButterKnife.findById(getActivity(), R.id.fab);

        setupProgressBar();

        //Setup RecyclerView & Adapter
        setupRecyclerView();

        //Setup Context Action Mode
        setupActionModeCallback();

        //Setup Floating Action Button
        setupFAB();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean mFirstAppLaunch = sharedPreferences.getBoolean(MyApplication.FIRST_LAUNCH, true);
        boolean mRootAccess = sharedPreferences.getBoolean(ROOT_ACCESS, true);

        if (mFirstAppLaunch) {
            MyApplication.sShouldAutoUpdateList = false;
            getActivity().startActivityForResult(new Intent(getActivity(), IntroActivity.class), RequestCodes.ACTIVITY_INTRO_CODE);

        } else {

            if (savedInstanceState != null) {

                mRecyclerView.setLayoutAnimation(null);
                mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);
                mActionModeEnabled = savedInstanceState.getBoolean(STATE_ACTION_MODE);
                mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);
                mSortModeEnabled = savedInstanceState.getBoolean(STATE_SORT_MODE);

            } else {

                Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.simple_grow);
                mFAB.startAnimation(animation);

                mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
                MyApplication.closeDatabase();

                if (mListWifi.isEmpty() && mRootAccess) {

                    loadFromFile(true);
                }
            }
        }

        mAdapter.setWifiList(mListWifi);


        //Restore Context Action Mode state
        if (mActionModeEnabled) {
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            for (int i = 0; i < mActionModeSelections.size(); i++) {
                mAdapter.toggleSelection(mActionModeSelections.get(i));
            }

        } else if (mSortModeEnabled) { //Restore Sort Mode state
            sortMode(true);

        }

        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();

        RateItDialogFragment.show(getActivity(), getFragmentManager());
        AutoUpdateList.update(getActivity(), getFragmentManager());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        new Thread(this::updateDatabase).start();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES,
                mSearchView != null && mSearchView.isIconified() ? mListWifi : mSearchSavedList);


        if (mSearchView != null) {
            mSearchSavedQuery = mSearchView.getQuery().toString();
        }

        outState.putBoolean(STATE_SORT_MODE, mSortModeEnabled);
        outState.putBoolean(STATE_ACTION_MODE, mActionModeEnabled);
        outState.putIntegerArrayList(STATE_ACTION_MODE_SELECTIONS, mAdapter.getSelectedItems());
    }


    //WifiListLoadedListener method - called from TaskLoadWifiEntries
    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi, int numOfEntries, boolean resetDB) {

        //Hide Progress Bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        mListWifi = new ArrayList<>(listWifi);

        mAdapter.setWifiList(mListWifi);

        if (resetDB) {
            mRecyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.layout_left_to_right_slide));
            mRecyclerView.startLayoutAnimation();
        }

        //Show number of wifi entries inserted
        String snackbarMessage;

        if (numOfEntries > 0) {
            snackbarMessage = numOfEntries + " " + getString(R.string.snackbar_wifi_entries_inserted);

            if (!resetDB) {
                mRecyclerView.smoothScrollToPosition(mListWifi.size());
            }

        } else {
            snackbarMessage = getString(R.string.snackbar_wifi_entries_inserted_none);

        }

        Snackbar.make(mRoot, snackbarMessage, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_dismiss, v -> {
                    //Dismiss
                })
                .show();


        mCurrentlyLoading = false;
        hideFAB(false);
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        //Disable\Enable menu items according to SortMode
        menu.setGroupEnabled(R.id.menu_group_main, !mCurrentlyLoading);
        menu.setGroupVisible(R.id.menu_group_main, !mSortModeEnabled);
        menu.setGroupVisible(R.id.menu_group_sort_mode, mSortModeEnabled);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wifi_list_fragment, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        setupSearch(menu, searchItem);

        //Restore SearchView state
        if (!mSearchSavedQuery.isEmpty()) {
            MenuItemCompat.expandActionView(searchItem);
            mSearchView.setQuery(mSearchSavedQuery, true);
            mSearchView.clearFocus();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case R.id.action_add_from_file:
                loadFromFile(false);
                return true;

            case R.id.action_sort_start:
                sortMode(true);
                return true;

            case R.id.action_sort_done:
                sortMode(false);
                return true;

            case R.id.action_share:
                showShareDialog(mListWifi);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {


            case RequestCodes.ACTIVITY_INTRO_CODE: //Handle IntroActivity loadFromFile on finish.

                if (resultCode == Activity.RESULT_OK) {

                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(MyApplication.FIRST_LAUNCH, false).apply();

                    loadFromFile(true);
                    Toast toast = Toast.makeText(getActivity(), R.string.toast_root_request, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                break;


            case RequestCodes.ACTIVITY_ARCHIVE_CODE: //Handle ArchiveActivity items restored

                if (resultCode == Activity.RESULT_OK) {

                    if (data != null) {

                        ArrayList<WifiEntry> itemsRestored = data.getParcelableArrayListExtra(STATE_RESTORED_ENTRIES);
                        for (int i = 0; i < itemsRestored.size(); i++) {
                            WifiEntry entry = itemsRestored.get(i);
                            mAdapter.addItem(i, entry);
                        }
                        mRecyclerView.scrollToPosition(0);
                    }
                }
                break;

            case RequestCodes.DIALOG_PATH_ERROR_CODE:  //Handle Path Error Dialog

                //Hide Progress Bar
                if (mProgressBar.getVisibility() == View.VISIBLE) {
                    mProgressBar.setVisibility(View.GONE);
                }

                if (resultCode == RequestCodes.DIALOG_CONFIRM) {

                    FragmentActivity parent = getActivity();
                    ActivityOptionsCompat compat = ActivityOptionsCompat.makeCustomAnimation(parent, R.anim.right_in, R.anim.left_out);
                    parent.startActivityForResult(
                            new Intent(parent, SettingsActivity.class),
                            RequestCodes.SETTINGS_PATH_ERROR_CODE,
                            compat.toBundle());
                } //Else Dismissed
                break;

            case RequestCodes.DIALOG_LOAD_WARNING_CODE: //Handle LoadFromFile Warning Dialog

                if (resultCode == RequestCodes.DIALOG_CONFIRM) {

                    loadFromFile(true);

                } //Else Dismissed
                break;
        }
    }


    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/


    //Copy wpa_supplicant and extract data from it via AsyncTask
    public void loadFromFile(boolean resetDB) {

        if (!resetDB) {
            updateDatabase();

        } else {
            mListWifi = new ArrayList<>();
            new Thread(() -> {
                MyApplication.getWritableDatabase().purgeDatabase();
                MyApplication.closeDatabase();
            }).start();
            mAdapter.setWifiList(mListWifi);
            mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
        }

        mCurrentlyLoading = true;
        hideFAB(true);
        getActivity().invalidateOptionsMenu();


//        Snackbar.make(mRoot, R.string.snackbar_load_from_file, Snackbar.LENGTH_LONG).show();
        //Changed to Toast as subsequent snackbars cause FAB animation to glitch
        if (!resetDB) {
            Toast.makeText(getActivity(), R.string.snackbar_load_from_file, Toast.LENGTH_SHORT).show();
        }

        boolean manualLocation = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.pref_path_checkbox_key), false);

        if (manualLocation) {
            if (!getPath()) {

                showPathErrorDialog();
                return;
            }
            new TaskLoadWifiEntries(mPath, mFileName, resetDB, this, this).execute();

        } else {
            new TaskLoadWifiEntries(resetDB, this, this).execute();
        }


    }


    //Update database with changes made to wifi list.

    private void updateDatabase() {

        PasswordDB db = MyApplication.getWritableDatabase();
        db.deleteWifiEntries(mListWifi, false);
        db.insertWifiEntries(mListWifi, true, false);
        MyApplication.closeDatabase();
    }


    //Share wifi list
    private void shareWifiList(ArrayList<WifiEntry> listWifi) {

        String textToShare = "";

        for (int i = 0; i < listWifi.size(); i++) {

            WifiEntry current = listWifi.get(i);
            textToShare += "Wifi Name: " + current.getTitle() + "\n"
                    + "Password: " + current.getPassword() + "\n\n";
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);

    }

    //Copy to Clipboard Method
    private void copyToClipboard(String copiedLabel, String copiedText, String snackbarMessage) {

        ClipboardManager clipboardManager =
                (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(copiedLabel, copiedText);
        clipboardManager.setPrimaryClip(clipData);

        Snackbar.make(mRoot, snackbarMessage, Snackbar.LENGTH_SHORT).show();
    }


    //Retrieve wpa_supplicant Path from Settings
    private boolean getPath() {

        mPath = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(getString(R.string.pref_path_manual_key), getString(R.string.pref_path_default));

        if (mPath == null || mPath.replace(" ", "").isEmpty()) {
            return false;
        }

        //Split entire path to Path & Filename
        mFileName = mPath.substring(mPath.lastIndexOf("/") + 1);
        mPath = mPath.substring(0, mPath.lastIndexOf("/") + 1);

        return !mFileName.replace(" ", "").isEmpty();

    }

    //Toggle Search Mode
    public void searchMode(Menu menu, boolean isOn) {

        hideFAB(isOn);

        for (int i = 0; i < menu.size(); i++) {

            MenuItem item = menu.getItem(i);
            if (item.getGroupId() != R.id.menu_group_sort_mode) {
                item.setVisible(!isOn);
            }
        }
    }

    //Toggle Sort Mode
    public void sortMode(boolean isOn) {

        hideFAB(isOn);
        mAdapter.showDragHandler(isOn);

        //Disable Touch actions while in sort mode
        if (isOn) {
            mRecyclerView.removeOnItemTouchListener(mRecyclerTouchListener);
        } else {
            mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);
        }

        mSortModeEnabled = isOn;

        getActivity().invalidateOptionsMenu();
    }

    //Return Sort Mode Status - used OnBackPressed in MainActivity
    public boolean getSortModeStatus() {
        return mSortModeEnabled;
    }


    public void hideFAB(boolean hide) {



        if (hide) {
            mFAB.hide();
            mFAB.setEnabled(false);

        } else {
            mFAB.show();
            mFAB.setEnabled(true);
        }
    }

    public void toggleNoPassword() {

        boolean showNoPassword = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getBoolean(getString(R.string.pref_show_no_password_key), false);

        if (showNoPassword) {

            ArrayList<WifiEntry> listNoPassword = MyApplication.getWritableDatabase().getAllWifiEntries(false);
            MyApplication.closeDatabase();

            for (int i = 0; i < listNoPassword.size(); i++) {

                if (!(listNoPassword.get(i).getPassword()).equals(MyApplication.NO_PASSWORD_TEXT)) {
                    listNoPassword.remove(i);
                    i--;
                }
            }

            for (int i = 0; i < listNoPassword.size(); i++) {
                mAdapter.addItem(mListWifi.size(), listNoPassword.get(i));
            }

            mRecyclerView.smoothScrollToPosition(mListWifi.size());

            Snackbar.make(mRoot, listNoPassword.size() + " " + getString(R.string.snackbar_wifi_no_password_added), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_dismiss, v -> {
                        //dismiss
                    })
                    .show();
        } else {

            int removedEntries = 0;

            for (int i = mListWifi.size() - 1; i >= 0; i--) {

                if (mListWifi.get(i).getPassword().equals(MyApplication.NO_PASSWORD_TEXT)) {
                    mAdapter.removeItem(i);
                    removedEntries++;
                }
            }

            mRecyclerView.smoothScrollToPosition(0);

            Snackbar.make(mRoot, removedEntries + " " + getString(R.string.snackbar_wifi_no_password_removed), Snackbar.LENGTH_LONG)
                    .setAction(R.string.snackbar_dismiss, v -> {
                        //dismiss
                    })
                    .show();

        }
    }

    //Sort Mode Method - sort via drag
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {

        mItemTouchHelper.startDrag(viewHolder);
    }


    /***************************************************************/
    /******************** Layout Setup Methods *********************/
    /***************************************************************/


    private void setupProgressBar() {

        //backward compatible MaterialProgressBar - https://github.com/DreaminginCodeZH/MaterialProgressBar
        IndeterminateProgressDrawable progressDrawable = new IndeterminateProgressDrawable(getActivity());
        mProgressBar.setIndeterminateDrawable(progressDrawable);

    }

    private void setupRecyclerView() {

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        //Setup ItemTouchHelper
        ItemTouchHelper.Callback mTouchHelperCallback = new MyTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        //Setup OnItemTouchListener
        mRecyclerTouchListener = new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
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
                    Toast.makeText(getActivity(), R.string.toast_cab_double_tap, Toast.LENGTH_SHORT).show();
                    return;
                }
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            }

            @Override
            public void onDoubleTap(View view, int position) {

                if (mActionModeEnabled || position >= mListWifi.size()) {
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

    //Setup Context Action Mode
    public void setupActionModeCallback() {

        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_context, menu);

                //Fix for CAB forcing icons into overflow menu - doing this via XML doesn't work.
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                mActionModeEnabled = true;

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                hideFAB(true);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                final ArrayList<WifiEntry> selectedEntries = new ArrayList<>();
                final ArrayList<Integer> selectedItems = mAdapter.getSelectedItems();

                if (selectedItems.size() == 0 || mListWifi.size() == 0) {
                    Toast.makeText(getActivity(), R.string.toast_nothing_selected, Toast.LENGTH_SHORT).show();
                    return false;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedEntries.add(mListWifi.get(selectedItems.get(i)));
                }

                switch (item.getItemId()) {

                    case R.id.menu_context_top:
                        mRecyclerView.smoothScrollToPosition(0);

                        for (int i = selectedItems.size() - 1; i >= 0; i--) {
                            mAdapter.removeItem(selectedItems.get(i));
                        }

                        for (int i = 0; i < selectedEntries.size(); i++) {
                            mAdapter.addItem(0, selectedEntries.get(i));
                        }


                        Snackbar.make(mRoot, selectedEntries.size() + " " + getString(R.string.snackbar_move_to_top), Snackbar.LENGTH_LONG)
                                .setAction(R.string.snackbar_undo, v -> {

                                    for (int i = 0; i < selectedEntries.size(); i++) {
                                        mAdapter.removeItem(0);
                                    }

                                    for (int i = 0; i < selectedEntries.size(); i++) {
                                        mAdapter.addItem(selectedItems.get(i), selectedEntries.get(i));

                                    }
                                })
                                .show();
                        mode.finish();
                        return true;

                    case R.id.menu_context_archive:
                        mAnimateChanges = true;

                        for (int i = selectedItems.size() - 1; i >= 0; i--) {
                            //Starting removal from end of list so Indexes wont change when item is removed
                            mAdapter.removeItem(selectedItems.get(i));
                        }

                        final PasswordDB db = MyApplication.getWritableDatabase();
                        db.insertWifiEntries(selectedEntries, true, true);

                        mode.finish();

                        if (selectedItems.size() > 0) {
                            String snackbarMessage;

                            if (selectedItems.size() > 1) {
                                snackbarMessage = selectedItems.size() + " " + getString(R.string.snackbar_wifi_archive_multiple);
                            } else {
                                snackbarMessage = getString(R.string.snackbar_wifi_archive);
                            }

                            Snackbar.make(mRoot, snackbarMessage, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.snackbar_undo, v -> {

                                        for (int i = 0; i < selectedItems.size(); i++) {
                                            mAdapter.addItem(selectedItems.get(i), selectedEntries.get(i));

                                        }
                                        db.deleteWifiEntries(selectedEntries, true);
                                    })
                                    .show();
                        }
                        MyApplication.closeDatabase();
                        return true;

                    case R.id.menu_context_copy:
                        String textToCopy = "";

                        for (WifiEntry entry : selectedEntries) {
                            textToCopy += "Wifi Name: " + entry.getTitle() + "\n"
                                    + "Password: " + entry.getPassword() + "\n\n";
                        }

                        copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, getString(R.string.snackbar_wifi_copy));
                        mode.finish();
                        return true;

                    case R.id.menu_context_share:

                        showShareDialog(selectedEntries);
                        return true;

                    case R.id.menu_context_tag:

                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(InputDialogFragment.ENTRIES_KEY, selectedEntries);
                        bundle.putIntegerArrayList(InputDialogFragment.POSITIONS_LEY, selectedItems);

                        InputDialogFragment fragment = InputDialogFragment.getInstance(InputDialogFragment.INPUT_TAG, bundle);
                        fragment.setTargetFragment(getFragmentManager().findFragmentByTag(MainActivity.WIFI_LIST_FRAGMENT_TAG), RequestCodes.DIALOG_TAG_CODE);
                        fragment.show(getFragmentManager(), getString(R.string.dialog_tag_key));

                        return true;


                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

                if (!mAnimateChanges) {
                    mAdapter.clearSelection();
                }
                hideFAB(false);
                mAnimateChanges = false;
                mRecyclerView.setNestedScrollingEnabled(true);
                mActionModeEnabled = false;
                mActionMode = null;
            }
        };

    }

    //Setup SearchView
    private void setupSearch(final Menu menu, MenuItem searchItem) {

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            //on Search - Collapse Toolbar & disable expanding, hide title;
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                searchMode(menu, true);
                //Save full list to restore once search is over (Live Search changes list)
                mSearchSavedList = new ArrayList<>(mListWifi);

                return true;
            }

            //on Close - Expand Toolbar & enable expanding, show title;
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {

                searchMode(menu, false);

                mSearchView.setQuery("", false);
                mSearchSavedQuery = "";
                //Restore full list
                mListWifi = mSearchSavedList;

                return true;
            }
        });

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(this);
    }


    private void setupFAB() {

        mFAB.setImageResource(R.drawable.ic_action_add);
        mFAB.setOnClickListener(v -> showAddWifiDialog());

    }


    /********************************************************/
    /******************** Dialog Methods ********************/
    /********************************************************/

    @Override
    public void onSubmitAddDialog(String title, String password) {

        WifiEntry entry = new WifiEntry(title, password);
        mAdapter.addItem(0, entry);
        mRecyclerView.smoothScrollToPosition(0);
        addToDatabase(entry);
    }

    private void addToDatabase(WifiEntry entry) {

        PasswordDB db = MyApplication.getWritableDatabase();

        ArrayList<WifiEntry> entries = new ArrayList<>();
        entries.add(entry);

        db.insertWifiEntries(entries, true, false);
        MyApplication.closeDatabase();
    }

    private void showAddWifiDialog() {

        if(getActivity() == null || !isAdded())
            return;

        InputDialogFragment fragment = InputDialogFragment
                .getInstance(InputDialogFragment.INPUT_ENTRY, null);
        fragment.setTargetFragment(this, RequestCodes.DIALOG_ADD_CODE);
        fragment.show(getFragmentManager(), getString(R.string.dialog_add_key));

    }

    @Override
    public void onSubmitTagDialog(String tag, ArrayList<WifiEntry> listWifi, ArrayList<Integer> positions) {

        if(getActivity() == null || !isAdded())
            return;

        for (int i = 0; i < positions.size(); i++) {

            mListWifi.get(positions.get(i)).setTag(tag);
        }

        mActionMode.finish();
        mAdapter.notifyDataSetChanged();
        MyApplication.getWritableDatabase().updateWifiTags(listWifi, tag);
        MyApplication.closeDatabase();
    }


    private void showShareDialog(final ArrayList<WifiEntry> listWifi) {

        if(getActivity() == null || !isAdded())
            return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean mShowShareDialog = sharedPreferences.getBoolean(getString(R.string.pref_share_warning_key), true);

        if (mShowShareDialog) {

            AlertDialog.Builder builder;

            if (MyApplication.sIsDark) {
                builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme_Dark);
            } else {
                builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
            }

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View shareDialogLayout = inflater.inflate(R.layout.dialog_share_warning, null);
            final CheckBox dontShowShareDialog = (CheckBox) shareDialogLayout.findViewById(R.id.dont_show_checkbox);

            String[] buttons = getResources().getStringArray(R.array.dialog_warning_share_buttons);

            builder.setView(shareDialogLayout)
                    .setPositiveButton(buttons[0], (dialog, which) -> {
                        if (dontShowShareDialog.isChecked()) {

                            SharedPreferences sharedPreferences1 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            sharedPreferences1.edit().putBoolean(getString(R.string.pref_share_warning_key), false).apply();
                        }

                        shareWifiList(listWifi);
                    })
                    .setNegativeButton(buttons[1], (dialog, which) -> {
                        //Dismiss
                    });

            builder.create().show();

        } else {
            shareWifiList(listWifi);
        }
    }


    //TaskLoadWifiEntries creating PathError Dialog.
    @Override
    public void showPathErrorDialog() {

        if(getActivity() == null || !isAdded()) {
            Toast.makeText(MyApplication.getAppContext(), R.string.dialog_error_path_message, Toast.LENGTH_SHORT).show();
            return;
        }

        Answers.getInstance().logCustom(new CustomEvent("PathError")
                .putCustomAttribute("device", Build.DEVICE)
                .putCustomAttribute("model", Build.MODEL)
                .putCustomAttribute("manufacturer", Build.MANUFACTURER)
                .putCustomAttribute("id", "99"));


        String title = getString(R.string.dialog_error_path_title);
        String message = getString(R.string.dialog_error_path_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_path_buttons);

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.getInstance(title, message, buttons);
        dialog.setTargetFragment(this, RequestCodes.DIALOG_PATH_ERROR_CODE);

        getFragmentManager().beginTransaction().add(dialog, getString(R.string.dialog_error_path_key)).commitAllowingStateLoss();
    }



    public void showRootErrorDialog() {

        if(getActivity() == null || !isAdded()) {
            Toast.makeText(MyApplication.getAppContext(), R.string.dialog_error_root_title, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = getString(R.string.dialog_error_root_title);
        String message = getString(R.string.dialog_error_root_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_root_button);

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.getInstance(title, message, buttons);
        dialog.setTargetFragment(this, 0);

        getFragmentManager().beginTransaction().add(dialog, getString(R.string.dialog_error_root_key)).commitAllowingStateLoss();

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(ROOT_ACCESS, false).apply();

        //Restore Menu functions
        hideFAB(false);
        mCurrentlyLoading = false;
        getActivity().invalidateOptionsMenu();

        //Remove Progress bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }
    }


    /********************************************************/
    /******************** Search Methods ********************/
    /********************************************************/

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {

        //filter logic
        final ArrayList<WifiEntry> filteredWifiList = filter(mSearchSavedList, query);
        mAdapter.animateTo(filteredWifiList);
        mRecyclerView.smoothScrollToPosition(0);
        return true;
    }


    private ArrayList<WifiEntry> filter(ArrayList<WifiEntry> listWifi, String query) {
        query = query.toLowerCase();

        final ArrayList<WifiEntry> filteredWifiList = new ArrayList<>();

        for (WifiEntry entry : listWifi) {
            final String title = entry.getTitle().toLowerCase();
            final String tag = entry.getTag().toLowerCase();

            if (title.contains(query) || tag.contains(query)) {
                filteredWifiList.add(entry);
            }
        }

        return filteredWifiList;
    }

}
