package com.gmail.ndrdevelop.wifipasswords.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.dialogs.AboutDialogFragment;
import com.gmail.ndrdevelop.wifipasswords.extras.AppCompatPreferenceActivity;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RequestCodes;
import com.gmail.ndrdevelop.wifipasswords.task.TaskCheckPasscode;


public class SettingsActivity extends AppCompatPreferenceActivity {

    static Toolbar mToolbar;
    SettingsFragment mSettingsFragment;

    static final String SETTINGS_FRAGMENT_TAG = "settings_fragment_tag";

    static boolean mPasscodePrefs = false;
    final String PASSCODE_PREFS = "passcode_preferences";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.sIsDark) {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mToolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(mToolbar);

        ActionBar sBar = getSupportActionBar();
        if (sBar != null) {
            sBar.setDisplayShowHomeEnabled(true);
            sBar.setDisplayHomeAsUpEnabled(true);
            sBar.setDisplayShowTitleEnabled(true);

        }


        if (savedInstanceState == null) {
            mSettingsFragment = new SettingsFragment();

        } else {
            mSettingsFragment = (SettingsFragment) getFragmentManager().findFragmentByTag(SETTINGS_FRAGMENT_TAG);
            mPasscodePrefs = savedInstanceState.getBoolean(PASSCODE_PREFS, false);
        }

        // Display the fragment as the main content
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_frame, mSettingsFragment, SETTINGS_FRAGMENT_TAG).commit();


        if (mPasscodePrefs) {
            getSupportActionBar().setTitle(getString(R.string.pref_passcode_toolbar_title));
        }
    }


    //Required Method to Override to Validated Fragments
    @Override
    protected boolean isValidFragment(String fragmentName) {

        return SettingsFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_help:
                AboutDialogFragment dialog = AboutDialogFragment.getInstance();
                dialog.show(getFragmentManager(), getString(R.string.dialog_about_key));
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (mPasscodePrefs) {
            mPasscodePrefs = false;
            mSettingsFragment.setPreferenceScreen(null);
            mSettingsFragment.loadGeneralPreferences();

        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(PASSCODE_PREFS, mPasscodePrefs);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (MyApplication.mPasscodeActivated && MyApplication.mAppWentBackground) {
            startActivity(new Intent(this, PasscodeActivity.class));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (MyApplication.mPasscodeActivated && !isFinishing()) {

            new TaskCheckPasscode(getApplicationContext()).execute();
        }
    }


    /***************************************************************/
    /****************** Settings Fragment **************************/
    /***************************************************************/
    public static class SettingsFragment extends PreferenceFragment {


        private Preference mPasscodeToggle;
        private Preference mPasscodeChange;


        /***** Bind Summary to value - Listener *****/
        private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener
                = (preference, newValue) -> {

                    getActivity().setResult(RESULT_OK);

                    String stringValue = newValue.toString();

                    if (preference instanceof EditTextPreference) {

                        preference.setSummary(stringValue);

                        Answers.getInstance().logCustom(new CustomEvent("Manual Path")
                                .putCustomAttribute("device", Build.DEVICE)
                                .putCustomAttribute("model", Build.MODEL)
                                .putCustomAttribute("manufacturer", Build.MANUFACTURER)
                                .putCustomAttribute("path", stringValue));

                    } else if (preference instanceof ListPreference) {

                        int index = ((ListPreference) preference).findIndexOfValue(stringValue);
                        String summary = "";

                        if(preference.getKey().equals(getString(R.string.pref_auto_update_key))) {
                            String disabled = getResources().getStringArray(R.array.pref_auto_update_list_values)[0];
                            if(!stringValue.equals(disabled))
                                summary += getString(R.string.pref_auto_update_summary) + " - ";
                        }

                        summary += ((ListPreference) preference).getEntries()[index];
                        preference.setSummary(summary);
                    }

                    return true;
                };

        private void bindPreferenceSummaryToValue(Preference preference) {
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

            getActivity().setResult(RESULT_CANCELED);

            if (mPasscodePrefs) {
                loadPasscodePreferences();

            } else {
                loadGeneralPreferences();

            }

        }


        //Helper method for onCreate
        public void loadGeneralPreferences() {

            mToolbar.setTitle(getString(R.string.action_settings));

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);


            Preference resetManualPath = findPreference(getString(R.string.pref_reset_manual_key));
            resetManualPath.setOnPreferenceClickListener(preference -> {
                resetPathPref();
                return true;
            });

            //set dependency on checkbox
            resetManualPath.setDependency(getString(R.string.pref_path_checkbox_key));
            findPreference(getString(R.string.pref_path_manual_key)).setDependency(getString(R.string.pref_path_checkbox_key));

            findPreference(getString(R.string.pref_default_key)).setOnPreferenceClickListener(preference -> {
                showResetWarningDialog();
                return true;
            });

            findPreference(getString(R.string.pref_header_passcode_key)).setOnPreferenceClickListener(preference -> {

                setPreferenceScreen(null);
                loadPasscodePreferences();
                mPasscodePrefs = true;

                return true;
            });

            findPreference(getString(R.string.pref_show_no_password_key)).setOnPreferenceClickListener(preference -> {
                getActivity().setResult(RequestCodes.SHOW_NO_PASSWORD_CODE);
                return true;
            });

            findPreference(getString(R.string.pref_dark_theme_key)).setOnPreferenceClickListener(preference -> {
                MyApplication.darkTheme((CheckBoxPreference)preference);
                getActivity().setResult(RequestCodes.DARK_THEME);
                getActivity().finish();
                return true;
            });

            findPreference(getString(R.string.pref_crashlytics_optout_key)).setOnPreferenceClickListener(preference -> {
                Toast.makeText(getActivity(), getString(R.string.toast_crashlytics_opt_out), Toast.LENGTH_LONG).show();
                return true;
            });

            //Summary to Value
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_path_manual_key)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_auto_update_key)));
        }

        //Helper method for onCreate
        public void loadPasscodePreferences() {

            mToolbar.setTitle(getString(R.string.pref_passcode_toolbar_title));
            addPreferencesFromResource(R.xml.passcode_prefs);


            mPasscodeToggle = findPreference(getString(R.string.pref_passcode_toggle_key));
            mPasscodeChange = findPreference(getString(R.string.pref_passcode_change_key));


            if (MyApplication.mPasscodeActivated) {
                mPasscodeToggle.setTitle(R.string.pref_passcode_toggle_title_off);
                mPasscodeChange.setEnabled(true);

            } else {
                mPasscodeToggle.setTitle(R.string.pref_passcode_toggle_title_on);
                mPasscodeChange.setEnabled(false);

            }


            mPasscodeToggle.setOnPreferenceClickListener(preference -> {

                if (!MyApplication.mPasscodeActivated) {

                    Intent intent = new Intent(getActivity(), PasscodeActivity.class);
                    intent.putExtra(MyApplication.PASSCODE_REQUEST_CODE, RequestCodes.PASSCODE_PREF_ENABLE);
                    startActivityForResult(intent, RequestCodes.PASSCODE_PREF_ENABLE);


                } else {
                    Intent intent = new Intent(getActivity(), PasscodeActivity.class);
                    intent.putExtra(MyApplication.PASSCODE_REQUEST_CODE, RequestCodes.PASSCODE_PREF_DISABLE);
                    startActivityForResult(intent, RequestCodes.PASSCODE_PREF_DISABLE);

                }

                return true;
            });


            mPasscodeChange.setOnPreferenceClickListener(preference -> {

                Intent intent = new Intent(getActivity(), PasscodeActivity.class);
                intent.putExtra(MyApplication.PASSCODE_REQUEST_CODE, RequestCodes.PASSCODE_PREF_CHANGE);
                startActivityForResult(intent, RequestCodes.PASSCODE_PREF_CHANGE);
                return true;
            });


        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {

            switch (requestCode) {

                case RequestCodes.PASSCODE_PREF_ENABLE:
                    if (resultCode == RESULT_OK) {
                        mPasscodeToggle.setTitle(R.string.pref_passcode_toggle_title_off);
                        mPasscodeChange.setEnabled(true);
                        MyApplication.mAppWentBackground = false;
                    }
                    break;


                case RequestCodes.PASSCODE_PREF_DISABLE:
                    if (resultCode == RESULT_OK) {
                        mPasscodeToggle.setTitle(R.string.pref_passcode_toggle_title_on);
                        mPasscodeChange.setEnabled(false);
                    }
                    break;

            }
        }

        //Restore wpa_supplicant path to default
        private void resetPathPref() {

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.pref_path_manual_key), getString(R.string.pref_path_default));
            editor.apply();


            findPreference(getString(R.string.pref_path_manual_key)).setSummary(getString(R.string.pref_path_default));

            //Refresh Preference Screen
            setPreferenceScreen(null);
            loadGeneralPreferences();

        }


        private void showResetWarningDialog() {

            String[] buttons = getResources().getStringArray(R.array.dialog_warning_reset_buttons);

            AlertDialog.Builder builder;

            if (MyApplication.sIsDark) {
                builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme_Dark);
            } else {
                builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
            }

            //Send Result Codes to target fragment according to button clicked
            builder.setMessage(R.string.dialog_warning_reset_message)
                    .setTitle(R.string.dialog_warning_reset_title)
                    .setPositiveButton(buttons[0], (dialog, which) -> {
                        getActivity().setResult(RequestCodes.RESET_TO_DEFAULT);
                        getActivity().finish();
                    })
                    .setNegativeButton(buttons[1], (dialog, which) -> {
                        //Dismiss Dialog
                    });

            builder.create().show();
        }

    }
}
