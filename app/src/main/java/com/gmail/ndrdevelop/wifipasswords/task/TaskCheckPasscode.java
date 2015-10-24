package com.gmail.ndrdevelop.wifipasswords.task;


import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;

import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;

import java.util.List;

public class TaskCheckPasscode extends AsyncTask<Void, Void, Boolean>{


    Context mApplicationContext;


    public TaskCheckPasscode(Context applicationContext) {

        mApplicationContext = applicationContext;
    }

    @Override
    protected Boolean doInBackground(Void... params) {

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Boolean result = false;

        boolean isScreenOn = ((PowerManager)mApplicationContext.getSystemService(android.content.Context.POWER_SERVICE)).isScreenOn();
        boolean isAppForeground = isAppOnForeground(mApplicationContext);


        if (!isScreenOn || !isAppForeground) {
            result = true;
        }
        return result;
    }


    @Override
    protected void onPostExecute(Boolean result) {

            MyApplication.mAppWentBackground = result;
    }



    protected boolean isAppOnForeground(final Context context) {
        List<ActivityManager.RunningAppProcessInfo> appProcesses = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();

        if (appProcesses == null) {
            return false;
        }

        final String packageName = context.getPackageName();

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if ((appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

}
