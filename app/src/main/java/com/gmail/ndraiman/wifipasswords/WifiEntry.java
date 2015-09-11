package com.gmail.ndraiman.wifipasswords;

/**
 * Created by ND88 on 09/09/2015.
 */
public class WifiEntry {

    private String id;
    private String title;
    private String password;

    public WifiEntry() {}

    public WifiEntry(String id, String title, String password) {
        this.id = id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
