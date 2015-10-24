package com.gmail.ndrdevelop.wifipasswords.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;


/*****************************************************************************************/
/****************** Implement Clicks & Gesture for RecycleView ***************************/
/*****************************************************************************************/
public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

    GestureDetector gestureDetector;
    ClickListener clickListener;


    public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {

        this.clickListener = clickListener;

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {

                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(child != null && clickListener != null) {
                    clickListener.onDoubleTap(child, recyclerView.getChildAdapterPosition(child));
                }
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {

                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if(child != null && clickListener != null) {
                    clickListener.onLongClick(child, recyclerView.getChildAdapterPosition(child));
                }

                super.onLongPress(e);
            }
        });
    }


    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child = rv.findChildViewUnder(e.getX(), e.getY());
        if(child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {

            clickListener.onClick(child, rv.getChildAdapterPosition(child));
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }




    public interface ClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
        void onDoubleTap(View view, int position);
    }
}
