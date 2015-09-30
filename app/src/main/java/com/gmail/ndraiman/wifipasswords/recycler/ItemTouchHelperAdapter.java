package com.gmail.ndraiman.wifipasswords.recycler;

import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

public interface ItemTouchHelperAdapter {

    boolean onItemMove(int fromPosition, int toPosition);

    WifiEntry onItemDismiss(int position);

}
