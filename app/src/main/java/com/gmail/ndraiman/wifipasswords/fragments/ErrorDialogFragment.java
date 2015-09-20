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


public class ErrorDialogFragment extends DialogFragment {

    private static final String MESSAGE_KEY = "error_dialog_message";
    private static final String ERROR_TITLE = "Error...";
    private static final int BUTTON_SETTINGS = -1;
    private static final int BUTTON_DISMISS = -2;
    private static final String LOG_TAG = "ErrorDialog";
    private DialogListener mListener;

    public static ErrorDialogFragment getInstance(String message) {
        ErrorDialogFragment fragment = new ErrorDialogFragment();

        Bundle args = new Bundle();
        args.putString(MESSAGE_KEY, message);
        fragment.setArguments(args);

        return fragment;
    }

    public ErrorDialogFragment() {

    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString(MESSAGE_KEY);
        mListener = new DialogListener();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.CustomErrorDialog);
        builder.setMessage(message)
                .setTitle(ERROR_TITLE)
                .setPositiveButton(R.string.error_settings_button, mListener)
                .setNegativeButton(R.string.error_dismiss_button, mListener);

         return builder.create();
    }


    private class DialogListener implements DialogInterface.OnClickListener {


        @Override
        public void onClick(DialogInterface dialog, int which) {


            if(which == BUTTON_SETTINGS) {
                Log.d(LOG_TAG, "Listener - Settings");
                startActivity(new Intent(getActivity(), SettingsActivity.class));
            }

            if(which == BUTTON_DISMISS) {
                Log.d(LOG_TAG, "Listener - Dismissed");
            }
        }
    }
}
