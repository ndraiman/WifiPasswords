package com.gmail.ndraiman.wifipasswords.activities;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.AppCompatPreferenceActivity;

import java.util.List;


public class SettingsActivity extends AppCompatPreferenceActivity {

    private Toolbar mToolbar;
    private static final String LOG_TAG = "SettingsActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set Activity Transition - Lollipop+
        if(Build.VERSION.SDK_INT >= 21) {

            TransitionInflater transitionInflater = TransitionInflater.from(this);
            Transition slide_in = transitionInflater.inflateTransition(R.transition.activity_slide_in);
            Transition slide_out = transitionInflater.inflateTransition(R.transition.activity_slide_out);

            getWindow().setEnterTransition(slide_in);
            getWindow().setExitTransition(slide_out);

        }

        setContentView(R.layout.activity_settings);

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out)
                .replace(R.id.settings_frame, new SettingsFragment()).commit();

    }


    @Override
    public void onBuildHeaders(List<Header> target) {
        Log.d(LOG_TAG, "SettingsActivity onBuildHeaders");

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



    /***************************************************************/
    /****************** Settings Fragment **************************/
    /***************************************************************/
    public static class SettingsFragment extends PreferenceFragment {

        private static final String LOG_TAG = "SettingsFragment";

        private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
                = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                getActivity().setResult(RESULT_OK);

                String stringValue = newValue.toString();
                Log.d(LOG_TAG, "onPreferenceChange: " + stringValue);

                if (preference instanceof EditTextPreference) {

                    preference.setSummary(stringValue);

                }

                if (preference instanceof ListPreference) {

                    preference.setSummary(stringValue);

                    //Toggle Manual location entry according to List Choice
                    if(stringValue.equals(getString(R.string.pref_path_list_manual))) {

                        findPreference(getString(R.string.pref_path_manual_key)).setEnabled(true);
                        findPreference(getString(R.string.pref_reset_key)).setEnabled(true);
                    } else {
                        findPreference(getString(R.string.pref_path_manual_key)).setEnabled(false);
                        findPreference(getString(R.string.pref_reset_key)).setEnabled(false);
                    }

                }

                return true;
            }
        };

        private void bindPreferenceSummaryToValue(Preference preference) {
            Log.d(LOG_TAG, "bindPreferenceSummaryToValue");
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(LOG_TAG, "SettingsFragment - onCreate");

            getActivity().setTheme(R.style.AppTheme); //TODO theme according to preference
            setHasOptionsMenu(true);

            getActivity().setResult(RESULT_CANCELED);

            loadPreferences();
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
                Log.d(LOG_TAG, "SettingsFragment onCreateView");
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

        //Helper method for onCreate
        private void loadPreferences() {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            findPreference(getString(R.string.pref_reset_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    resetPathPref();
                    return true;
                }
            });


            //Summary to Value
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_path_manual_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_path_list_key)));

        }
        

        //Restore wpa_supplicant path to default
        private void resetPathPref() {
            Log.d(LOG_TAG, "resetPathPref");

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.pref_path_manual_key), getString(R.string.pref_path_default));
            editor.apply();


            findPreference(getString(R.string.pref_path_manual_key)).setSummary(getString(R.string.pref_path_default));

            //Refresh Preference Screen
            setPreferenceScreen(null);
            loadPreferences();

        }

    }
}
