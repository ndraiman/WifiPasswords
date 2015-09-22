package com.gmail.ndraiman.wifipasswords.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.activities.MainActivity;
import com.gmail.ndraiman.wifipasswords.activities.SettingsActivity;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.extras.L;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.CustomItemTouchHelper;
import com.gmail.ndraiman.wifipasswords.recycler.ItemDragListener;
import com.gmail.ndraiman.wifipasswords.recycler.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListAdapter;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListLoadedListener;
import com.gmail.ndraiman.wifipasswords.task.TaskLoadWifiEntries;

import java.util.ArrayList;

import me.zhanghai.android.materialprogressbar.IndeterminateProgressDrawable;


public class MainWifiFragment extends Fragment implements WifiListLoadedListener,
        SearchView.OnQueryTextListener, CustomAlertDialogListener, InputDialogListener,
        ItemDragListener {

    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries"; //Parcel key
    private static final String COPIED_WIFI_ENTRY = "copied_wifi_entry"; //Clipboard Label
    private RecyclerView mRecyclerView;
    private WifiListAdapter mAdapter;
    private ArrayList<WifiEntry> mListWifi;
    private ProgressBar mProgressBar;
    private String mPath;
    private String mFileName;
    private AppBarLayout mAppBarLayout;
    private FloatingActionButton mFAB;
    private SearchView mSearchView;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private ItemTouchHelper mItemTouchHelper;
    private Menu mMenu;
    private boolean mSortModeOn = false;

    public static TextView textNoRoot;


    public static MainWifiFragment newInstance() {
        MainWifiFragment fragment = new MainWifiFragment();
        Bundle args = new Bundle();
        //put any extra arguments that you may want to supply to this fragment
        fragment.setArguments(args);
        return fragment;
    }


    public MainWifiFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View layout = inflater.inflate(R.layout.fragment_main_wifi, container, false);
        setHasOptionsMenu(true);

        //Init local Views
        mListWifi = new ArrayList<>();
        textNoRoot = (TextView) layout.findViewById(R.id.text_no_root);
        mRecyclerView = (RecyclerView) layout.findViewById(R.id.main_wifi_list_recycler);
        mProgressBar = (ProgressBar) layout.findViewById(R.id.progress_bar);

        //get Activity Views
        mFAB = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        mCollapsingToolbarLayout = (CollapsingToolbarLayout) getActivity().findViewById(R.id.collapsing_layout);
        mAppBarLayout = (AppBarLayout) getActivity().findViewById(R.id.app_bar_layout);


        //backward compatible MaterialProgressBar - https://github.com/DreaminginCodeZH/MaterialProgressBar
        IndeterminateProgressDrawable progressDrawable = new IndeterminateProgressDrawable(getActivity());
        progressDrawable.setTint(ContextCompat.getColor(getActivity(), R.color.colorPrimary)); //Change Color
        mProgressBar.setIndeterminateDrawable(progressDrawable);

        //Setup RecyclerView & Adapter
        setupRecyclerView();

        //Setup Floating Action Button
        setupFAB();

        //Determine if Activity runs for first time
        if (savedInstanceState != null) {

            L.m("extracting mListWifi from Parcelable");
            //if starts after a rotation or configuration change, load the existing Wifi list from a parcelable
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);

        } else {
            //if starts for the first time, load the list of wifi from a database
            mListWifi = MyApplication.getWritableDatabase().getAllWifiEntries(false);
            MyApplication.closeDatabase();
            //if the database is empty, trigger an AsyncTask to get wifi list from the wpa_supplicant
            if (mListWifi.isEmpty()) {

                loadFromFile();
                L.m("executing task from onCreate");
            }
        }

        mAdapter.setWifiList(mListWifi);

        return layout;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the wifi list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_WIFI_ENTRIES, mListWifi);
    }


    //WifiListLoadedListener method - called from TaskLoadWifiEntries
    @Override
    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi) {

        //Hide Progress Bar
        if (mProgressBar.getVisibility() == View.VISIBLE) {
            mProgressBar.setVisibility(View.GONE);
        }

        L.m("onWifiListLoaded");
        mListWifi = listWifi;
        mAdapter.setWifiList(listWifi);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_wifi_main_fragment, menu);

        mMenu = menu;

        MenuItem searchItem = menu.findItem(R.id.action_search);

        setupSearch(searchItem);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_reload_from_file) {
            showReloadWarningDialog();
        }

        else if (id == R.id.action_sort_start) {
            sortMode(true);

        }

        else if (id == R.id.action_sort_done) {
            sortMode(false);

        }

        else if (id == R.id.action_share) {
            shareWifiList();
        }

        return true;
    }



    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/

    //Share entire wifi list
    private void shareWifiList() {

        String textToShare = "";

        for (int i = 0; i < mListWifi.size(); i++) {

            WifiEntry current = mListWifi.get(i);

            textToShare += "Wifi Name: " + current.getTitle() + "\n"
                    + "Password: " + current.getPassword() + "\n\n";
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);

    }


    //Copy wpa_supplicant and extract data from it via AsyncTask
    public void loadFromFile() {

        getPath();

        mAdapter.setWifiList(new ArrayList<WifiEntry>());
        mProgressBar.setVisibility(View.VISIBLE); //Show Progress Bar
        L.m("loadFromFile");
        MainActivity.makeSnackbar("Loading Data From File");
        new TaskLoadWifiEntries(mPath, mFileName, this, this).execute();

    }

    //Copy to Clipboard Method
    private void copyToClipboard(String copiedLabel, String copiedText, String snackbarMessage) {

        ClipboardManager clipboardManager =
                (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = ClipData.newPlainText(copiedLabel, copiedText);
        clipboardManager.setPrimaryClip(clipData);

        MainActivity.makeSnackbar(snackbarMessage);
        L.m("copyToClipboard:\n" + clipData.toString());
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

        L.m("getPath() - path = " + mPath + "\n filename = " + mFileName);

    }

    //Toggle Sort Mode
    public void sortMode(boolean isOn) {
        L.m("sortMode - isOn = " + isOn);
        mAppBarLayout.setExpanded(!isOn);
        mRecyclerView.setNestedScrollingEnabled(!isOn);
        mAdapter.showDragHandler(isOn);
        MenuItem done = mMenu.findItem(R.id.action_sort_done);
        done.setVisible(isOn);

        mSortModeOn = isOn;
    }

    //Return Sort Mode Status - used OnBackPressed in MainActivity
    public boolean getSortModeStatus() {
        return mSortModeOn;
    }


    /********************************************************/
    /******************** Setup Methods *********************/
    /********************************************************/

    private void setupRecyclerView() {

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new WifiListAdapter(getActivity(), this);
        mRecyclerView.setAdapter(mAdapter);

        //Item Touch Helper
        ItemTouchHelper.Callback callback = new CustomItemTouchHelper(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        //Setup RecyclerTouchListener
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Toast.makeText(getActivity(), "onClick " + position, Toast.LENGTH_SHORT).show();  //placeholder
            }

            @Override
            public void onLongClick(View view, int position) {
                L.m("RecyclerView - onLongClick " + position);

                WifiEntry entry = mListWifi.get(position);
                String textToCopy = "Wifi Name: " + entry.getTitle() + "\n"
                        + "Password: " + entry.getPassword();
                String snackbarMessage = getResources().getString(R.string.snackbar_wifi_copy);

                copyToClipboard(COPIED_WIFI_ENTRY, textToCopy, snackbarMessage);
            }
        }));
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        L.m("onStartDrag");
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void setupSearch(MenuItem searchItem) {

        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            //on Search - Collapse Toolbar & disable expanding, hide title;
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                L.m("Search - onMenuItemActionExpand");
                mAppBarLayout.setExpanded(false);
                mCollapsingToolbarLayout.setCollapsedTitleTextColor(Color.TRANSPARENT);
                mRecyclerView.setNestedScrollingEnabled(false);

                return true;
            }

            //on Close - Expand Toolbar & enable expanding, show title;
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                L.m("Search - onMenuItemActionCollapse");
                mRecyclerView.setNestedScrollingEnabled(true);
                mCollapsingToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
                mAppBarLayout.setExpanded(true);
                return true;
            }
        });

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setOnQueryTextListener(this);
    }


    private void setupFAB() {

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
    public void onSubmitInputDialog(String title, String password) {
        L.m("onSubmitInputDialog");
        WifiEntry entry = new WifiEntry(title, password);
        mAdapter.addItem(0, entry);
        mRecyclerView.scrollToPosition(0);
        addToDatabase(entry, false);
    }

    private void addToDatabase(WifiEntry entry, boolean isHidden) {
        L.m("addToDatabase");
        PasswordDB db = MyApplication.getWritableDatabase();
        db.insertEntry(entry, isHidden);
        MyApplication.closeDatabase();
    }

    private void showAddWifiDialog() {
        L.m("showAddWifiDialog");

        InputDialogFragment fragment = InputDialogFragment.getInstance();
        fragment.setTargetFragment(this, R.integer.dialog_add_code);
        fragment.show(getFragmentManager(), getString(R.string.dialog_add_tag));

    }

    private void showReloadWarningDialog() {
        L.m("showReloadWarningDialog");

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
        L.m("showPathErrorDialog");

        String title = getString(R.string.dialog_error_title);
        String message = getString(R.string.dialog_error_message);
        String[] buttons = getResources().getStringArray(R.array.dialog_error_buttons);

        CustomAlertDialogFragment fragment = CustomAlertDialogFragment.getInstance(title, message, buttons);
        fragment.setTargetFragment(this, R.integer.dialog_error_code);
        fragment.show(getFragmentManager(), getString(R.string.dialog_error_tag));
    }

    //Handle Dialog Result Codes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Resources resources = getResources();

        //Handle Path Error Dialog
        if (requestCode == R.integer.dialog_error_code) {

            if (resultCode == R.integer.dialog_confirm) {
                L.m("Dialog Error - Confirm");
                FragmentActivity parent = getActivity();
                ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(parent, null);
                parent.startActivityForResult(
                        new Intent(parent, SettingsActivity.class),
                        R.integer.settings_activity_code,
                        compat.toBundle());

            } //Else Dismissed
        }

        //Handle LoadFromFile Warning Dialog
        if (requestCode == R.integer.dialog_warning_code) {

            if (resultCode == R.integer.dialog_confirm) {
                L.m("Dialog Warning - Confirm");
                loadFromFile();

            } //Else Dismissed
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
        final ArrayList<WifiEntry> filteredWifiList = filter(mListWifi, query);
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
