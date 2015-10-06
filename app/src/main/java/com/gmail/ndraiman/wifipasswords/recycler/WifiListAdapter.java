package com.gmail.ndraiman.wifipasswords.recycler;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
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
    private Context mContext;
    private SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    public WifiListAdapter(Context context, ItemDragListener dragListener) {
        layoutInflater = LayoutInflater.from(context);
        mContext = context;
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

        holder.mTitle.setText(currentEntry.getTitle());
        holder.mPassword.setText(currentEntry.getPassword());
        holder.mTag.setText(currentEntry.getTag());

        //Selected Background
        if (mSelectedItems.get(position, false)) {
            holder.mBackground.setBackgroundResource(R.color.colorHighlight);
        } else {
            holder.mBackground.setBackgroundResource(R.drawable.wifi_entry_bg);
        }

        //Drag Icon
        holder.mDragHandler.setOnTouchListener(new View.OnTouchListener() {
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
            holder.mDragHandler.setVisibility(View.VISIBLE);
            holder.mTagLayout.setVisibility(View.GONE);

        } else {
            holder.mDragHandler.setVisibility(View.GONE);

            if(!holder.mTag.getText().toString().isEmpty()) {
                holder.mTagLayout.setVisibility(View.VISIBLE);
            }
        }

        //Set Animation
        setAnimation(holder.mContainer, position);

    }


    @Override
    public int getItemCount() {
        return mListWifi.size();
    }


    public void setWifiList(ArrayList<WifiEntry> listWifi) {
        Log.d(TAG, "setWifiList");
        mListWifi = listWifi;
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


    private void setAnimation(View viewToAnimate, int position)
    {
        // If the bound view wasn't previously displayed on screen, it's animated
        if (position > mPreviousPosition)
        {
            //Alternating Slide Animation - Glitchy when fast scrolling
//            Animation animation = android.view.animation.AnimationUtils
//                    .loadAnimation(mContext,
//                            position%2 == 0 ? R.anim.slide_up_left : R.anim.slide_up_right);
//            viewToAnimate.startAnimation(animation);

            AnimationUtils.translateY(viewToAnimate, true);
            mPreviousPosition = position;
        }
    }


    /*****************************************/
    /********** View Holder Sub-Class ********/
    /*****************************************/

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mPassword;
        private ImageView mDragHandler;
        private LinearLayout mBackground;
        private CardView mContainer;
        private LinearLayout mTagLayout;
        private TextView mTag;

        public MyViewHolder(View itemView) {
            super(itemView);

            mTitle = (TextView) itemView.findViewById(R.id.title_wifi);
            mPassword = (TextView) itemView.findViewById(R.id.password_wifi);
            mDragHandler = (ImageView) itemView.findViewById(R.id.drag_handler);
            mBackground = (LinearLayout) itemView.findViewById(R.id.wifi_entry_layout);
            mContainer = (CardView) itemView.findViewById(R.id.wifi_entry_container);
            mTagLayout = (LinearLayout) itemView.findViewById(R.id.tag_wifi_layout);
            mTag = (TextView) itemView.findViewById(R.id.tag_wifi_text);

        }
    }
}
