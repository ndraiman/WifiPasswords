package com.gmail.ndraiman.wifipasswords.pojo;

import android.os.Parcel;
import android.os.Parcelable;

import com.gmail.ndraiman.wifipasswords.extras.L;

/**
 * Created by ND88 on 09/09/2015.
 */
public class WifiEntry implements Parcelable {

    private String title;
    private String password;

    public WifiEntry() {}

//    public WifiEntry(String title, String password) {
//        this.title = title;
//        this.password = password;
//    }



    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /************************************************************/
    //Parcelable Implementation

    public static final Creator<WifiEntry> CREATOR = new Creator<WifiEntry>() {
        @Override
        public WifiEntry createFromParcel(Parcel in) {
            L.m("create from parcel :WifiEntry");
            return new WifiEntry(in);
        }

        @Override
        public WifiEntry[] newArray(int size) {
            return new WifiEntry[size];
        }
    };

    //Parcel Constructor
    protected WifiEntry(Parcel in) {
        title = in.readString();
        password = in.readString();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(password);
    }
}
