package com.gmail.ndraiman.wifipasswords.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.activities.MainActivity;
import com.gmail.ndraiman.wifipasswords.activities.SettingsActivity;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.dialogs.CustomAlertDialogFragment;
import com.gmail.ndraiman.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndraiman.wifipasswords.dialogs.InputDialogFragment;
import com.gmail.ndraiman.wifipasswords.dialogs.InputDialogListener;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndraiman.wifipasswords.recycler.MyTouchHelperCallback;
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

    private boolean isSortModeOn = false;

    //OnSavedInstance Keys
    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries"; //Parcel key
    private static final String STATE_ACTION_MODE = "state_action_mode";
    private static final String STATE_ACTION_MODE_SELECTIONS = "state_action_mode_selections";
    private static final String STATE_SORT_MODE = "state_sort_mode";

    //Layout
    private FrameLayout mRoot;
    private AppBarLayout mAppBarLayout;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private FloatingActionButton mFAB;
    private ProgressBar mProgressBar;
    public static TextView textNoRoot;

    //wpa_supplicant file
    private String mPath;
    private String mFileName;
    private boolean isLoadingFromFile = false;

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
    private boolean isActionModeOn = false;
    private boolean mActionModeArchivePressed = false; //Checks if Archive was pressed - will not call clearSelection to preserve animations


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

        //Setup Floating Action Button
        setupFAB();


        if (savedInstanceState != null) {

            Log.d(TAG, "restoring from savedInstanceState");

            mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);
            isActionModeOn = savedInstanceState.getBoolean(STATE_ACTION_MODE);
            mActionModeSelections = savedInstanceState.getIntegerArrayList(STATE_ACTION_MODE_SELECTIONS);
            isSortModeOn = savedInstanceState.getBoolean(STATE_SORT_MODE);

        } else {

            Log.d(TAG, "getting WifiEntries from database");

            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
            MyApplication.closeDatabase();

            if (mListWifi.isEmpty()) {

                loadFromFile(true);
                Log.d(TAG, "executing task from onCreateView");
            }
        }

        mAdapter.setWifiList(mListWifi);

        //Restore Context Action Bar state
        if (isActionModeOn) {
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

        if (isSortModeOn) {
            sortMode(true);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        //Update database with changes made to wifi list.
        PasswordDB db = MyApplication.getWritableDatabase();
        db.deleteAll(false);
        db.insertWifiEntries(mListWifi, false);
        MyApplication.closeDatabase();

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES,
                mSearchView.isIconified() ? mListWifi : mSearchSavedList);

        if (mSearchView != null) {
            mSearchSavedQuery = mSearchView.getQuery().toString();
        }

        outState.putBoolean(STATE_SORT_MODE, isSortModeOn);
        outState.putBoolean(STATE_ACTION_MODE, isActionModeOn);
        outState.putIntegerArrayList(STATE_ACTION_MODE_SELECTIONS, mAdapter.getSelectedItems());
    }


    //WifiListLoadedListener method - called from TaskLoadWifiEntries
    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "onWifiListLoaded");

        //Hide Progress Bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        mListWifi = new ArrayList<>(listWifi);

        mAdapter.setWifiList(mListWifi);

        isLoadingFromFile = false;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.d(TAG, "onPrepareOptionsMenu");

        //Disable\Enable menu items according to SortMode
        menu.setGroupVisible(R.id.menu_group_main, !isSortModeOn && !isLoadingFromFile);
        menu.setGroupVisible(R.id.menu_group_sort_mode, isSortModeOn);

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
                shareWifiList(mListWifi);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");

        switch (requestCode) {

            case R.integer.activity_hidden_code: //Handle HiddenWifiActivity items restored

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "HiddenWifiActivity - Items Restored");
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

            case R.integer.dialog_error_code:  //Handle Path Error Dialog

                if (resultCode == R.integer.dialog_confirm) {
                    Log.d(TAG, "Dialog Error - Confirm");
                    FragmentActivity parent = getActivity();
                    ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(parent, null);
                    parent.startActivityForResult(
                            new Intent(parent, SettingsActivity.class),
                            R.integer.activity_settings_code,
                            compat.toBundle());
                } //Else Dismissed
                break;

            case R.integer.dialog_warning_code: //Handle LoadFromFile Warning Dialog

                if (resultCode == R.integer.dialog_confirm) {
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

        getPath();

        isLoadingFromFile = true;
        getActivity().invalidateOptionsMenu();

//        if (resetDB) {
        mAdapter.setWifiList(new ArrayList<WifiEntry>());
        mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
//        }

        Snackbar.make(mRoot, R.string.snackbar_load_from_file, Snackbar.LENGTH_SHORT).show();
        new TaskLoadWifiEntries(mPath, mFileName, resetDB, this, this).execute();

    }


    //Share entire wifi list
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

        collapseAppBarLayout(isOn);
        mCollapsingToolbarLayout.setCollapsedTitleTextColor(isOn ? Color.TRANSPARENT : Color.WHITE);

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
        collapseAppBarLayout(isOn);
        mAdapter.showDragHandler(isOn);

        //Disable Touch actions while in sort mode
        if (isOn) {
            mRecyclerView.removeOnItemTouchListener(mRecyclerTouchListener);
        } else {
            mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);
        }

        isSortModeOn = isOn;

        getActivity().invalidateOptionsMenu();
    }

    //Return Sort Mode Status - used OnBackPressed in MainActivity
    public boolean getSortModeStatus() {
        return isSortModeOn;
    }


    public void collapseAppBarLayout(boolean collapse) {

        mAppBarLayout.setExpanded(!collapse);
        mRecyclerView.setNestedScrollingEnabled(!collapse);
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
        textNoRoot = (TextView) layout.findViewById(R.id.text_no_root);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.main_wifi_list_recycler);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);
        mRoot = (FrameLayout) layout.findViewById(R.id.fragment_main_container);

        //get Activity Views
        mFAB = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) getActivity().findViewById(R.id.collapsing_layout);
        mAppBarLayout = (AppBarLayout) getActivity().findViewById(R.id.app_bar_layout);

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
        mAdapter = new WifiListAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        //Setup ItemTouchHelper
        ItemTouchHelper.Callback mTouchHelperCallback = new MyTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(mTouchHelperCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        //Setup Context Action Mode
        setupActionModeCallback();

        //Setup OnItemTouchListener
        mRecyclerTouchListener = new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onClick " + position);

                //while in ActionMode - regular clicks will also select items
                if (isActionModeOn) {
                    mAdapter.toggleSelection(position);
                    mRecyclerView.scrollToPosition(position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d(TAG, "RecyclerView - onLongClick " + position);

                //Invoking Context Action Mode
                mAdapter.toggleSelection(position);
                mRecyclerView.scrollToPosition(position);

                if (mActionMode != null || isSortModeOn) {
                    return;
                }
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(mActionModeCallback);
            }
        });

        mRecyclerView.addOnItemTouchListener(mRecyclerTouchListener);
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

                isActionModeOn = true;

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                collapseAppBarLayout(true);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.d(TAG, "onActionItemClicked");
                final ArrayList<WifiEntry> selectedEntries = new ArrayList<>();
                final ArrayList<Integer> selectedItems = mAdapter.getSelectedItems();

                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedEntries.add(mListWifi.get(selectedItems.get(i)));
                }

                switch (item.getItemId()) {
                    case R.id.menu_context_archive:
                        mActionModeArchivePressed = true;

                        for (int i = selectedItems.size() - 1; i >= 0; i--) {
                            //Starting removal from end of list so Indexes wont change when item is removed
                            mAdapter.removeItem(selectedItems.get(i));
                        }

                        final PasswordDB db = MyApplication.getWritableDatabase();
                        db.insertWifiEntries(selectedEntries, true);

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
                                            mRecyclerView.scrollToPosition(selectedItems.get(0));
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

                        shareWifiList(selectedEntries);
                        return true;

                    case R.id.menu_context_tag:
                        //TODO EditText Dialog to write Tag
                        Bundle bundle = new Bundle();
                        bundle.putParcelableArrayList(InputDialogFragment.ENTRIES_KEY, selectedEntries);
                        bundle.putIntegerArrayList(InputDialogFragment.POSITIONS_LEY, selectedItems);
                        InputDialogFragment fragment = InputDialogFragment.getInstance(InputDialogFragment.INPUT_TAG, bundle);
                        fragment.setTargetFragment(getFragmentManager().findFragmentByTag(MainActivity.MAIN_FRAGMENT_TAG), R.integer.dialog_tag_code);
                        fragment.show(getFragmentManager(), getString(R.string.dialog_add_key));

                        return true;


                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

                if (!mActionModeArchivePressed) {
                    mAdapter.clearSelection();
                }
                mActionModeArchivePressed = false;
                mRecyclerView.setNestedScrollingEnabled(true);
                isActionModeOn = false;
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
        mFAB.setImageResource(R.drawable.ic_add_light);
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
        mRecyclerView.scrollToPosition(0);
        addToDatabase(entry);
    }

    private void addToDatabase(WifiEntry entry) {
        Log.d(TAG, "addToDatabase");
        PasswordDB db = MyApplication.getWritableDatabase();

        ArrayList<WifiEntry> entries = new ArrayList<>();
        entries.add(entry);

        db.insertWifiEntries(entries, false);
        MyApplication.closeDatabase();
    }

    private void showAddWifiDialog() {
        Log.d(TAG, "showAddWifiDialog");

        InputDialogFragment fragment = InputDialogFragment
                .getInstance(InputDialogFragment.INPUT_ENTRY, null);
        fragment.setTargetFragment(this, R.integer.dialog_add_code);
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
        MyApplication.getWritableDatabase().insertWifiTags(listWifi, tag);
        MyApplication.closeDatabase();
    }

    private void showReloadWarningDialog() {
        Log.d(TAG, "showReloadWarningDialog");

        String title = getString(R.string.dialog_warning_title);
        String message = getString(R.string.dialog_warning_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_warning_buttons);

        CustomAlertDialogFragment fragment = CustomAlertDialogFragment.getInstance(title, message, buttons);
        fragment.setTargetFragment(this, R.integer.dialog_warning_code);
        fragment.show(getFragmentManager(), getString(R.string.dialog_warning_tag));

    }

    //TaskLoadWifiEntries creating PathError Dialog.
    @Override
    public void showPathErrorDialog() {
        Log.d(TAG, "showPathErrorDialog");

        String title = getString(R.string.dialog_error_title);
        String message = getString(R.string.dialog_error_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_buttons);

        CustomAlertDialogFragment fragment = CustomAlertDialogFragment.getInstance(title, message, buttons);
        fragment.setTargetFragment(this, R.integer.dialog_error_code);
        fragment.show(getFragmentManager(), getString(R.string.dialog_error_tag));
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
        mRecyclerView.scrollToPosition(0);
        return true;
    }

    public ArrayList<WifiEntry> filter(ArrayList<WifiEntry> listWifi, String query) {
        Log.d("SEARCH", "filter");
        query = query.toLowerCase();

        final ArrayList<WifiEntry> filteredWifiList = new ArrayList<>();

        for (WifiEntry entry : listWifi) {
            final String title = entry.getTitle().toLowerCase();
            if (title.contains(query)) {
                filteredWifiList.add(entry);
            }
        }

        return filteredWifiList;
    }

}
