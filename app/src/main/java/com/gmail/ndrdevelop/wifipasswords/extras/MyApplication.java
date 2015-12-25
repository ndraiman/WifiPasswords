package com.gmail.ndrdevelop.wifipasswords.extras;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;

import com.crashlytics.android.Crashlytics;
import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import io.fabric.sdk.android.Fabric;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private static PasswordDB mPasswordDB;
    private static AtomicInteger mOpenCounter = new AtomicInteger();

    public static boolean mPasscodeActivated;
    public static boolean mAppWentBackground;

    public static final String FIRST_LAUNCH = "first_launch";
    public static final String DEVICE_UUID = "uuid";

    public static final String PASSCODE_STATE = "passcode_state";
    public static final String PASSCODE_KEY = "passcode_key";
    public static final String PASSCODE_REQUEST_CODE = "passcode_request_code";

    public static final String NO_PASSWORD_TEXT = "no password";

    public static boolean sIsDark;
    public static String sMyUUID;
    public static boolean sShouldAutoUpdateList;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPasswordDB = new PasswordDB(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Generate UUID for use with Crashlytics
        if(sharedPreferences.getBoolean(FIRST_LAUNCH, true)) {
            generateUUID();
        } else {
            sMyUUID = sharedPreferences.getString(DEVICE_UUID, "");
        }


        //Check user opt-out of Crashlytics before initializing it
        if(!sharedPreferences.getBoolean(getString(R.string.pref_crashlytics_optout_key), false)) {
            Fabric.with(this, new Crashlytics());
            logUser();
        }

        mPasscodeActivated = sharedPreferences.getBoolean(PASSCODE_STATE, false);
        mAppWentBackground = true;
        sShouldAutoUpdateList = !mPasscodeActivated;

        sIsDark = sharedPreferences.getBoolean(getString(R.string.pref_dark_theme_key), false);
    }


    public static MyApplication getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }

    public synchronized static PasswordDB getWritableDatabase() {

        if (mPasswordDB == null && mOpenCounter.incrementAndGet() == 1) {
            mPasswordDB = new PasswordDB(getAppContext());
        }
        return mPasswordDB;
    }

    public synchronized static void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            mPasswordDB.close();
        }

    }

    public static void darkTheme(CheckBoxPreference preference) {
        sIsDark = preference.isChecked();
    }

    private void generateUUID() {

        sMyUUID = UUID.randomUUID().toString();
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(DEVICE_UUID, sMyUUID).apply();

    }

    private void logUser() {

        Crashlytics.setUserIdentifier(sMyUUID);
        Crashlytics.setUserName(Build.DEVICE);
        Crashlytics.setUserEmail(Build.MODEL);
    }

}
