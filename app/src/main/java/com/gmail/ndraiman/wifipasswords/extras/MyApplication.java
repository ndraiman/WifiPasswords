package com.gmail.ndraiman.wifipasswords.extras;

import android.app.Application;
import android.content.Context;

import com.gmail.ndraiman.wifipasswords.database.PasswordDB;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private static PasswordDB mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mDatabase = new PasswordDB(this);
    }

    public static MyApplication getInstance() {
        return sInstance;
    }

    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }

    public synchronized static PasswordDB getWritableDatabase() {
        if (mDatabase == null) {
            mDatabase = new PasswordDB(getAppContext());
        }
        return mDatabase;
    }
}
