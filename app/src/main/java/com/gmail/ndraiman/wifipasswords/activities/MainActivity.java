package com.gmail.ndraiman.wifipasswords.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.fragments.CustomAlertDialogFragment;
import com.gmail.ndraiman.wifipasswords.fragments.MainWifiFragment;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private static CoordinatorLayout mRoot;


    //TODO Implement "Hidden" table.
    //TODO App Design - Implement Drawer Layout to swap between activities and settings

    //TODO move settings activity to fragment

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

        mRoot = (CoordinatorLayout) findViewById(R.id.coordinator_layout);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, MainWifiFragment.newInstance()).commit();

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
        if (id == R.id.action_settings) {
            //Start Settings with Transition
            ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, null);
            startActivity(new Intent(this, SettingsActivity.class), compat.toBundle());
            return true;
        }

        if (id == R.id.action_help) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
//        L.T(this, "Destroying Database");
//        MyApplication.getWritableDatabase().deleteAll(false);
        super.onDestroy();
    }



    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/


    //Custom Snackbar
    public static void makeSnackbar(String message) {

        Snackbar mSnackbar = Snackbar.make(mRoot, message, Snackbar.LENGTH_SHORT);
        View snackbarView = mSnackbar.getView();

        //snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

        TextView snackbarText = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
        //snackbarText.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
        snackbarText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        mSnackbar.show();

    }

    public void showErrorDialog(String message) {


        CustomAlertDialogFragment dialogFragment = CustomAlertDialogFragment.getInstance(message);
        dialogFragment.show(getSupportFragmentManager(), "DIALOG_TAG");

    }

}
