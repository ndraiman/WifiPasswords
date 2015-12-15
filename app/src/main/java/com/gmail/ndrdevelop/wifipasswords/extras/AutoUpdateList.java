package com.gmail.ndrdevelop.wifipasswords.extras;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.activities.MainActivity;
import com.gmail.ndrdevelop.wifipasswords.fragments.WifiListFragment;


public class AutoUpdateList {

    private static final String DEFAULT_UPDATE = "1";
    private static final String LAST_UPDATE = "last_update";
    private static final int DISABLED = -1;


    public static void update(Context context, FragmentManager fragmentManager) {

        if(!MyApplication.sShouldAutoUpdateList)
            return;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        long currentTime = System.currentTimeMillis();
        long lastUpdateTime = sharedPreferences.getLong(LAST_UPDATE, 0);

        if(lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            editor.putLong(LAST_UPDATE, lastUpdateTime).apply();
        }

        int updateOption = Integer.parseInt(sharedPreferences.getString(context.getString(R.string.pref_auto_update_key), DEFAULT_UPDATE));

        if(updateOption != DISABLED) {
            MyApplication.sShouldAutoUpdateList = false;

            if(currentTime > lastUpdateTime + getMillisUntilPrompt(updateOption)) {

                editor.putLong(LAST_UPDATE, System.currentTimeMillis()).apply();
                ((WifiListFragment) fragmentManager
                        .findFragmentByTag(MainActivity.WIFI_LIST_FRAGMENT_TAG))
                        .loadFromFile(false);
            }

        }
    }


    private static int getMillisUntilPrompt(int days) {
        return days * 24 * 60 * 60 * 1000;
    }
}
