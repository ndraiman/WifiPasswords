package com.gmail.ndraiman.wifipasswords.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.dialogs.HelpDialogFragment;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.extras.RequestCodes;
import com.gmail.ndraiman.wifipasswords.fragments.WifiListFragment;
import com.gmail.ndraiman.wifipasswords.task.TaskCheckPasscode;

public class MainActivity extends AppCompatActivity {

    private WifiListFragment mWifiListFragment;

    public static final String WIFI_LIST_FRAGMENT_TAG = "main_fragment_tag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set Activity Transition - Lollipop+
        if (Build.VERSION.SDK_INT >= 21) {
            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition transition = transitionInflater.inflateTransition(R.transition.activity_slide_left);
            getWindow().setExitTransition(transition);

        }

        setContentView(R.layout.activity_main);

        //Set default values for preferences - false = runs only once!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {

            mWifiListFragment = WifiListFragment.newInstance();

        } else {
            mWifiListFragment = (WifiListFragment) getSupportFragmentManager().findFragmentByTag(WIFI_LIST_FRAGMENT_TAG);
        }

        getSupportFragmentManager().beginTransaction().replace
                (R.id.content_frame, mWifiListFragment, WIFI_LIST_FRAGMENT_TAG).commit();

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

        if(MyApplication.mPasscodeActivated && !isFinishing()) {

            new TaskCheckPasscode(getApplicationContext()).execute();

        } else if (isFinishing()) {

            MyApplication.mAppWentBackground = true;
        }
    }

    @Override
    public void onBackPressed() {

        if (mWifiListFragment.isVisible() && mWifiListFragment.getSortModeStatus()) {
            mWifiListFragment.sortMode(false);
            return;
        }

        //Dialog to confirm Exit

        String[] buttons = getResources().getStringArray(R.array.dialog_exit_buttons);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle(R.string.dialog_exit_title)
                .setMessage(R.string.dialog_exit_message)
                .setPositiveButton(buttons[0], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        overridePendingTransition(R.anim.activity_slide_in_up, R.anim.activity_slide_out_down);
                    }
                })
                .setNegativeButton(buttons[1], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Dismiss Dialog
                    }
                });


        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        ActivityOptionsCompat compat;

        //noinspection SimplifiableIfStatement
        switch (id) {

            case R.id.action_hidden_list:
                compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
                startActivityForResult(new Intent(this, ArchiveActivity.class), RequestCodes.ACTIVITY_ARCHIVE_CODE, compat.toBundle());
                return true;

            case R.id.action_settings:
                //Start Settings with Transition
                compat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.right_in, R.anim.left_out);
                startActivityForResult(new Intent(this, SettingsActivity.class), RequestCodes.RESET_TO_DEFAULT, compat.toBundle());
                return true;

            case R.id.action_help:
                HelpDialogFragment dialog = HelpDialogFragment.getInstance();
                dialog.show(getFragmentManager(), getString(R.string.dialog_about_key));

                return true;

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {

            case RequestCodes.ACTIVITY_INTRO_CODE:

                mWifiListFragment.onActivityResult(requestCode, resultCode, data);
                break;


            case RequestCodes.ACTIVITY_SETTINGS_CODE: //Handles returning from SettingsActivity after Error in Path

                if (resultCode == Activity.RESULT_OK) {

                    if (mWifiListFragment.isVisible())
                        mWifiListFragment.loadFromFile(true);
                }
                break;


            case RequestCodes.ACTIVITY_ARCHIVE_CODE: //return from ArchiveActivity

                mWifiListFragment.onActivityResult(requestCode, resultCode, data);
                break;


            case RequestCodes.RESET_TO_DEFAULT: //Return from Settings Activity - Reset to Default
                if (resultCode == RequestCodes.RESET_TO_DEFAULT) {

                    mWifiListFragment.loadFromFile(true);

                }
                break;
        }
    }

}
