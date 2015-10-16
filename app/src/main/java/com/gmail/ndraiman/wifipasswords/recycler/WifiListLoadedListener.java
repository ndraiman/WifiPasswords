package com.gmail.ndraiman.wifipasswords.recycler;

import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;

public interface WifiListLoadedListener {

    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi, int numOfEntries, boolean resetDB);

}
