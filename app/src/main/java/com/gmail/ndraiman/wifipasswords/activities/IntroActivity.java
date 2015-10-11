package com.gmail.ndraiman.wifipasswords.activities;


import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.gmail.ndraiman.wifipasswords.R;

public class IntroActivity extends AppIntro2 {

    private static final String TAG = "IntroActivity";

    @Override
    public void init(Bundle savedInstanceState) {
        Log.d(TAG, "init");

        addSlide(AppIntroFragment.newInstance("Welcome to WifiPasswords", "Let's walkthrough the app features", R.drawable.collapsing_header, ContextCompat.getColor(this, R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("Root Permission Required", "Please grant Root Access when prompted", R.drawable.collapsing_header, ContextCompat.getColor(this, R.color.colorAccent)));
        addSlide(AppIntroFragment.newInstance("Data Security", "While sharing wifi passwords is available through this app, please take care of doing so as it might comprise your network's security", R.drawable.collapsing_header, ContextCompat.getColor(this, android.R.color.black)));
        addSlide(AppIntroFragment.newInstance("Quick Copy", "Double Tap a wifi entry to quickly copy its data to clipboard", R.drawable.collapsing_header, ContextCompat.getColor(this, R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("Context Action Bar", "Long Click a Wifi Entry to bring it up", R.drawable.collapsing_header, ContextCompat.getColor(this, R.color.colorPrimaryDark)));


        setDepthAnimation();
    }


    @Override
    public void onDonePressed() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}
