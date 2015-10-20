package com.gmail.ndrdevelop.wifipasswords.recycler;

import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;

public interface ItemTouchHelperAdapter {

    boolean onItemMove(int fromPosition, int toPosition);

    WifiEntry onItemDismiss(int position);

}
