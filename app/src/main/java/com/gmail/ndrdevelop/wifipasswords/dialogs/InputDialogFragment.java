package com.gmail.ndrdevelop.wifipasswords.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.database.PasswordDB;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;


public class InputDialogFragment extends DialogFragment {

    static final String TYPE_KEY = "type_key";

    public static final String ENTRIES_KEY = "entries_key";
    public static final String POSITIONS_LEY = "positions_key";
    public static final int INPUT_ENTRY = 0;
    public static final int INPUT_TAG = 1;

    @Bind(R.id.input_title) EditText mTitle;
    @Bind(R.id.input_password) EditText mPassword;
    @Bind(R.id.input_confirm) Button mConfirm;
    @Bind(R.id.input_cancel) Button mCancel;
    @Bind(R.id.dialog_add_container) LinearLayout mRoot;


    public static InputDialogFragment getInstance(int type, Bundle listWifiBundle) {
        InputDialogFragment fragment = new InputDialogFragment();

        if (listWifiBundle == null) {
            listWifiBundle = new Bundle();
        }
        listWifiBundle.putInt(TYPE_KEY, type);
        fragment.setArguments(listWifiBundle);

        return fragment;
    }


    public InputDialogFragment() {

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.dialog_input, container);
        ButterKnife.bind(this, layout);

        final Bundle bundle = getArguments();
        int type = bundle.getInt(TYPE_KEY);

        final InputDialogListener listener = (InputDialogListener) getTargetFragment();

        switch (type) {

            case INPUT_ENTRY:

                getDialog().setTitle(R.string.dialog_add_title);
                mConfirm.setOnClickListener(v -> {

                    if (hasErrors())
                        return; //return to dialog

                    listener.onSubmitAddDialog(mTitle.getText().toString(), mPassword.getText().toString());
                    dismiss();
                });

                break;

            case INPUT_TAG:

                getDialog().setTitle(R.string.dialog_tag_title);
                mPassword.setVisibility(View.GONE);
                mTitle.setHint(R.string.dialog_tag_hint);
                mTitle.setImeOptions(EditorInfo.IME_ACTION_GO);
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

                mConfirm.setOnClickListener(v -> {

                    if (mTitle.getText() == null) {
                        return; //return to dialog
                    }

                    listener.onSubmitTagDialog(mTitle.getText().toString(),
                            listWifi, indexWifi);
                    dismiss();
                });

                break;

        }

        mCancel.setOnClickListener(v -> dismiss());


        return layout;
    }


    @Override
    public void onResume() {
        super.onResume();

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
                    .setAction(R.string.snackbar_dismiss, v -> {
                        //Dismisses Snackbar
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
                    .setAction(R.string.snackbar_dismiss, v -> {
                        //Dismisses Snackbar
                    }).show();
        }

        return hasError;
    }
}
