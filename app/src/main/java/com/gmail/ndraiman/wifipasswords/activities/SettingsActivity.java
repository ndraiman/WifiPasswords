package com.gmail.ndraiman.wifipasswords.activities;

import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.AppCompatPreferenceActivity;
import com.gmail.ndraiman.wifipasswords.extras.L;

import java.util.List;


public class SettingsActivity extends AppCompatPreferenceActivity {

    private Toolbar mToolbar;
    //Setup Listener
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            String stringValue = newValue.toString();
            L.m("onPreferenceChange: " + stringValue);

            if (preference instanceof EditTextPreference) {

                preference.setSummary(stringValue);

            }

            return true;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set Activity Transition - Lollipop+
        if(Build.VERSION.SDK_INT >= 21) {

            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition slide_in = transitionInflater.inflateTransition(R.transition.activity_slide_in);
            Transition slide_out = transitionInflater.inflateTransition(R.transition.activity_slide_out);

            getWindow().setEnterTransition(slide_in);
            getWindow().setReturnTransition(slide_out);

        }

        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out)
                .replace(R.id.settings_frame, new SettingsFragment()).commit();

    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        L.m("SettingsActivity onBuildHeaders");

        //loadHeadersFromResource(R.xml.pref_header, target);

        setContentView(R.layout.fragment_settings);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

    }

    //Required Method to Override to Validated Fragments
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_help) {
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        L.m("bindPreferenceSummaryToValue");
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    /***************************************************************/
    /****************** Settings Fragment **************************/
    /***************************************************************/
    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            L.m("SettingsFragment - onCreate");

            getActivity().setTheme(R.style.AppTheme); //TODO theme according to preference
            setHasOptionsMenu(true);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            //Summary to Value
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_path_key)));

        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.menu_settings, menu);
        }


        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            if(Build.VERSION.SDK_INT >= 21)
                setEnterTransition(TransitionInflater.from(getActivity()).inflateTransition(R.transition.activity_slide_in));

            View layout = inflater.inflate(R.layout.fragment_settings, container, false);

            if (layout != null) {

                //Adding Toolbar to fragment
                L.m("SettingsFragment onCreateView");
                AppCompatPreferenceActivity sActivity = (AppCompatPreferenceActivity) getActivity();
                Toolbar sToolbar = (Toolbar) layout.findViewById(R.id.app_bar);
                sActivity.setSupportActionBar(sToolbar);

                ActionBar sBar = sActivity.getSupportActionBar();
                sBar.setDisplayShowHomeEnabled(true);
                sBar.setDisplayHomeAsUpEnabled(true);
                sBar.setDisplayShowTitleEnabled(true);

                sBar.setTitle(getResources().getString(R.string.activity_title_settings));

            }


            return layout;
        }
    }
}
