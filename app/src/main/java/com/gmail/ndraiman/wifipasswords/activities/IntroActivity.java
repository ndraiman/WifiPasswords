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

        addSlide(AppIntroFragment.newInstance("test title 1", "test desc 1", R.drawable.collapsing_header, ContextCompat.getColor(this, R.color.colorPrimary)));
        addSlide(AppIntroFragment.newInstance("test title 2", "test desc 2", R.drawable.ic_settings, ContextCompat.getColor(this, R.color.colorAccent)));

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
