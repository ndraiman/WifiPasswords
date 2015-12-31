package com.gmail.ndrdevelop.wifipasswords.task;


import android.os.AsyncTask;

import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RootCheck;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListLoadedListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


/***********************************************************************/
//Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

/***********************************************************************/
public class TaskLoadWifiEntries extends AsyncTask<String, Void, ArrayList<WifiEntry>> {

    WifiListLoadedListener mListListener;
    boolean mRootAccess = true;
    String mPath;
    String mFileName;
    CustomAlertDialogListener mDialogListener;
    boolean mResetDB;
    String[] mLocationList = {"/data/misc/wifi/wpa_supplicant.conf", "/data/wifi/bcm_supp.conf", "/data/misc/wifi/wpa.conf"};
    boolean mManualLocation;

    final String SSID = "ssid";
    final String WPA_PSK = "psk";
    final String WEP_PSK = "wep_key0";
    final String ENTRY_START = "network={";
    final String ENTRY_END = "}";

    //Constructor for Manual Path
    public TaskLoadWifiEntries(String filePath, String fileName, boolean resetDB, WifiListLoadedListener listListener, CustomAlertDialogListener dialogListener) {
        mListListener = listListener;
        mPath = filePath;
        mFileName = fileName;
        mDialogListener = dialogListener;
        mResetDB = resetDB;
        mManualLocation = true;
    }

    //Constructor for Known Paths
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
            return null;
        }

        return readFile();
    }


    @Override
    protected void onPostExecute(ArrayList<WifiEntry> wifiEntries) {

        //Insert Wifi Entries to database
        PasswordDB db = MyApplication.getWritableDatabase();

//        if (mResetDB) {
//            db.purgeDatabase();
//        }

        db.insertWifiEntries(wifiEntries, mResetDB, false); //keep Tags according to mResetDB

        //Update RecyclerView
        if (mListListener != null) {

            wifiEntries = new ArrayList<>(db.getAllWifiEntries(false)); //re-read list from database as it removes duplicates
            mListListener.onWifiListLoaded(wifiEntries, mResetDB ? wifiEntries.size() : PasswordDB.mNewEntriesOnLastInsert, mResetDB);
        }

        MyApplication.closeDatabase();
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


    private ArrayList<WifiEntry> readFile() {

        ArrayList<WifiEntry> listWifi = new ArrayList<>();
        BufferedReader bufferedReader = null;

        try {

            if (mManualLocation) {


                Process suProcess = Runtime.getRuntime().exec("su -c /system/bin/cat " + mPath + mFileName);
                try {
                    suProcess.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                bufferedReader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                String testString = bufferedReader.readLine();

                if (testString == null) {
                    //Show Error Dialog

                    if (mRootAccess) {
                        mDialogListener.showPathErrorDialog();
                    }

                    return new ArrayList<>();
                }

            } else {

                //Check for file in all known locations
                for (int i = 0; i < mLocationList.length; i++) {

                    Process suProcess = Runtime.getRuntime().exec("su -c /system/bin/cat " + mLocationList[i]);
                    try {
                        suProcess.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    bufferedReader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
                    String testString = bufferedReader.readLine();

                    if (testString != null) {
                        break;

                    } else if (i == mLocationList.length - 1) {
                        //Show Error Dialog

                        if (mRootAccess) {
                            mDialogListener.showPathErrorDialog();
                        }
                        return new ArrayList<>();
                    }
                }
            }

            if(bufferedReader == null) {
                return new ArrayList<>();
            }


            String line;
            String title = "";
            String password = "";

            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(ENTRY_START)) {

                    while (!line.contains(ENTRY_END)) {
                        line = bufferedReader.readLine();

                        if (line.contains(SSID)) {
                            title = line.replace(SSID, "").replace("=", "").replace("\"", "").replace(" ", "");
                        }

                        if (line.contains(WPA_PSK)) {

                            password = line.replace(WPA_PSK, "").replace("=", "").replace("\"", "").replace(" ", "");

                        } else if (line.contains(WEP_PSK)) {

                            password = line.replace(WEP_PSK, "").replace("=", "").replace("\"", "").replace(" ", "");
                        }

                    }


                    if(password.equals("")) {
                        password = MyApplication.NO_PASSWORD_TEXT;
                    }

                    WifiEntry current = new WifiEntry(title, password);
                    listWifi.add(current);

                    title = "";
                    password = "";
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return listWifi;
    }
}
