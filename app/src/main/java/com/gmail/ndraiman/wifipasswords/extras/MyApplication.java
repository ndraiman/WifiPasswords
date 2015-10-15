package com.gmail.ndraiman.wifipasswords.extras;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gmail.ndraiman.wifipasswords.database.PasswordDB;

import java.util.concurrent.atomic.AtomicInteger;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private static PasswordDB mPasswordDB;
    private static AtomicInteger mOpenCounter = new AtomicInteger();

    public static boolean mPasscodeActivated;
    public static boolean mAppWentBackground;

    public static final String SHARE_WARNING = "share_dialog"; //sharedPrefs key
    public static final String PASSCODE_STATE = "passcode_state";
    public static final String PASSCODE_KEY = "passcode_key";
    public static final String PASSCODE_REQUEST_CODE = "passcode_request_code";

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPasswordDB = new PasswordDB(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPasscodeActivated = sharedPreferences.getBoolean(PASSCODE_STATE, false);
        mAppWentBackground = true;
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
}
