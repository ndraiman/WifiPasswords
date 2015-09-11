package com.gmail.ndraiman.wifipasswords;

import java.util.ArrayList;

/**
 * Created by ND88 on 11/09/2015.
 */
public class FetchSupplicant extends ExecuteAsRootBase {

    public static String fileData = "";

    public FetchSupplicant() {

    }

    @Override
    protected ArrayList<String> getCommandsToExecute() {

        ArrayList<String> arrayList = new ArrayList<>();
        //copy supplicant to sdcard
        arrayList.add("su -c cp /data/misc/wifi/wpa_supplicant.conf /sdcard/WifiPasswords");


        return arrayList;
    }

}
