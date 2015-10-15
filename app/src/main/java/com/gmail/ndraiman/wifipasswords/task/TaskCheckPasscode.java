package com.gmail.ndraiman.wifipasswords.task;


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.extras.MyApplication;

import java.util.List;

public class TaskCheckPasscode extends AsyncTask<Void, Void, Boolean>{

    public static final String TAG = "TaskCheckPasscode";

    private Context mApplicationContext;
    private Context mActivityContext;


    public TaskCheckPasscode(Context applicationContext, Context activityContext) {
        mApplicationContext = applicationContext;
        mActivityContext = activityContext;
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
//        boolean isAppBackground = isAppIsInBackground(mApplicationContext);

//        Log.e(TAG, "isScreenOn = " + isScreenOn);
//        Log.e(TAG, "isAppForeground = " + isAppForeground);
//        Log.e(TAG, "isAppBackground = " + isAppBackground);



        if (!isScreenOn || !isAppForeground) {
            result = true;
        }
        return result;
    }


    @Override
    protected void onPostExecute(Boolean result) {
        Log.e(TAG, "onPostExecute() called with: " + "result = [" + result + "]");

            MyApplication.mAppWentBackground = result;
    }



    protected boolean isAppOnForeground(final Context context) {
        List<ActivityManager.RunningAppProcessInfo> appProcesses = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningAppProcesses();

        if (appProcesses == null) {
            return false;
        }

        final String packageName = context.getPackageName();
        Log.e(TAG, "packageName = " + packageName);
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if ((appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) &&
                    appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }


    private boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }
}
