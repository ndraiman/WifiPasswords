package com.gmail.ndrdevelop.wifipasswords.activities;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.task.TaskCheckPasscode;

public class IntroActivity extends AppIntro2 {


    @Override
    public void init(Bundle savedInstanceState) {

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_welcome),
                getString(R.string.intro_message_welcome),
                R.drawable.intro_app_icon,
                ContextCompat.getColor(this, R.color.colorPrimary)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_disclaimer),
                getString(R.string.intro_message_disclaimer),
                R.drawable.intro_disclaimer,
                ContextCompat.getColor(this, R.color.colorAccent)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_root),
                getString(R.string.intro_message_root),
                R.drawable.intro_root_permission,
                ContextCompat.getColor(this, R.color.colorPrimaryDark)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_quick_copy),
                getString(R.string.intro_message_quick_copy),
                R.drawable.intro_quick_copy,
                ContextCompat.getColor(this, R.color.colorAccent)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_cab),
                getString(R.string.intro_message_cab),
                R.drawable.intro_cab,
                ContextCompat.getColor(this, R.color.colorBlue900)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_thank_you),
                "",
                R.drawable.intro_thank_you,
                ContextCompat.getColor(this, R.color.colorPrimary)));


        setDepthAnimation();

        //lock to Portrait Orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    @Override
    public void onDonePressed() {
//        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MyApplication.FIRST_LAUNCH, false).apply();
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(MyApplication.mPasscodeActivated && !isFinishing()) {

            new TaskCheckPasscode(getApplicationContext()).execute();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(MyApplication.mPasscodeActivated && MyApplication.mAppWentBackground) {
            startActivity(new Intent(this, PasscodeActivity.class));
        }
    }
}
