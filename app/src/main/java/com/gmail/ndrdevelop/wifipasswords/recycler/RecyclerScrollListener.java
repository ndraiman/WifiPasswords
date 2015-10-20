package com.gmail.ndrdevelop.wifipasswords.recycler;

import android.support.v7.widget.RecyclerView;


//Handles FAB show\hide
public abstract class RecyclerScrollListener extends RecyclerView.OnScrollListener {

    static final float MINIMUM = 25;
    int scrollDist = 0;
    boolean isVisible = true;


    public abstract void show();
    public abstract void hide();

    //dy gives a positive value on scrolling down, and negative value scrolling up.
    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        if (isVisible && scrollDist > MINIMUM) {
            hide();
            scrollDist = 0;
            isVisible = false;

        } else if (!isVisible && scrollDist < -MINIMUM) {
            show();
            scrollDist = 0;
            isVisible = true;
        }

        if((isVisible && dy > 0) || (!isVisible && dy < 0)) {
            scrollDist += dy;
        }
    }
}
