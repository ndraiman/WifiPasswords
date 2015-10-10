package com.gmail.ndraiman.wifipasswords.task;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.extras.RootCheck;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.recycler.WifiListLoadedListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/***********************************************************************/
//Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

/***********************************************************************/
public class TaskLoadWifiEntries extends AsyncTask<String, Void, ArrayList<WifiEntry>> {


    private static final String TAG = "TaskLoadWifiEntries";
    private static final String APP_FOLDER = "WifiPasswords";
    private WifiListLoadedListener mListListener;
    private boolean hasRootAccess = true;
    private String mPath;
    private String mFileName;
    private CustomAlertDialogListener mDialogListener;
    private boolean mResetDB;

    public TaskLoadWifiEntries(String filePath, String fileName, boolean resetDB, WifiListLoadedListener listListener, CustomAlertDialogListener dialogListener) {
        mListListener = listListener;
        mPath = filePath;
        mFileName = fileName;
        mDialogListener = dialogListener;
        mResetDB = resetDB;

        Log.d(TAG, "Constructor - mPath = " + mPath + "\n mFileName = " + mFileName);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        //Remove "No Root Access" error
//        if(WifiListFragment.textNoRoot.getVisibility() == View.VISIBLE && hasRootAccess) {
//            WifiListFragment.textNoRoot.setVisibility(View.GONE);
//        }
    }

    @Override
    protected ArrayList<WifiEntry> doInBackground(String... params) {

        if(!(hasRootAccess = RootCheck.canRunRootCommands())) {
            Log.e(TAG, "No Root Access");
            cancel(true);
        }

        boolean dirCreated = createDir();
        if (!dirCreated) {
            Log.e(TAG, "Failed to create app directory");
            return null;
        }
        copyFile();

        return readFile();
    }

    @Override
    protected void onPostExecute(ArrayList<WifiEntry> wifiEntries) {
        Log.d(TAG, "OnPost Execute \n" + wifiEntries.toString());

        //Insert Wifi Entries to database
        PasswordDB db = MyApplication.getWritableDatabase();

        if(mResetDB) {
            Log.d(TAG, "Resetting Database");
            db.deleteAll(false);
            db.deleteAll(true);
        }

        db.insertWifiEntries(wifiEntries, mResetDB, false); //keep Tags according to mResetDB

        //Update RecyclerView
        if(mListListener != null) {

            wifiEntries = new ArrayList<>(db.getAllWifiEntries(false)); //re-read list from database as it removes duplicates
            mListListener.onWifiListLoaded(wifiEntries, wifiEntries.size(), mResetDB);
        }

        MyApplication.closeDatabase();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        //Show "No Root Access" error
        if(!hasRootAccess) {
//            WifiListFragment.textNoRoot.setVisibility(View.VISIBLE);

            if(mListListener != null) {
                Log.d(TAG, "OnCancelled Execute \n");
                mListListener.showRootErrorDialog();
                //return empty list
//                mListListener.onWifiListLoaded(new ArrayList<WifiEntry>());
            }
        }

    }



    /**************
     * Helper Methods
     ********************/
    private boolean createDir() {
        Log.e(TAG, "Creating Dir");
        File folder = new File(Environment.getExternalStorageDirectory() + "/" + APP_FOLDER);
        boolean dirCreated = true;
        if (!folder.exists()) {
            dirCreated = folder.mkdir();
        }
        if (!dirCreated) {
            Log.e(TAG, "Failed to create directory");
            return false;
        }

        return true;
    }

    private void copyFile() {
        if (!RootCheck.canRunRootCommands()) {
            return;
        }

        Log.e(TAG, "Copying File");
        try {
            Process suProcess = Runtime.getRuntime().exec("su -c cp " + mPath + mFileName + " /sdcard/" + APP_FOLDER);
            suProcess.waitFor(); //wait for SU command to finish
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "copyFile Error: " + e.getClass().getName() + " " + e);
            e.printStackTrace();
        }
    }

    private ArrayList<WifiEntry> readFile() {

        ArrayList<WifiEntry> listWifi = new ArrayList<>();
        try {


            File directory = Environment.getExternalStorageDirectory();
            File file = new File(directory + "/" + APP_FOLDER + "/" + mFileName);
            Log.d(TAG, directory + "/" + APP_FOLDER + "/" + mFileName);

            if (!file.exists()) {
                Log.e(TAG, "readFile - File not found");
                //Show Error Dialog
                mDialogListener.showPathErrorDialog();
                return new ArrayList<>();
            }

            Log.e(TAG, "Starting to read");

            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = "";
            String title = "";
            String password = "";

            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("network={")) {

                    line = bufferedReader.readLine();
                    title = line.substring(7, line.length() - 1);

                    line = bufferedReader.readLine();

                    //Log.i(TAG, title + " " + line.substring(6, line.length() - 1));
                    //Log.i(TAG, title + " " + line.substring(1, 4));

                    if ((line.substring(1, 4)).equals("psk")) {
                        password = line.substring(6, line.length() - 1);
                    } else {
                        password = "no password";
                    }

                    Log.e(TAG, title + " " + password);

                    WifiEntry current = new WifiEntry(title, password);
                    listWifi.add(current);
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return listWifi;
    }
}
