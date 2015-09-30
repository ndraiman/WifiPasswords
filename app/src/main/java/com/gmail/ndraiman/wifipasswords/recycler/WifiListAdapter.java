package com.gmail.ndraiman.wifipasswords.recycler;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.MyViewHolder>
        implements ItemTouchHelperAdapter {

    private static final String TAG = "RecyclerAdapter";
    private LayoutInflater layoutInflater;
    private List<WifiEntry> mListWifi;
    private ItemDragListener mDragListener;
    private int mPreviousPosition = -1; //used for Item Animation
    private boolean mShowDragHandler;
    private SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    public WifiListAdapter(Context context, ItemDragListener dragListener) {
        layoutInflater = LayoutInflater.from(context);
        mDragListener = dragListener;
        mListWifi = new ArrayList<>();
        mShowDragHandler = false;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View view = layoutInflater.inflate(R.layout.wifi_entry_row, parent, false);

        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        WifiEntry currentEntry = mListWifi.get(position);

        holder.wifiTitle.setText(currentEntry.getTitle());
        holder.wifiPassword.setText(currentEntry.getPassword());

        //Selected Background
        if (mSelectedItems.get(position, false)) {
            holder.wifiBackground.setBackgroundResource(R.color.colorHighlight);
        } else {
            holder.wifiBackground.setBackgroundResource(R.drawable.wifi_entry_bg);
        }

        //Drag Icon
        holder.dragHandler.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    mDragListener.onStartDrag(holder);
                }
                return false;
            }
        });

        //Drag Handler Visibility
        if (mShowDragHandler) {
            holder.dragHandler.setVisibility(View.VISIBLE);

        } else {
            holder.dragHandler.setVisibility(View.GONE);
        }


        //Set Animation
//        if (position > mPreviousPosition) {
//            AnimationUtils.translateY(holder, true);
//
//        } else {
//            AnimationUtils.translateY(holder, false);
//        }
//        mPreviousPosition = position;

    }

    @Override
    public int getItemCount() {
        return mListWifi.size();
    }


    public void setWifiList(ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "setWifiList");
        mListWifi = listWifi; //new ArrayList<>(listWifi);
        mPreviousPosition = -1; //fix load animation on new data loaded
        notifyDataSetChanged();
    }

    /*******************************************************/
    /************ Contextual Action Mode Methods ***********/
    /*******************************************************/

    public void toggleSelection(int position) {
        Log.d(TAG, "toggleSelection");
        if (mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        } else {
            mSelectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        Log.d(TAG, "clearSelection");
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getSelectedItems() {
        Log.d(TAG, "getSelectedItems");
        ArrayList<Integer> items = new ArrayList<>(mSelectedItems.size());

        for (int i = 0; i < mSelectedItems.size(); i++) {
            items.add(mSelectedItems.keyAt(i));
        }
        return items;
    }

    /**********************************************/
    /************ Items Changed Methods ***********/
    /**********************************************/

    public WifiEntry removeItem(int position) {
        Log.d(TAG, "removeItem - position = " + position);
        final WifiEntry entry = mListWifi.remove(position);

        if(mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        }

        notifyItemRemoved(position);
        return entry;
    }

    public void addItem(int position, WifiEntry entry) {
        Log.d(TAG, "addItem - position = " + position);
        mListWifi.add(position, entry);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        Log.d(TAG, "moveItem - from " + fromPosition + " to " + toPosition);
        final WifiEntry entry = mListWifi.remove(fromPosition);
        mListWifi.add(toPosition, entry);
        notifyItemMoved(fromPosition, toPosition);
    }


    /*********************************************/
    /************ Animate Search Query ***********/
    /*********************************************/

    public void animateTo(ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "animateTo");
        //Order is important
        applyAndAnimateRemovals(listWifi);
        applyAndAnimateAdditions(listWifi);
        applyAndAnimateMovedItems(listWifi);
    }

    private void applyAndAnimateRemovals(ArrayList<WifiEntry> newListWifi) {
        Log.d(TAG, "applyAndAnimateRemovals");
        for (int i = mListWifi.size() - 1; i >= 0; i--) {
            final WifiEntry entry = mListWifi.get(i);
            if (!newListWifi.contains(entry)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<WifiEntry> newListWifi) {
        Log.d(TAG, "applyAndAnimateAdditions");
        for (int i = 0, count = newListWifi.size(); i < count; i++) {
            final WifiEntry entry = newListWifi.get(i);
            if (!mListWifi.contains(entry)) {
                addItem(i, entry);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<WifiEntry> newListWifi) {
        Log.d(TAG, "applyAndAnimateMovedItems");
        for (int toPosition = newListWifi.size() - 1; toPosition >= 0; toPosition--) {
            final WifiEntry entry = newListWifi.get(toPosition);
            final int fromPosition = mListWifi.indexOf(entry);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    /*****************************************************/
    /************** ItemTouchHelper Methods***************/
    /*****************************************************/

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mListWifi, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mListWifi, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);

        return true;
    }

    @Override
    public WifiEntry onItemDismiss(int position) {
        Log.d(TAG, "onItemDismiss");
        return removeItem(position);
    }

    public void showDragHandler(boolean show) {
        mShowDragHandler = show;
        notifyDataSetChanged();
    }


    /*****************************************/
    /********** View Holder Sub-Class ********/
    /*****************************************/

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView wifiTitle;
        private TextView wifiPassword;
        private ImageView dragHandler;
        private LinearLayout wifiBackground;

        public MyViewHolder(View itemView) {
            super(itemView);

            wifiTitle = (TextView) itemView.findViewById(R.id.title_wifi);
            wifiPassword = (TextView) itemView.findViewById(R.id.password_wifi);
            dragHandler = (ImageView) itemView.findViewById(R.id.drag_handler);
            wifiBackground = (LinearLayout) itemView.findViewById(R.id.wifi_entry_layout);

        }
    }
}
