package com.gmail.ndraiman.wifipasswords.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
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

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.activities.IntroActivity;
import com.gmail.ndraiman.wifipasswords.activities.MainActivity;
import com.gmail.ndraiman.wifipasswords.activities.SettingsActivity;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.dialogs.CustomAlertDialogFragment;
import com.gmail.ndraiman.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndraiman.wifipasswords.dialogs.InputDialogFragment;
import com.gmail.ndraiman.wifipasswords.dialogs.InputDialogListener;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.extras.RequestCodes;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndraiman.wifipasswords.recycler.MyTouchHelperCallback;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerScrollListener;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListLoadedListener;
import com.gmail.ndraiman.wifipasswords.task.TaskLoadWifiEntries;

import java.util.ArrayList;

import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;


public class WifiListFragment extends Fragment implements WifiListLoadedListener,
        SearchView.OnQueryTextListener, CustomAlertDialogListener, InputDialogListener,
        ItemDragListener {

    private static final String TAG = "WifiListFragment";
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label
    private static final String STATE_RESTORED_ENTRIES = "state_restored_entries"; //HiddenActivityWifi intent.extra key

    private ArrayList<WifiEntry> mListWifi;

    private boolean mSortModeOn = false;

    //OnSavedInstance Keys
    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries"; //Parcel key
    private static final String STATE_ACTION_MODE = "state_action_mode";
    private static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";
    private static final String STATE_SORT_MODE = "state_sort_mode";

    //Layout
    private FrameLayout mRoot;
    private AppBarLayout mAppBarLayout;
    private FloatingActionButton mFAB;
    private ProgressBar mProgressBar;

    //wpa_supplicant file
    private String mPath;
    private String mFileName;
    private boolean mCurrentlyLoading = false;

    //RecyclerView
    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private RecyclerView.OnItemTouchListener mRecyclerTouchListener;
    private ItemTouchHelper mItemTouchHelper;

    //SearchView
    private SearchView mSearchView;
    private ArrayList<WifiEntry> mSearchSavedList; //saves list for SearchView Live Search
    private String mSearchSavedQuery = ""; //saves query for configuration change

    //Context Action Mode
    private ActionMode mActionMode;
    private ArrayList<Integer> mActionModeSelections;
    private ActionMode.Callback mActionModeCallback;
    private boolean mActionModeOn = false;
    private boolean mAnimateChanges = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations

    //Share Warning Dialog
    private boolean mShowShareDialog;

    private boolean mFirstAppLaunch;
    private static final String FIRST_LAUNCH = "first_launch";

    private boolean mRootAccess;
    private static final String ROOT_ACCESS = "root_access";


    public static WifiListFragment newInstance() {

        return new WifiListFragment();
    }


    public WifiListFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View layout = inflater.inflate(R.layout.fragment_wifi_list, container, false);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mListWifi = new ArrayList<>();

        bindViews(layout);

        setupProgressBar();

        //Setup RecyclerView & Adapter
        setupRecyclerView();

        //Setup Context Action Mode
        setupActionModeCallback();

        //Setup Floating Action Button
        setupFAB();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mFirstAppLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH, true);
        mRootAccess = sharedPreferences.getBoolean(ROOT_ACCESS, true);

        if (mFirstAppLaunch) {
            getActivity().startActivityForResult(new Intent(getActivity(), IntroActivity.class), RequestCodes.ACTIVITY_INTRO_CODE);
            sharedPreferences.edit().putBoolean(FIRST_LAUNCH, false).apply();

        } else {

            if (savedInstanceState != null) {

                Log.d(TAG, "restoring from savedInstanceState");

                mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);
                mActionModeOn = savedInstanceState.getBoolean(STATE_ACTION_MODE);
                mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);
                mSortModeOn = savedInstanceState.getBoolean(STATE_SORT_MODE);

            } else {

                Log.d(TAG, "getting WifiEntries from database");

                mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
                MyApplication.closeDatabase();

                if (mListWifi.isEmpty() && mRootAccess) {

                    loadFromFile(true);
                    Log.d(TAG, "executing task from onCreateView");
                }
            }
        }

        mAdapter.setWifiList(mListWifi);

//        ArrayList<WifiEntry> placeholderData = new ArrayList<>();
//        for (int i = 0; i < 50; i++) {
//            WifiEntry current = new WifiEntry("Wifi " + (i+1), "Password " + (i+1));
//            placeholderData.add(current);
//        }
//        mAdapter.setWifiList(placeholderData);

        //Restore Context Action Bar state
        if (mActionModeOn) {
            mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            for (int i = 0; i < mActionModeSelections.size(); i++) {
                mAdapter.toggleSelection(mActionModeSelections.get(i));
            }
        }

        return layout;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        if (mSortModeOn) {
            sortMode(true);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        updateDatabase();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES,
                mSearchView != null && mSearchView.isIconified() ? mListWifi : mSearchSavedList);

        if (mSearchView != null) {
            mSearchSavedQuery = mSearchView.getQuery().toString();
        }

        outState.putBoolean(STATE_SORT_MODE, mSortModeOn);
        outState.putBoolean(STATE_ACTION_MODE, mActionModeOn);
        outState.putIntegerArrayList(STATE_ACTION_MODE_SELECTIONS, mAdapter.getSelectedItems());
    }


    //WifiListLoadedListener method - called from TaskLoadWifiEntries
    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi, int numOfEntries, boolean resetDB) {
        Log.d(TAG, "onWifiListLoaded");

        //Hide Progress Bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        mListWifi = new ArrayList<>(listWifi);

        mAdapter.setWifiList(mListWifi);


        //Show number of wifi entries inserted
        String snackbarMessage;

        if (numOfEntries > 0) {
            snackbarMessage = numOfEntries + " " + getString(R.string.snackbar_wifi_entries_inserted);

            if(!resetDB) {
                mRecyclerView.smoothScrollToPosition(mListWifi.size());
            }

        } else {
            snackbarMessage = getString(R.string.snackbar_wifi_entries_inserted_none);

        }

        Snackbar.make(mRoot, snackbarMessage, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Dismiss
                    }
                })
                .show();


        mCurrentlyLoading = false;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu");

        //Disable\Enable menu items according to SortMode
        menu.setGroupEnabled(R.id.menu_group_main, !mCurrentlyLoading);
        menu.setGroupVisible(R.id.menu_group_main, !mSortModeOn);
        menu.setGroupVisible(R.id.menu_group_sort_mode, mSortModeOn);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wifi_list_fragment, menu);
        Log.d(TAG, "onCreateOptionsMenu");

        MenuItem searchItem = menu.findItem(R.id.action_search);

        setupSearch(menu, searchItem);

        Log.d(TAG, "mSearchSavedQuery = " + mSearchSavedQuery);
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
        Log.e(TAG, "onActivityResult - requestCode = " + requestCode + ", resultCode = " + resultCode);

        switch (requestCode) {


            case RequestCodes.ACTIVITY_INTRO_CODE:

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Returning From IntroApp - loading from file");
                    loadFromFile(true);
                    Toast toast = Toast.makeText(getActivity(), R.string.toast_root_request, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
                break;


            case RequestCodes.ACTIVITY_HIDDEN_CODE: //Handle ArchiveActivity items restored

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "ArchiveActivity - Items Restored");
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

            case RequestCodes.DIALOG_ERROR_CODE:  //Handle Path Error Dialog

                if (resultCode == RequestCodes.DIALOG_CONFIRM) {
                    Log.d(TAG, "Dialog Error - Confirm");
                    FragmentActivity parent = getActivity();
                    ActivityOptionsCompat compat = ActivityOptionsCompat.makeCustomAnimation(parent, R.anim.right_in, R.anim.left_out);
                    parent.startActivityForResult(
                            new Intent(parent, SettingsActivity.class),
                            RequestCodes.ACTIVITY_SETTINGS_CODE,
                            compat.toBundle());
                } //Else Dismissed
                break;

            case RequestCodes.DIALOG_WARNING_CODE: //Handle LoadFromFile Warning Dialog

                if (resultCode == RequestCodes.DIALOG_CONFIRM) {
                    Log.d(TAG, "Dialog Warning - Confirm");
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
        Log.d(TAG, "loadFromFile");

        if (!resetDB) {
            updateDatabase();

        } else {
            mAdapter.setWifiList(new ArrayList<WifiEntry>());
            mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
        }

        getPath();

        mCurrentlyLoading = true;
        getActivity().invalidateOptionsMenu();


//        Snackbar.make(mRoot, R.string.snackbar_load_from_file, Snackbar.LENGTH_LONG).show();
        //Changed to Toast as subsequent snackbars cause FAB to glitch
        Toast.makeText(getActivity(), R.string.snackbar_load_from_file, Toast.LENGTH_SHORT).show();
        new TaskLoadWifiEntries(mPath, mFileName, resetDB, this, this).execute();

    }


    //Update database with changes made to wifi list.

    private void updateDatabase() {

        Log.d(TAG, "updateDatabase()");
        PasswordDB db = MyApplication.getWritableDatabase();
        db.deleteWifiEntries(mListWifi, false);
        db.insertWifiEntries(mListWifi, true, false);
        MyApplication.closeDatabase();
    }


    //Share wifi list
    private void shareWifiList(ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "shareWifiList");

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
        Log.d(TAG, "copyToClipboard:\n" + clipData.toString());
    }


    //Retrieve wpa_supplicant Path from Settings
    private void getPath() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String prefListChoice = sharedPreferences.getString(getString(R.string.pref_path_list_key), getString(R.string.pref_path_default));

        if (prefListChoice.equals(getString(R.string.pref_path_list_manual)))
            mPath = sharedPreferences.getString(getString(R.string.pref_path_manual_key), getString(R.string.pref_path_default));
        else
            mPath = prefListChoice;

        //Split entire path to Path & Filename
        mFileName = mPath.substring(mPath.lastIndexOf("/") + 1);
        mPath = mPath.substring(0, mPath.lastIndexOf("/") + 1);

        Log.d(TAG, "getPath() - path = " + mPath + "\n filename = " + mFileName);

    }

    //Toggle Search Mode
    public void searchMode(Menu menu, boolean isOn) {
        Log.d(TAG, "searchMode - isOn = " + isOn);

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
        Log.d(TAG, "sortMode - isOn = " + isOn);
        hideFAB(isOn);
        mAdapter.showDragHandler(isOn);

        //Disable Touch actions while in sort mode
        if (isOn) {
            mRecyclerView.removeOnItemTouchListener(mRecyclerTouchListener);
        } else {
            mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);
        }

        mSortModeOn = isOn;

        getActivity().invalidateOptionsMenu();
    }

    //Return Sort Mode Status - used OnBackPressed in MainActivity
    public boolean getSortModeStatus() {
        return mSortModeOn;
    }


    public void hideFAB(boolean hide) {
        Log.d(TAG, "hideFAB() called with: " + "hide = [" + hide + "]");
//        mRecyclerView.setNestedScrollingEnabled(!hide);

        if (hide) {
            mFAB.hide();

        } else {
            mFAB.show();

        }
    }

    //Sort Mode Method - sort via drag
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onStartDrag");
        mItemTouchHelper.startDrag(viewHolder);
    }


    /***************************************************************/
    /******************** Layout Setup Methods *********************/
    /***************************************************************/

    private void bindViews(View layout) {

        //Init local Views
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.main_wifi_list_recycler);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
        mRoot = (FrameLayout) layout.findViewById(R.id.fragment_main_container);

        //get Activity Views
        mFAB = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        mAppBarLayout = (AppBarLayout) getActivity().findViewById(R.id.app_bar_layout);

        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.simple_grow);
        mFAB.startAnimation(animation);

    }

    private void setupProgressBar() {

        //backward compatible MaterialProgressBar - https://github.com/DreaminginCodeZH/MaterialProgressBar
        IndeterminateProgressDrawable progressDrawable = new IndeterminateProgressDrawable(getActivity());
        progressDrawable.setTint(ContextCompat.getColor(getActivity(), R.color.colorPrimary)); //Change Color
        mProgressBar.setIndeterminateDrawable(progressDrawable);

    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView");

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new WifiListAdapter(getActivity(), true, this);
        mRecyclerView.setAdapter(mAdapter);

        //Setup ItemTouchHelper
        ItemTouchHelper.Callback mTouchHelperCallback = new MyTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        //Setup OnItemTouchListener
        mRecyclerTouchListener = new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
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

                if (mActionMode != null || mSortModeOn) {
                    return;
                }
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
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

        mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);


        //Setup Scroll Listener
        mRecyclerView.addOnScrollListener(new RecyclerScrollListener() {
            @Override
            public void show() {
                mFAB.animate().translationY(0).start();
            }

            @Override
            public void hide() {
                mFAB.animate().translationY(mFAB.getHeight() * 2).start();
            }
        });
    }

    //Setup Context Action Mode
    public void setupActionModeCallback() {
        Log.d(TAG, "setupActionModeCallback");
        mActionModeCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater menuInflater = mode.getMenuInflater();
                menuInflater.inflate(R.menu.menu_context, menu);

                //Fix for CAB forcing icons into overflow menu - doing this via XML doesnt work.
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                mActionModeOn = true;

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                hideFAB(true);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d(TAG, "onActionItemClicked");
                final ArrayList<WifiEntry> selectedEntries = new ArrayList<>();
                final ArrayList<Integer> selectedItems = mAdapter.getSelectedItems();

                if (selectedItems.size() == 0) {
                    return false;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedEntries.add(mListWifi.get(selectedItems.get(i)));
                }

                switch (item.getItemId()) {

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

                            Snackbar.make(mRoot,
                                    selectedItems.size() > 1 ? R.string.snackbar_wifi_archive_multiple
                                            : R.string.snackbar_wifi_archive, Snackbar.LENGTH_LONG)
                                    .setAction(R.string.snackbar_undo, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            for (int i = 0; i < selectedItems.size(); i++) {
                                                mAdapter.addItem(selectedItems.get(i), selectedEntries.get(i));

                                            }
//                                            mRecyclerView.smoothScrollToPosition(selectedItems.get(0));
                                            db.deleteWifiEntries(selectedEntries, true);
                                        }
                                    })
                                    .show();
                        }
                        MyApplication.closeDatabase();
                        return true;

                    case R.id.menu_context_copy:
                        StringBuilder textToCopy = new StringBuilder();

                        for (WifiEntry entry : selectedEntries) {
                            textToCopy.append("Wifi Name: " + entry.getTitle() + "\n"
                                    + "Password: " + entry.getPassword() + "\n\n");
                        }

                        copyToClipboard(COPIED_WIFI_ENTRY, textToCopy.toString(), getString(R.string.snackbar_wifi_copy));
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
                mActionModeOn = false;
                mActionMode = null;
            }
        };

    }

    //Setup SearchView
    private void setupSearch(final Menu menu, MenuItem searchItem) {
        Log.d(TAG, "setupSearch");
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            //on Search - Collapse Toolbar & disable expanding, hide title;
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.d(TAG, "Search - onMenuItemActionExpand");
                searchMode(menu, true);
                //Save full list to restore once search is over (Live Search changes list)
                mSearchSavedList = new ArrayList<>(mListWifi);

                return true;
            }

            //on Close - Expand Toolbar & enable expanding, show title;
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.d(TAG, "Search - onMenuItemActionCollapse");
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
        Log.d(TAG, "setupFAB");
        mFAB.setImageResource(R.drawable.ic_action_add);
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddWifiDialog();
            }
        });

    }


    /********************************************************/
    /******************** Dialog Methods ********************/
    /********************************************************/

    @Override
    public void onSubmitAddDialog(String title, String password) {
        Log.d(TAG, "onSubmitAddDialog");
        WifiEntry entry = new WifiEntry(title, password);
        mAdapter.addItem(0, entry);
        mRecyclerView.smoothScrollToPosition(0);
        addToDatabase(entry);
    }

    private void addToDatabase(WifiEntry entry) {
        Log.d(TAG, "addToDatabase");
        PasswordDB db = MyApplication.getWritableDatabase();

        ArrayList<WifiEntry> entries = new ArrayList<>();
        entries.add(entry);

        db.insertWifiEntries(entries, true, false);
        MyApplication.closeDatabase();
    }

    private void showAddWifiDialog() {
        Log.d(TAG, "showAddWifiDialog");

        InputDialogFragment fragment = InputDialogFragment
                .getInstance(InputDialogFragment.INPUT_ENTRY, null);
        fragment.setTargetFragment(this, RequestCodes.DIALOG_ADD_CODE);
        fragment.show(getFragmentManager(), getString(R.string.dialog_add_key));

    }

    @Override
    public void onSubmitTagDialog(String tag, ArrayList<WifiEntry> listWifi, ArrayList<Integer> positions) {
        Log.d(TAG, "onSubmitTagDialog() called with: " + "tag = [" + tag + "]");

        for (int i = 0; i < positions.size(); i++) {

            mListWifi.get(positions.get(i)).setTag(tag);
        }

        mActionMode.finish();
        mAdapter.notifyDataSetChanged();
        MyApplication.getWritableDatabase().updateWifiTags(listWifi, tag);
        MyApplication.closeDatabase();
    }

    private void showReloadWarningDialog() {
        Log.d(TAG, "showReloadWarningDialog");

        String title = getString(R.string.dialog_warning_reset_title);
        String message = getString(R.string.dialog_warning_reset_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_warning_reset_buttons);

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.getInstance(title, message, buttons);
        dialog.setTargetFragment(this, RequestCodes.DIALOG_WARNING_CODE);
        dialog.show(getFragmentManager(), getString(R.string.dialog_warning_reset_key));

    }


    private void showShareDialog(final ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "showShareDialog");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mShowShareDialog = sharedPreferences.getBoolean(MyApplication.SHARE_WARNING, true);

        if (mShowShareDialog) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View shareDialogLayout = inflater.inflate(R.layout.dialog_share_warning, null);
            final CheckBox dontShowShareDialog = (CheckBox) shareDialogLayout.findViewById(R.id.dont_show_checkbox);

            String[] buttons = getResources().getStringArray(R.array.dialog_warning_share_buttons);

            builder.setView(shareDialogLayout)
                    .setPositiveButton(buttons[0], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (dontShowShareDialog.isChecked()) {
                                Log.d(TAG, "Don't Show Again is Checked");
                                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                sharedPreferences.edit().putBoolean(MyApplication.SHARE_WARNING, false).apply();
                            }

                            shareWifiList(listWifi);
                        }
                    })
                    .setNegativeButton(buttons[1], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //Dismiss
                        }
                    });

            builder.create().show();

        } else {
            shareWifiList(listWifi);
        }
    }


    //TaskLoadWifiEntries creating PathError Dialog.
    @Override
    public void showPathErrorDialog() {
        Log.d(TAG, "showPathErrorDialog");

        String title = getString(R.string.dialog_error_path_title);
        String message = getString(R.string.dialog_error_path_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_path_buttons);

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.getInstance(title, message, buttons);
        dialog.setTargetFragment(this, RequestCodes.DIALOG_ERROR_CODE);
        dialog.show(getFragmentManager(), getString(R.string.dialog_error_path_key));
    }

    public void showRootErrorDialog() {
        Log.d(TAG, "showRootErrorDialog");

        String title = getString(R.string.dialog_error_root_title);
        String message = getString(R.string.dialog_error_root_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_root_button);

        CustomAlertDialogFragment dialog = CustomAlertDialogFragment.getInstance(title, message, buttons);
        dialog.setTargetFragment(this, 0);
        try {
            dialog.show(getFragmentManager(), getString(R.string.dialog_error_root_key));

        } catch (IllegalStateException e) {

            Log.e(TAG, "showRootErrorDialog ERROR: " + e.getClass().getName());
            Toast.makeText(getActivity(), R.string.dialog_error_root_title, Toast.LENGTH_LONG).show();
        }

        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putBoolean(ROOT_ACCESS, false).apply();

        //Restore Menu functions
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
        Log.d("SEARCH", "onQueryTextSubmit");
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        Log.d("SEARCH", "onQueryTextChange");

        //filter logic
        final ArrayList<WifiEntry> filteredWifiList = filter(mSearchSavedList, query);
        mAdapter.animateTo(filteredWifiList);
        mRecyclerView.smoothScrollToPosition(0);
        return true;
    }

    public ArrayList<WifiEntry> filter(ArrayList<WifiEntry> listWifi, String query) {
        Log.d("SEARCH", "filter");
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
