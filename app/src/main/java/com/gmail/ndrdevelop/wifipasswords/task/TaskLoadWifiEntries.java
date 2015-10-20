package com.gmail.ndrdevelop.wifipasswords.task;


import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RootCheck;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListLoadedListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


/***********************************************************************/
//Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

/***********************************************************************/
public class TaskLoadWifiEntries extends AsyncTask<String, Void, ArrayList<WifiEntry>> {


    private static final String APP_FOLDER = "WifiPasswords";
    private WifiListLoadedListener mListListener;
    private boolean mRootAccess = true;
    private String mPath;
    private String mFileName;
    private CustomAlertDialogListener mDialogListener;
    private boolean mResetDB;
    //TODO return wpa_supplicant.conf path to be the first in the array
    private String[] mLocationList = {"/data/wifi/bcm_supp.conf", "/data/misc/wifi/wpa.conf", "/data/misc/wifi/wpa_supplicant.conf"};
    boolean mManualLocation;

    private static final String WPA_PSK = "psk";
    private static final String WEP_PSK = "auth_alg=OPEN SHARED";

    public TaskLoadWifiEntries(String filePath, String fileName, boolean resetDB, WifiListLoadedListener listListener, CustomAlertDialogListener dialogListener) {
        mListListener = listListener;
        mPath = filePath;
        mFileName = fileName;
        mDialogListener = dialogListener;
        mResetDB = resetDB;
        mManualLocation = true;
    }

    public TaskLoadWifiEntries(boolean resetDB, WifiListLoadedListener listListener, CustomAlertDialogListener dialogListener) {
        mListListener = listListener;
        mDialogListener = dialogListener;
        mResetDB = resetDB;
        mManualLocation = false;
    }


    @Override
    protected ArrayList<WifiEntry> doInBackground(String... params) {

        if (!(mRootAccess = RootCheck.canRunRootCommands())) {
            cancel(true);
        }

        boolean dirCreated = createDir();
        if (!dirCreated) {
            return null;
        }
//        copyFile();

        return readFile();
    }


    @Override
    protected void onPostExecute(ArrayList<WifiEntry> wifiEntries) {

        //Insert Wifi Entries to database
        PasswordDB db = MyApplication.getWritableDatabase();

        if (mResetDB) {
            db.purgeDatabase();
        }

        db.insertWifiEntries(wifiEntries, mResetDB, false); //keep Tags according to mResetDB

        //Update RecyclerView
        if (mListListener != null) {

            wifiEntries = new ArrayList<>(db.getAllWifiEntries(false)); //re-read list from database as it removes duplicates
            mListListener.onWifiListLoaded(wifiEntries, mResetDB ? wifiEntries.size() : PasswordDB.mNewEntriesOnLastInsert, mResetDB);
        }

        MyApplication.closeDatabase();

        //Delete wpa_supplicant file for security reasons
        File directory = Environment.getExternalStorageDirectory();
        File file = new File(directory + "/" + APP_FOLDER + "/" + mFileName);

        file.delete();
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();

        //Show "No Root Access" error
        if (!mRootAccess) {

            if (mDialogListener != null) {

                mDialogListener.showRootErrorDialog();
            }
        }

    }


    /****************************************************/
    /****************** Helper Methods ******************/
    /****************************************************/

    private boolean createDir() {

        File folder = new File(Environment.getExternalStorageDirectory() + "/" + APP_FOLDER);
        boolean dirCreated = true;
        if (!folder.exists()) {
            dirCreated = folder.mkdir();
        }
        if (!dirCreated) {
            return false;
        }

        return true;
    }


    private void copyFile() {

        try {
            Process suProcess = Runtime.getRuntime().exec("su -c cp " + mPath + mFileName + " /sdcard/" + APP_FOLDER);
            suProcess.waitFor(); //wait for SU command to finish

        } catch (IOException | InterruptedException e) {

            e.printStackTrace();
        }
    }


    private ArrayList<WifiEntry> readFile() {
        Log.e("TaskLoadWifiEntries", "readFile()");
        ArrayList<WifiEntry> listWifi = new ArrayList<>();

        try {

            File directory = Environment.getExternalStorageDirectory();
            File file = null;

            if(mManualLocation) {
                copyFile();
                file = new File(directory + "/" + APP_FOLDER + "/" + mFileName);

                if(!file.exists()) {

                    //Show Error Dialog

                    if (mRootAccess) {
                        mDialogListener.showPathErrorDialog();
                    }

                    return new ArrayList<>();
                }

            } else {
                //Check for file in all known locations
                for (int i = 0; i < mLocationList.length; i++) {
                    Log.e("TaskLoadWifiEntries", "i = " + i);
                    String fileLocation = mLocationList[i];

                    mPath = fileLocation.substring(0, fileLocation.lastIndexOf("/") + 1);
                    mFileName = fileLocation.substring(fileLocation.lastIndexOf("/") + 1);

                    Log.e("TaskLoadWifiEntries", "fileLocation = " + fileLocation);
                    Log.e("TaskLoadWifiEntries", "mPath = " + mPath);
                    Log.e("TaskLoadWifiEntries", "mFileName = " + mFileName);

                    copyFile();

                    file = new File(directory + "/" + APP_FOLDER + "/" + mFileName);

                    if (file.exists()) {
                        break;

                    } else if (!file.exists() && i == mLocationList.length) {
                        //Show Error Dialog

                        if (mRootAccess) {
                            mDialogListener.showPathErrorDialog();
                        }
                        return new ArrayList<>();
                    }
                }
            }

            if(file == null) {
                Log.e("TaskLoadWifiEntries", "File == null");
                return new ArrayList<>();
            }

            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String line = "";
            String title = "";
            String password = "";

            while ((line = bufferedReader.readLine()) != null) {
                if (line.equals("network={")) {

                    line = bufferedReader.readLine();
                    title = line.substring(7, line.length() - 1);

                    line = bufferedReader.readLine();


                    if ((line.substring(1, 4)).equals(WPA_PSK)) {
                        password = line.substring(6, line.length() - 1);

                    } else if ((line = bufferedReader.readLine()).substring(1, line.length()).equals(WEP_PSK)) {
                        line = bufferedReader.readLine();
                        password = line.substring(10, line.length() - 1);

                    } else {
                        password = MyApplication.NO_PASSWORD_TEXT;
                    }

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
