package com.gmail.ndraiman.wifipasswords.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.extras.RequestCodes;


public class CustomAlertDialogFragment extends DialogFragment {

    private static final String MESSAGE_KEY = "alert_dialog_message";
    private static final String TITLE_KEY = "alert_dialog_title";
    private static final String BUTTONS_KEY = "alert_dialog_buttons";

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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
        builder.setMessage(message)
                .setTitle(title);

        if(buttons != null && buttons.length > 0) {

            //Send Result Codes to target fragment according to button clicked
            builder.setPositiveButton(buttons[0], new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sendResult(RequestCodes.DIALOG_CONFIRM, null);
                }
            });

            if(buttons.length > 1) {
                builder.setNegativeButton(buttons[1], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(RequestCodes.DIALOG_CANCEL, null);
                    }
                });
            }
        }
         return builder.create();
    }



    private void sendResult(int resultCode, Intent intent) {

        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }

}
