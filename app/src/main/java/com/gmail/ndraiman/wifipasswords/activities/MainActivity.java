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
import com.gmail.ndraiman.wifipasswords.fragments.WifiListFragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private WifiListFragment mainWifiFragment;
    private static final String TAG = "MainActivity";
    private ActivityOptionsCompat mCompat;

    public static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";


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
            mainWifiFragment = WifiListFragment.newInstance();

        } else {
            mainWifiFragment = (WifiListFragment) getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
        }

        getSupportFragmentManager().beginTransaction().replace
                (R.id.content_frame, mainWifiFragment, MAIN_FRAGMENT_TAG).commit();

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (mainWifiFragment.isVisible() && mainWifiFragment.getSortModeStatus()) {
            mainWifiFragment.sortMode(false);
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
                startActivityForResult(new Intent(this, HiddenWifiActivity.class), R.integer.activity_hidden_code, mCompat.toBundle());
                return true;

            case R.id.action_settings:
                //Start Settings with Transition
//                mCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
                mCompat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.right_in, R.anim.left_out);
                startActivityForResult(new Intent(this, SettingsActivity.class), R.integer.reset_to_default, mCompat.toBundle());
                return true;

            case R.id.action_help:
                //TODO implement "Help & Feedback" fragment
                return true;

        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult");

        switch (requestCode) {

            case R.integer.activity_settings_code: //Handles returning from SettingsActivity after Error in Path

                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Return from Settings - Loading from file");
                    if (mainWifiFragment.isVisible())
                        mainWifiFragment.loadFromFile(true);
                } else {
                    Log.d(TAG, "Return from Settings - didn't change anything");
                }
                break;

            case R.integer.activity_hidden_code: //return from HiddenWifiActivity
                Log.d(TAG, "Return from HiddenWifiActivity - resultCode = " + resultCode);
                mainWifiFragment.onActivityResult(requestCode, resultCode, data);
                break;

            case R.integer.reset_to_default: //Return from Settings Activity - Reset to Default
                if (resultCode == R.integer.reset_to_default) {
                    Log.d(TAG, "Return from Settings - Reset to Default");
                    mainWifiFragment.loadFromFile(true);

                }
                break;
        }
    }

}
