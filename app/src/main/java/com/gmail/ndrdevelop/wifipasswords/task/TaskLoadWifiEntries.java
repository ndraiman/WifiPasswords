package com.gmail.ndrdevelop.wifipasswords.task;


import android.os.AsyncTask;
import android.util.Xml;

import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.dialogs.CustomAlertDialogListener;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RootCheck;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;
import com.gmail.ndrdevelop.wifipasswords.recycler.WifiListLoadedListener;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
    String mOreoLocation = "/data/misc/wifi/WifiConfigStore.xml";
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

    /****************** Oreo Helper Methods: BEGIN ******************/
    private ArrayList<WifiEntry> readNetworkList(XmlPullParser parser) throws XmlPullParserException, IOException {
        ArrayList<WifiEntry> result = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "NetworkList");
        boolean doLoop = true;
        while (doLoop) {
            parser.next();
            String tagName = parser.getName();
            if (tagName == null) {
                tagName = "";
            }
            doLoop = (!tagName.equalsIgnoreCase("NetworkList"));
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (tagName.equals("Network")) {
                WifiEntry newWifi = readNetworkEntry(parser);
                if (newWifi.getTitle().length() != 0) {
                    result.add(newWifi);
                }
            } else {
                skip(parser);
            }
        }
        return result;
    }

    // Parses a "Network" entry
    private WifiEntry readNetworkEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "Network");
        WifiEntry result = new WifiEntry("", "");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            // Starts by looking for the entry tag
            if (tagName.equals("WifiConfiguration")) {
                result = readWiFiConfig(parser, result);
//            } else if (tagName.equals("WifiEnterpriseConfiguration")) {
//                result.setTyp(WifiObject.TYP_ENTERPRISE);
            } else {
                skip(parser);
            }
        }
        return result;
    }

    // Parses a "WifiConfiguration" entry
    private WifiEntry readWiFiConfig(XmlPullParser parser, WifiEntry result) throws XmlPullParserException, IOException {
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            String name = parser.getAttributeValue(null, "name");
            if (name.equals("SSID") && !tagName.equalsIgnoreCase("null")) {
                result.setTitle(readTag(parser, tagName));
            } else if (name.equals("PreSharedKey") && !tagName.equalsIgnoreCase("null")) {
                result.setPassword(readTag(parser, tagName));
//                result.setTyp(WifiObject.TYP_WPA);
            } else if (name.equals("WEPKeys") && !tagName.equalsIgnoreCase("null")) {
                result.setPassword(readTag(parser, tagName));
//                result.setTyp(WifiObject.TYP_WEP);
            } else {
                skip(parser);
            }
        }
        return result;
    }

    // Return the text for a specified tag.
    private String readTag(XmlPullParser parser, String tagName) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tagName);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, null, tagName);
        if (tagName.equalsIgnoreCase("string")
                && Character.toString(result.charAt(0)).equals("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private ArrayList<WifiEntry> readOreoFile() {
        ArrayList<WifiEntry> result = new ArrayList<>();
        try {
            Process suOreoProcess = Runtime.getRuntime().exec("su -c /system/bin/cat " + mOreoLocation);
            try {
                suOreoProcess.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(suOreoProcess.getInputStream(), null);
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().equalsIgnoreCase("NetworkList")) {
                    // Process the <Network> entries in the list
                    result.addAll(readNetworkList(parser));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } finally {
            if (!result.isEmpty()) {
                return result;
            } else {
                return null;
            }
        }
    }

    /****************** Oreo Helper Methods: END ******************/

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

            if (bufferedReader == null) {
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


                    if (password.equals("")) {
                        password = MyApplication.NO_PASSWORD_TEXT;
                    }

                    WifiEntry current = new WifiEntry(title, password);
                    listWifi.add(current);

                    title = "";
                    password = "";
                }
            }

            // Add Oreo results here.
            ArrayList<WifiEntry> oreoList = readOreoFile();
            if ((oreoList != null) && (!oreoList.isEmpty())) {
                listWifi.addAll(oreoList);
            }


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return listWifi;
    }
}
