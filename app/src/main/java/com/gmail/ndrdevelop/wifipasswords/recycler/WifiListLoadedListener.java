package com.gmail.ndrdevelop.wifipasswords.recycler;

import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;

public interface WifiListLoadedListener {

    public void onWifiListLoaded(ArrayList<WifiEntry> listWifi, int numOfEntries, boolean resetDB);

}
