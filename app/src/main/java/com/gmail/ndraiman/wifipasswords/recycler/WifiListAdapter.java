package com.gmail.ndraiman.wifipasswords.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;
import java.util.List;


public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.MyViewHolder> {

    private LayoutInflater layoutInflater;
    private List<WifiEntry> mListWifi = new ArrayList<>();
    private int mPreviousPosition = 0;

    public WifiListAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);

        //TODO Delete once SQLite database is implemented
        //mListWifi = placeholderData();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View view = layoutInflater.inflate(R.layout.custom_wifi_entry, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        WifiEntry currentEntry = mListWifi.get(position);
        holder.wifiTitle.setText(currentEntry.getTitle());
        holder.wifiPassword.setText(currentEntry.getPassword());

        //Set Animation
        if (position > mPreviousPosition) {
            AnimationUtils.animateSunblind(holder, true);

        } else {
            AnimationUtils.animateSunblind(holder, false);
        }
        mPreviousPosition = position;

    }

    @Override
    public int getItemCount() {
        return mListWifi.size();
    }


    public void setWifiList(ArrayList<WifiEntry> listWifi) {
        mListWifi = listWifi;
        notifyItemRangeChanged(0, listWifi.size());
    }

    //TODO Delete the non-used ViewHolder
    /*****************************************/
    /********** View Holder Sub-Class ********/
    /*****************************************/
    //Without Animation

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView wifiTitle;
        private TextView wifiPassword;

        public MyViewHolder(View itemView) {
            super(itemView);

            wifiTitle = (TextView) itemView.findViewById(R.id.title_wifi);
            wifiPassword = (TextView) itemView.findViewById(R.id.password_wifi);

        }
    }

    /*****************************************************************************/
    //Testing Custom Animation ViewHolder - add "compile 'jp.wasabeef:recyclerview-animators:1.2.0@aar' " to gradle
    /*****************************************************************************/
//    class MyCustomViewHolder extends AnimateViewHolder {
//
//        private TextView wifiTitle;
//        private TextView wifiPassword;
//
//        public MyCustomViewHolder(View itemView) {
//            super(itemView);
//
//            wifiTitle = (TextView) itemView.findViewById(R.id.title_wifi);
//            wifiPassword = (TextView) itemView.findViewById(R.id.password_wifi);
//
//        }
//
//        @Override
//        public void animateAddImpl(ViewPropertyAnimatorListener listener) {
//            ViewCompat.animate(itemView)
//                    .translationY(0)
//                    .alpha(1)
//                    .setDuration(300)
//                    .setListener(listener)
//                    .start();
//        }
//
//        @Override
//        public void animateRemoveImpl(ViewPropertyAnimatorListener listener) {
//            ViewCompat.animate(itemView)
//                    .translationY(-itemView.getHeight() * 0.3f)
//                    .alpha(0)
//                    .setDuration(300)
//                    .setListener(listener)
//                    .start();
//        }
//
//        @Override
//        public void preAnimateAddImpl() {
//            ViewCompat.setTranslationY(itemView, -itemView.getHeight() * 0.3f);
//            ViewCompat.setAlpha(itemView, 0);
//        }
//    }
    /*****************************************************************************/
    /*****************************************************************************/

    /*****************************************/
    /************ PlaceHolder Data ***********/
    /*****************************************/
    public static List<WifiEntry> placeholderData() {

        List<WifiEntry> data = new ArrayList<>();

        String[] titles = {"Wifi 1", "Wifi 2", "Wifi 3", "Wifi 4", "Wifi 5", "Wifi 6", "Wifi 7"
                , "Wifi 8", "Wifi 9", "Wifi 10"};

        String[] passwords = {"Pass 1", "Pass 2", "Pass 3", "Pass 4", "Pass 5", "Pass 6", "Pass 7"
                , "Pass 8", "Pass 9", "Pass 10"};

        for (int i = 0; i < 100; i++) {

            WifiEntry current = new WifiEntry();
            current.setTitle(titles[i % titles.length]);
            current.setPassword(passwords[i % passwords.length]);

            data.add(current);
        }

        return data;
    }
}
