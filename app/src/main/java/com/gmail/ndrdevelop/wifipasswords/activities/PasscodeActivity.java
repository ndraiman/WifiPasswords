package com.gmail.ndrdevelop.wifipasswords.activities;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.andexert.library.RippleView;
import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RequestCodes;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;


public class PasscodeActivity extends AppCompatActivity {

    int mRequestCode;

    @Bind(R.id.edit_text_passcode) EditText mEditTextPasscode;
    String mPasscode;

    int mAttemptNum;
    final int ATTEMPT_CHANGE = -1;
    final int ATTEMPT_ENABLE = 0;
    final int ATTEMPT_ENABLE_REENTER = 1;

    TextWatcher mPasscodeListener;


    @Bind(R.id.layout_radio_buttons) LinearLayout mRadioButtonLayout;
    @Bind(R.id.passcode_description) TextView mDescription;
    @Bind(R.id.passcode_logo) ImageView mLogo;

    @Bind({R.id.radioButton1,
            R.id.radioButton2,
            R.id.radioButton3,
            R.id.radioButton4})
    List<RadioButton> mRadioButtonList;

    @Bind({R.id.radioButtonError1,
            R.id.radioButtonError2,
            R.id.radioButtonError3,
            R.id.radioButtonError4})
    List<RadioButton> mRadioErrorList;

    @Bind({R.id.button_1,
            R.id.button_2,
            R.id.button_3,
            R.id.button_4,
            R.id.button_5,
            R.id.button_6,
            R.id.button_7,
            R.id.button_8,
            R.id.button_9,
            R.id.button_0,
            R.id.button_backspace})
    List<RippleView> mDigitButtonList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.sIsDark) {
            setTheme(R.style.AppTheme_Dark);
        }
        super.onCreate(savedInstanceState);

        if (MyApplication.sIsDark) {
            setContentView(R.layout.activity_passcode_dark);
        } else {
            setContentView(R.layout.activity_passcode);
        }

        ButterKnife.bind(this);

        mRequestCode = getIntent().getIntExtra(MyApplication.PASSCODE_REQUEST_CODE, RequestCodes.PASSCODE_ACTIVITY);

        mPasscode = PreferenceManager.getDefaultSharedPreferences(this).getString(MyApplication.PASSCODE_KEY, "0000");

        if (mRequestCode == RequestCodes.PASSCODE_PREF_ENABLE || mRequestCode == RequestCodes.PASSCODE_PREF_CHANGE) {
            mAttemptNum = ATTEMPT_ENABLE;

            if (mRequestCode == RequestCodes.PASSCODE_PREF_CHANGE) {
                mAttemptNum = ATTEMPT_CHANGE;
                mDescription.setText(R.string.passcode_description_old);
            }

            setupEnablePasscodeListener();

        } else {
            setupCorrectPasscodeListener();

        }

        mEditTextPasscode.addTextChangedListener(mPasscodeListener);

        //lock to Portrait Orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onPause() {
        super.onPause();

        MyApplication.sShouldAutoUpdateList = true;
    }

    @Override
    public void onBackPressed() {
        if (mRequestCode == RequestCodes.PASSCODE_ACTIVITY) {
            return;
        }
        super.onBackPressed();
    }



    /********************************************************/
    /****************** Additional Methods ******************/
    /********************************************************/

    private void setupCorrectPasscodeListener() {

        mPasscodeListener = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (count > before) { //digit added

                    switch (count) {

                        case 1:
                            mRadioButtonList.get(0).setChecked(true);
                            break;

                        case 2:
                            mRadioButtonList.get(1).setChecked(true);
                            break;

                        case 3:
                            mRadioButtonList.get(2).setChecked(true);
                            break;

                        case 4:
                            mRadioButtonList.get(3).setChecked(true);
                            break;
                    }

                } else { //digit subtracted

                    switch (before) {

                        case 1:
                            mRadioButtonList.get(0).setChecked(false);
                            break;

                        case 2:
                            mRadioButtonList.get(1).setChecked(false);
                            break;

                        case 3:
                            mRadioButtonList.get(2).setChecked(false);
                            break;
                    }
                }

                if (s.length() == 4) {

                    if (submit(s.toString())) {
                        setResult(RESULT_OK);

                        if (mRequestCode == RequestCodes.PASSCODE_PREF_DISABLE) {
                            disablePasscode();
                        }

                        finish();

                    } else {
                        wrongPasscode();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        };

    }

    private void setupEnablePasscodeListener() {

        mPasscodeListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (count > before) { //digit added

                    switch (count) {

                        case 1:
                            mRadioButtonList.get(0).setChecked(true);
                            break;

                        case 2:
                            mRadioButtonList.get(1).setChecked(true);
                            break;

                        case 3:
                            mRadioButtonList.get(2).setChecked(true);
                            break;

                        case 4:
                            mRadioButtonList.get(3).setChecked(true);
                            break;
                    }

                } else { //digit subtracted

                    switch (before) {

                        case 1:
                            mRadioButtonList.get(0).setChecked(false);
                            break;

                        case 2:
                            mRadioButtonList.get(1).setChecked(false);
                            break;

                        case 3:
                            mRadioButtonList.get(2).setChecked(false);
                            break;
                    }
                }

                if (s.length() == 4) {

                    if (mAttemptNum == ATTEMPT_CHANGE) {

                        if(submit(s.toString())) {
                            clearRadioButtons();
                            mDescription.setText(R.string.passcode_description);
                            mEditTextPasscode.setText("");
                            mAttemptNum = ATTEMPT_ENABLE;

                        } else {
                            wrongPasscode();

                        }

                    } else if (mAttemptNum == ATTEMPT_ENABLE) {
                        clearRadioButtons();
                        mDescription.setText(R.string.passcode_description_reenter);
                        mPasscode = s.toString();
                        mEditTextPasscode.setText("");
                        mAttemptNum = ATTEMPT_ENABLE_REENTER;

                    } else {

                        if (submit(s.toString())) {
                            enablePasscode();
                            setResult(RESULT_OK);
                            finish();

                        } else {
                            wrongPasscode();
                            mPasscode = "";
                            mAttemptNum = ATTEMPT_ENABLE;

                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        };
    }

    private void enablePasscode() {

        MyApplication.mPasscodeActivated = true;

        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(PasscodeActivity.this).edit();

        prefsEditor.putBoolean(MyApplication.PASSCODE_STATE, true);
        prefsEditor.putString(MyApplication.PASSCODE_KEY, mPasscode);
        prefsEditor.apply();

    }


    private void disablePasscode() {

        MyApplication.mPasscodeActivated = false;

        SharedPreferences.Editor prefsEditor = PreferenceManager.getDefaultSharedPreferences(PasscodeActivity.this).edit();

        prefsEditor.putBoolean(MyApplication.PASSCODE_STATE, false);
        prefsEditor.putString(MyApplication.PASSCODE_KEY, "");
        prefsEditor.apply();

    }

    private boolean submit(String passcodeInserted) {

        return mPasscode.equals(passcodeInserted);
    }


    private void wrongPasscode() {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        shake.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                digitsClickable(false);

                clearRadioButtons();

                showErrorRadioButtons(true);

                mLogo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.passcode_logo_error, getTheme()));
                mDescription.setTextColor(ContextCompat.getColor(PasscodeActivity.this, R.color.colorRed500));
                mDescription.setText(R.string.passcode_description_error);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                digitsClickable(true);

                showErrorRadioButtons(false);

                mLogo.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.passcode_logo, getTheme()));

                if (MyApplication.sIsDark) {
                    mDescription.setTextColor(ContextCompat.getColor(PasscodeActivity.this, R.color.colorWhite));
                } else {
                    mDescription.setTextColor(ContextCompat.getColor(PasscodeActivity.this, R.color.colorPrimary));
                }

                if(mAttemptNum == ATTEMPT_CHANGE) {
                    mDescription.setText(R.string.passcode_description_old);
                } else {
                    mDescription.setText(R.string.passcode_description);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mEditTextPasscode.setText("");
        mRadioButtonLayout.startAnimation(shake);
    }


    public void buttonClicked(View view) {

        String current = mEditTextPasscode.getText().toString();
        int id = view.getId();

        switch (id) {

            case R.id.button_1:
                current += "1";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_2:
                current += "2";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_3:
                current += "3";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_4:
                current += "4";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_5:
                current += "5";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_6:
                current += "6";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_7:
                current += "7";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_8:
                current += "8";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_9:
                current += "9";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_0:
                current += "0";
                mEditTextPasscode.setText(current);
                break;

            case R.id.button_backspace:
                if (current.length() > 0) {
                    current = current.substring(0, current.length() - 1);
                    mEditTextPasscode.setText(current);
                }
                break;

        }
    }


    private void digitsClickable(boolean enable) {

        ButterKnife.apply(mDigitButtonList, ENABLED, enable);

    }


    private void clearRadioButtons() {

        ButterKnife.apply(mRadioButtonList, CHECKED, false);
    }

    private void showErrorRadioButtons(boolean show) {

        if (show) {

            ButterKnife.apply(mRadioButtonList, VISIBILITY, View.GONE);
            ButterKnife.apply(mRadioErrorList, VISIBILITY, View.VISIBLE);

        } else {

            ButterKnife.apply(mRadioButtonList, VISIBILITY, View.VISIBLE);
            ButterKnife.apply(mRadioErrorList, VISIBILITY, View.GONE);
        }

    }


    /*********************************************************/
    /****************** ButterKnife Methods ******************/
    /*********************************************************/
    static final ButterKnife.Setter<View, Boolean> ENABLED = (view, value, index) -> view.setEnabled(value);

    static final ButterKnife.Setter<RadioButton, Boolean> CHECKED = (view, value, index) -> view.setChecked(value);

    static final ButterKnife.Setter<View, Integer> VISIBILITY = (view, value, index) -> view.setVisibility(value);
}
