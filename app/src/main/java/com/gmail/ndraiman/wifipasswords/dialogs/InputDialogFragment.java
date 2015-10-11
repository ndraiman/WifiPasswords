package com.gmail.ndraiman.wifipasswords.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.extras.MyApplication;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;


public class InputDialogFragment extends DialogFragment {

    private static final String TAG = "InputDialogFragment";
    private static final String TYPE_KEY = "type_key";

    public static final String ENTRIES_KEY = "entries_key";
    public static final String POSITIONS_LEY = "positions_key";

    public static final int INPUT_ENTRY = 0;
    public static final int INPUT_TAG = 1;

    private EditText mTitle, mPassword;
    private Button mCancel, mConfirm;
    private LinearLayout mRoot;

    public static InputDialogFragment getInstance(int type, Bundle bundle) {
        InputDialogFragment fragment = new InputDialogFragment();

        if (bundle == null) {
            Log.d(TAG, "getInstance() called with bundle = null");
            bundle = new Bundle();
        }
        bundle.putInt(TYPE_KEY, type);
        fragment.setArguments(bundle);

        return fragment;
    }


    public InputDialogFragment() {

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View layout = inflater.inflate(R.layout.dialog_input, container);

        final Bundle bundle = getArguments();
        int type = bundle.getInt(TYPE_KEY);
        Log.d(TAG, "type = " + type);

        mTitle = (EditText) layout.findViewById(R.id.input_title);
        mPassword = (EditText) layout.findViewById(R.id.input_password);
        mConfirm = (Button) layout.findViewById(R.id.input_confirm);
        mCancel = (Button) layout.findViewById(R.id.input_cancel);
        mRoot = (LinearLayout) layout.findViewById(R.id.dialog_add_container);

        final InputDialogListener listener = (InputDialogListener) getTargetFragment();

        switch (type) {

            case INPUT_ENTRY:
                Log.d(TAG, "type = INPUT_ENTRY");
                getDialog().setTitle(R.string.dialog_add_title);
                mConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "mConfirm - onClick");

                        if (hasErrors())
                            return; //return to dialog

                        listener.onSubmitAddDialog(mTitle.getText().toString(), mPassword.getText().toString());
                        dismiss();
                    }
                });

                break;

            case INPUT_TAG:
                Log.d(TAG, "type = INPUT_TAG");
                getDialog().setTitle(R.string.dialog_tag_title);
                mPassword.setVisibility(View.GONE);
                mTitle.setHint(R.string.dialog_tag_hint);
                TextInputLayout input = (TextInputLayout) layout.findViewById(R.id.input_title_layout);
                input.setHint("");
                mConfirm.setText(R.string.dialog_tag_button);

                final ArrayList<WifiEntry> listWifi = bundle.getParcelableArrayList(ENTRIES_KEY);
                final ArrayList<Integer> indexWifi = bundle.getIntegerArrayList(POSITIONS_LEY);

                //if single entry then add current tag to InputDialog text field
                if (listWifi != null && listWifi.size() == 1) {
                    mTitle.setText(listWifi.get(0).getTag());
                    mTitle.setSelection(mTitle.getText().toString().length());
                }

                mConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "mConfirm - onClick");

                        if (mTitle.getText() == null) {
                            Log.e(TAG, "mTitle.getText() = null");
                            return; //return to dialog
                        }

                        listener.onSubmitTagDialog(mTitle.getText().toString(),
                                listWifi, indexWifi);
                        dismiss();
                    }
                });

                break;

        }

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mCancel - onClick");
                dismiss();
            }
        });


        return layout;
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        //Set Dialog Dimensions
        Window dialogWindow = getDialog().getWindow();
        dialogWindow.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        dialogWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
    }


    private boolean hasErrors() {
        boolean hasError = false;

        boolean isEmptyTitle = mTitle.getText() == null
                || mTitle.getText().toString().isEmpty();
        boolean isEmptyPassword = mPassword.getText() == null
                || mPassword.getText().toString().isEmpty();

        if (isEmptyTitle && isEmptyPassword) {
            Snackbar.make(mRoot, getString(R.string.dialog_add_error_empty), Snackbar.LENGTH_SHORT)
                    .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
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

        } else if (isEmptyPassword) {
            mTitle.setError(null);
            mPassword.setError(getString(R.string.dialog_add_password_empty));
            hasError = true;

        } else {

            String title = mTitle.getText().toString().toLowerCase();

            PasswordDB db = MyApplication.getWritableDatabase();
            String whereClause = PasswordDB.PasswordHelper.COLUMN_TITLE + " = ?";
            String[] whereArgs = new String[]{title};
            ArrayList<WifiEntry> checkList = db.getWifiEntries(whereClause, whereArgs, false);
            MyApplication.closeDatabase();

            for (WifiEntry entry : checkList) {
                if (entry.getTitle().toLowerCase().equals(title)) {
                    hasError = true;
                    break; //no need to check further
                }
            }

            Snackbar.make(mRoot, R.string.snackbar_wifi_exists, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Dismisses Snackbar
                        }
                    }).show();
        }

        return hasError;
    }
}
