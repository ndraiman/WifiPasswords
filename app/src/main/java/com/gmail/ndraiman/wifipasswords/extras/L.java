package com.gmail.ndraiman.wifipasswords.extras;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by ND88 on 16/09/2015.
 */
public class L {

    private static final String LOG_TAG = "ND";

    public static void m(String message) {
        Log.d(LOG_TAG, "" + message);
    }

    public static void t(Context context, String message) {
        Toast.makeText(context, message + "", Toast.LENGTH_SHORT).show();
    }
    public static void T(Context context, String message) {
        Toast.makeText(context, message + "", Toast.LENGTH_LONG).show();
    }

    public static void e(String message) {
        Log.e(LOG_TAG, message);
    }
}