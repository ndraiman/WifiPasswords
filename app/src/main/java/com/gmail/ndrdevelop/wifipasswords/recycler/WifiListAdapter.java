package com.gmail.ndrdevelop.wifipasswords.recycler;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.extras.MyAnimationUtils;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.MyViewHolder>
        implements ItemTouchHelperAdapter {

    LayoutInflater layoutInflater;
    List<WifiEntry> mListWifi;
    ItemDragListener mDragListener;
    int mPreviousPosition = -1; //used for Item Animation
    boolean mShowDragHandler;
    boolean isAnimated;
    Context mContext;
    SparseBooleanArray mSelectedItems = new SparseBooleanArray();



    public WifiListAdapter(Context context, boolean isAnimated, ItemDragListener dragListener) {
        layoutInflater = LayoutInflater.from(context);
        mContext = context;
        mDragListener = dragListener;
        mListWifi = new ArrayList<>();
        mShowDragHandler = false;
        this.isAnimated = isAnimated;
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
        holder.mTagText.setText(currentEntry.getTag());


        //Selected Background
        if (mSelectedItems.get(position, false)) {
            holder.mBackground.setBackgroundResource(R.color.colorHighlight);
        } else {
            holder.mBackground.setBackgroundResource(R.drawable.highlight_selected);
        }

        //Drag Icon
        holder.mDragHandler.setOnTouchListener((v, event) -> {
            if (MotionEventCompat.getActionMasked(event) ==
                    MotionEvent.ACTION_DOWN) {
                mDragListener.onStartDrag(holder);
            }
            return false;
        });


        //Tag & Drag Handler Visibility
        toggleTagAndDrag(holder);
        
        //Set Animation
        if(isAnimated) {
            setAnimation(holder.mContainer, position);
        }

    }



    @Override
    public int getItemCount() {
        return mListWifi.size();
    }


    public void setWifiList(ArrayList<WifiEntry> listWifi) {

        mListWifi = listWifi;
        mPreviousPosition = -1; //fix load animation on new data loaded
        notifyDataSetChanged();
    }


    private void toggleTagAndDrag(MyViewHolder holder) {
        if (mShowDragHandler) {
            holder.mDragHandler.setVisibility(View.VISIBLE);
            holder.mDragHandler.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.simple_grow));
            holder.mTagText.setVisibility(View.GONE);

        } else {
            holder.mDragHandler.setVisibility(View.GONE);

            if(!holder.mTagText.getText().toString().replace(" ", "").isEmpty()) {
                holder.mTagText.setVisibility(View.VISIBLE);

            } else {
                holder.mTagText.setVisibility(View.GONE);
            }
        }
    }

    /*******************************************************/
    /************ Contextual Action Mode Methods ***********/
    /*******************************************************/

    public void toggleSelection(int position) {

        if (mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        } else {
            mSelectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {

        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public ArrayList<Integer> getSelectedItems() {

        ArrayList<Integer> items = new ArrayList<>(mSelectedItems.size());

        for (int i = 0; i < mSelectedItems.size(); i++) {
            items.add(mSelectedItems.keyAt(i));
        }
        return items;
    }

    /**********************************************/
    /************ Items Changes Methods ***********/
    /**********************************************/

    public WifiEntry removeItem(int position) {

        final WifiEntry entry = mListWifi.remove(position);

        if(mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        }

        notifyItemRemoved(position);
        return entry;
    }

    public void addItem(int position, WifiEntry entry) {

        mListWifi.add(position, entry);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {

        final WifiEntry entry = mListWifi.remove(fromPosition);
        mListWifi.add(toPosition, entry);
        notifyItemMoved(fromPosition, toPosition);
    }


    /*********************************************/
    /************ Animate Search Query ***********/
    /*********************************************/

    public void animateTo(ArrayList<WifiEntry> listWifi) {
        //Order is important
        applyAndAnimateRemovals(listWifi);
        applyAndAnimateAdditions(listWifi);
        applyAndAnimateMovedItems(listWifi);
    }

    private void applyAndAnimateRemovals(ArrayList<WifiEntry> newListWifi) {

        for (int i = mListWifi.size() - 1; i >= 0; i--) {
            final WifiEntry entry = mListWifi.get(i);
            if (!newListWifi.contains(entry)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<WifiEntry> newListWifi) {

        for (int i = 0, count = newListWifi.size(); i < count; i++) {
            final WifiEntry entry = newListWifi.get(i);
            if (!mListWifi.contains(entry)) {
                addItem(i, entry);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<WifiEntry> newListWifi) {

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

            MyAnimationUtils.translateY(viewToAnimate, true);
            mPreviousPosition = position;
        }
    }


    /*****************************************/
    /********** View Holder Sub-Class ********/
    /*****************************************/

    class MyViewHolder extends RecyclerView.ViewHolder {

        @Bind(R.id.title_wifi) TextView mTitle;
        @Bind(R.id.password_wifi) TextView mPassword;
        @Bind(R.id.drag_handler) ImageView mDragHandler;
        @Bind(R.id.wifi_entry_layout) LinearLayout mBackground;
        @Bind(R.id.wifi_entry_container) CardView mContainer;
        @Bind(R.id.tag_wifi_text) TextView mTagText;

        public MyViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}
