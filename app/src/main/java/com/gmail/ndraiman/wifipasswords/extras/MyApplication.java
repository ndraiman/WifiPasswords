package com.gmail.ndraiman.wifipasswords.extras;

import android.app.Application;
import android.content.Context;

import com.gmail.ndraiman.wifipasswords.database.PasswordDB;

import java.util.concurrent.atomic.AtomicInteger;

public class MyApplication extends Application {

    private static MyApplication sInstance;
    private static PasswordDB mPasswordDB;
    private static AtomicInteger mOpenCounter = new AtomicInteger();

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPasswordDB = new PasswordDB(this);
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

    //TODO Test if the addition of AtomicInteger helps with SQLiteDB remaining open error
    //TODO if not, remove it and remove "closeDatabase()" method from PasswordDB.java
}
