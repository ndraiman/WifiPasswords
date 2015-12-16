package com.gmail.ndrdevelop.wifipasswords.dialogs;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.extras.RequestCodes;


public class CustomAlertDialogFragment extends DialogFragment {

    static final String MESSAGE_KEY = "alert_dialog_message";
    static final String TITLE_KEY = "alert_dialog_title";
    static final String BUTTONS_KEY = "alert_dialog_buttons";

    public static CustomAlertDialogFragment getInstance(String title, String message, String... buttons) {
        CustomAlertDialogFragment fragment = new CustomAlertDialogFragment();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        args.putString(MESSAGE_KEY, message);
        args.putStringArray(BUTTONS_KEY, buttons);
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
        String[] buttons = bundle.getStringArray(BUTTONS_KEY);

        AlertDialog.Builder builder;

        if (MyApplication.sIsDark) {
            builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme_Dark);
        } else {
            builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        }

        builder.setMessage(message)
                .setTitle(title);

        if(buttons != null && buttons.length > 0) {

            //Send Result Codes to target fragment according to button clicked
            builder.setPositiveButton(buttons[0], (dialog, which) -> {
                sendResult(RequestCodes.DIALOG_CONFIRM, null);
            });

            if(buttons.length > 1) {
                builder.setNegativeButton(buttons[1], (dialog, which) -> {
                    sendResult(RequestCodes.DIALOG_CANCEL, null);
                });
            }
        }
         return builder.create();
    }



    private void sendResult(int resultCode, Intent intent) {

        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }

}
