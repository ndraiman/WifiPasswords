package com.gmail.ndraiman.wifipasswords.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.activities.SettingsActivity;


public class CustomAlertDialogFragment extends DialogFragment {

    private static final String MESSAGE_KEY = "alert_dialog_message";
    private static final String TITLE_KEY = "alert_dialog_title";
    private static final String HAS_BUTTONS_KEY = "alert_dialog_buttons";
    private static final int BUTTON_SETTINGS = AlertDialog.BUTTON_POSITIVE;
    private static final int BUTTON_DISMISS = AlertDialog.BUTTON_NEGATIVE;
    private static final String LOG_TAG = "CustomAlertDialog";
    private static final int SETTINGS_ACTIVITY_RESULT_CODE = 15;
    private DialogListener mListener;

    public static CustomAlertDialogFragment getInstance(String title, String message, boolean hasButtons) {
        CustomAlertDialogFragment fragment = new CustomAlertDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        args.putString(MESSAGE_KEY, message);
        args.putBoolean(HAS_BUTTONS_KEY, hasButtons);
        fragment.setArguments(args);

        return fragment;
    }

    public CustomAlertDialogFragment() {

    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        String title = bundle.getString(TITLE_KEY);
        String message = bundle.getString(MESSAGE_KEY);
        boolean hasButtons = bundle.getBoolean(HAS_BUTTONS_KEY);


        mListener = new DialogListener();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomErrorDialog);
        builder.setMessage(message)
                .setTitle(title);

        if(hasButtons) {
            builder.setPositiveButton(R.string.error_settings_button, mListener)
                    .setNegativeButton(R.string.error_dismiss_button, mListener);
        }

         return builder.create();
    }



    //Listener Class to handle Dialog Button Clicks
    private class DialogListener implements DialogInterface.OnClickListener {


        @Override
        public void onClick(DialogInterface dialog, int which) {


            if(which == BUTTON_SETTINGS) {
                Log.d(LOG_TAG, "Listener - Settings");
                //startActivity(new Intent(getActivity(), SettingsActivity.class));
                getActivity().startActivityForResult(
                        new Intent(getActivity(), SettingsActivity.class),
                        SETTINGS_ACTIVITY_RESULT_CODE);
            }

            if(which == BUTTON_DISMISS) {
                Log.d(LOG_TAG, "Listener - Dismissed");
            }
        }
    }
}
