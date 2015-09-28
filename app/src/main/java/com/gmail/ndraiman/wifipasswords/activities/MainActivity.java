package com.gmail.ndraiman.wifipasswords.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.fragments.MainWifiFragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private MainWifiFragment mainWifiFragment;
    private static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";
    private static final String TAG = "MainActivity";
    private FloatingActionButton mFAB;


    //TODO Implement "Hidden" table.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set Activity Transition - Lollipop+
        if(Build.VERSION.SDK_INT >= 21) {
            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition transition = transitionInflater.inflateTransition(R.transition.activity_slide_out);
            getWindow().setExitTransition(transition);

        }

        setContentView(R.layout.activity_main);

        //Set default values for preferences - false = runs only once!!
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        //Transparent Background for CollapsingToolbar Parallax
        mToolbar.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        setSupportActionBar(mToolbar);

        if(savedInstanceState == null) {
            mainWifiFragment = MainWifiFragment.newInstance();

        } else {
            mainWifiFragment = (MainWifiFragment) getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
        }

        getSupportFragmentManager().beginTransaction().replace
                (R.id.content_frame, mainWifiFragment, MAIN_FRAGMENT_TAG).commit();

    }

    @Override
    public void onBackPressed() {
        if(mainWifiFragment.isVisible() && mainWifiFragment.getSortModeStatus()) {
            mainWifiFragment.sortMode(false);
            return;
        }
        super.onBackPressed();
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

        //noinspection SimplifiableIfStatement
        switch (id) {

            case R.id.action_settings:
                //Start Settings with Transition
                ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
                startActivity(new Intent(this, SettingsActivity.class), compat.toBundle());
                return true;

            case R.id.action_help:
                //TODO implement "Help & Feedback" fragment
                return true;

        }

        return super.onOptionsItemSelected(item);
    }


    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Resources resources = getResources();

        //Handles returning from SettingsActivity after Error in Path
        if (requestCode == resources.getInteger(R.integer.settings_activity_code)) {

            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Return from Settings - Loading from file");
                if(mainWifiFragment.isVisible())
                    mainWifiFragment.loadFromFile();
            } else {
                Log.d(TAG, "Return from Settings - didn't change anything");
            }
        }
    }

}
