package com.gmail.ndraiman.wifipasswords.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.dialogs.HelpDialogFragment;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.extras.RequestCodes;
import com.gmail.ndraiman.wifipasswords.fragments.WifiListFragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private WifiListFragment mWifiListFragment;
    private static final String TAG = "MainActivity";
    private ActivityOptionsCompat mCompat;

    public static final String WIFI_LIST_FRAGMENT_TAG = "main_fragment_tag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //Set Activity Transition - Lollipop+
        if (Build.VERSION.SDK_INT >= 21) {
            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition transition = transitionInflater.inflateTransition(R.transition.activity_slide_left);
            getWindow().setExitTransition(transition);

        }

        setContentView(R.layout.activity_main);

        //Set default values for preferences - false = runs only once!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        //Transparent Background for CollapsingToolbar Parallax
        mToolbar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        setSupportActionBar(mToolbar);

        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState = null");
            mWifiListFragment = WifiListFragment.newInstance();

        } else {
            mWifiListFragment = (WifiListFragment) getSupportFragmentManager().findFragmentByTag(WIFI_LIST_FRAGMENT_TAG);
        }

        getSupportFragmentManager().beginTransaction().replace
                (R.id.content_frame, mWifiListFragment, WIFI_LIST_FRAGMENT_TAG).commit();

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
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
        Log.d(TAG, "onCreateOptionsMenu");
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

        //noinspection SimplifiableIfStatement
        switch (id) {

            case R.id.action_hidden_list:
                mCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
//                mCompat = ActivityOptionsCompat.makeCustomAnimation(this,R.anim.right_in, R.anim.left_out);
                startActivityForResult(new Intent(this, ArchiveActivity.class), RequestCodes.ACTIVITY_HIDDEN_CODE, mCompat.toBundle());
                return true;

            case R.id.action_settings:
                //Start Settings with Transition
//                mCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
                mCompat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.right_in, R.anim.left_out);
                startActivityForResult(new Intent(this, SettingsActivity.class), RequestCodes.RESET_TO_DEFAULT, mCompat.toBundle());
                return true;

            case R.id.action_help:
                HelpDialogFragment dialog = HelpDialogFragment.getInstance();
                dialog.show(getFragmentManager(), getString(R.string.dialog_about_key));

                return true;

            case R.id.show_tables:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                PasswordDB db = MyApplication.getWritableDatabase();

                builder.setMessage(db.printTable(PasswordDB.PasswordHelper.TABLE_MAIN) + "\n\n"
                        + db.printTable(PasswordDB.PasswordHelper.TABLE_ARCHIVE) + "\n\n"
                        + db.printTable(PasswordDB.PasswordHelper.TABLE_DELETED));

                MyApplication.closeDatabase();

                builder.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });
                builder.create().show();
                break;

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult - requestCode = " + requestCode + ", resultCode = " + resultCode);

        switch (requestCode) {

            case RequestCodes.ACTIVITY_INTRO_CODE:
                Log.d(TAG, "Returning From IntroApp - resultCode = " + resultCode);
                mWifiListFragment.onActivityResult(requestCode, resultCode, data);
                break;

            case RequestCodes.ACTIVITY_SETTINGS_CODE: //Handles returning from SettingsActivity after Error in Path

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Return from Settings - Loading from file");
                    if (mWifiListFragment.isVisible())
                        mWifiListFragment.loadFromFile(true);
                } else {
                    Log.d(TAG, "Return from Settings - didn't change anything");
                }
                break;

            case RequestCodes.ACTIVITY_HIDDEN_CODE: //return from ArchiveActivity
                Log.d(TAG, "Return from ArchiveActivity - resultCode = " + resultCode);
                mWifiListFragment.onActivityResult(requestCode, resultCode, data);
                break;

            case RequestCodes.RESET_TO_DEFAULT: //Return from Settings Activity - Reset to Default
                if (resultCode == RequestCodes.RESET_TO_DEFAULT) {
                    Log.d(TAG, "Return from Settings - Reset to Default");
                    mWifiListFragment.loadFromFile(true);

                }
                break;
        }
    }

}
