package com.gmail.ndrdevelop.wifipasswords.pojo;

import android.os.Parcel;
import android.os.Parcelable;


public class WifiEntry implements Parcelable {

    public static final Parcelable.Creator<WifiEntry> CREATOR = new Parcelable.Creator<WifiEntry>() {
        @Override
        public WifiEntry createFromParcel(Parcel in) {
            return new WifiEntry(in);
        }

        @Override
        public WifiEntry[] newArray(int size) {
            return new WifiEntry[size];
        }
    };


    private String title;
    private String password;
    private String tag = "";



    public WifiEntry() {}

    public WifiEntry(String title, String password) {
        this.title = title;
        this.password = password;
    }


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

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "Wifi: " + title + ", pass: " + password + ", tag: " + tag + "\n";
    }

    /************************************************************/
    //Parcelable Implementation



    //Parcel Constructor
    protected WifiEntry(Parcel in) {
        title = in.readString();
        password = in.readString();
        tag = in.readString();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(title);
        dest.writeString(password);
        dest.writeString(tag);
    }
}
