package com.gmail.ndraiman.wifipasswords.activities;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.L;

/**
 * Created by ND88 on 17/09/2015.
 */
public class SettingsActivity extends PreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //TODO add toolbar

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();

    }

    //Required Method to Override to Validated Fragments
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    //Setup Listener
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
            = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            String stringValue = newValue.toString();
            L.m("onPreferenceChange: " + stringValue);

            if(preference instanceof EditTextPreference) {

                preference.setSummary(stringValue);

            }

            return true;
        }
    };


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

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            //Summary to Value
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_path_key)));
        }


    }
}
