package com.gmail.ndraiman.wifipasswords.recycler;

/**
 * Created by ND88 on 22/09/2015.
 */
public interface ItemTouchHelperAdapter {

    boolean onItemMove(int fromPosition, int toPosition);

    void onItemDismiss(int position);
}
