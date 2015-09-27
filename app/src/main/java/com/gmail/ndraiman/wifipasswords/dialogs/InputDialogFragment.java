package com.gmail.ndraiman.wifipasswords.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gmail.ndraiman.wifipasswords.R;


public class InputDialogFragment extends DialogFragment {

    private static final String LOG_TAG = "InputDialogFragment";
    private EditText mTitle, mPassword;
    private Button mCancel, mConfirm;
    private LinearLayout mRoot;

    public static InputDialogFragment getInstance() {

        return new InputDialogFragment();
    }


    public InputDialogFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");
        View layout = inflater.inflate(R.layout.dialog_add_entry, container);

        mTitle = (EditText) layout.findViewById(R.id.input_title);
        mPassword = (EditText) layout.findViewById(R.id.input_password);
        mConfirm = (Button) layout.findViewById(R.id.input_confirm);
        mCancel = (Button) layout.findViewById(R.id.input_cancel);
        mRoot = (LinearLayout) layout.findViewById(R.id.dialog_add_container);

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "mConfirm - onClick");

                if (hasErrors())
                    return; //return to dialog

                InputDialogListener listener = (InputDialogListener) getTargetFragment();
                listener.onSubmitInputDialog(mTitle.getText().toString(), mPassword.getText().toString());
                dismiss();
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "mCancel - onClick");
                dismiss();
            }
        });



        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");

        //Set Dialog Dimensions
        getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private boolean hasErrors() {
        boolean hasError = false;

        boolean isEmptyTitle = mTitle.getText() == null
                || mTitle.getText().toString().isEmpty();
        boolean isEmptyPassword = mPassword.getText() == null
                || mPassword.getText().toString().isEmpty();

        if(isEmptyTitle && isEmptyPassword) {
            Snackbar.make(mRoot, getString(R.string.dialog_add_error_empty), Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.snackbar_dismiss), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Dismisses Snackbar
                        }
                    }).show();
            hasError = true;

        } else if (isEmptyTitle) {
            mTitle.setError(getString(R.string.dialog_add_title_empty));
            mPassword.setError(null);
            hasError = true;

        } else if (isEmptyPassword){
            mTitle.setError(null);
            mPassword.setError(getString(R.string.dialog_add_password_empty));
            hasError = true;

        }

        return hasError;
    }
}
