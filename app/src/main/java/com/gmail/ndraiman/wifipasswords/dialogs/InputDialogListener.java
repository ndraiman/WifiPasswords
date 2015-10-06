package com.gmail.ndraiman.wifipasswords.dialogs;


import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;

public interface InputDialogListener {

    public void onSubmitAddDialog(String title, String password);

    public void onSubmitTagDialog(String tag, ArrayList<WifiEntry> listWifi, ArrayList<Integer> positions);
}
