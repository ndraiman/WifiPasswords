package com.gmail.ndraiman.wifipasswords.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.activities.IntroActivity;


public class HelpDialogFragment extends DialogFragment {


    public static HelpDialogFragment getInstance() {
        return new HelpDialogFragment();
    }

    public HelpDialogFragment() {

    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity parent = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(parent, R.style.AlertDialogTheme);

        LayoutInflater inflater = LayoutInflater.from(parent);
        View helpDialogLayout = inflater.inflate(R.layout.dialog_about, null);

        TextView info = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_info);
        TextView version = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_version);
        TextView github = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_github);
        TextView libraryProgressBar = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_library_materialprogressbar);
        TextView libraryAppIntro = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_library_appintro);
        TextView helpIntro = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_help_intro);
        TextView helpReadme = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_help_readme);

        info.setMovementMethod(LinkMovementMethod.getInstance());
        github.setMovementMethod(LinkMovementMethod.getInstance());
        libraryProgressBar.setMovementMethod(LinkMovementMethod.getInstance());
        libraryAppIntro.setMovementMethod(LinkMovementMethod.getInstance());
        helpReadme.setMovementMethod(LinkMovementMethod.getInstance());

        helpIntro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), IntroActivity.class));
            }
        });

        String versionName;

        try {
            versionName = version.getText().toString() + " " + parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = getString(R.string.dialog_about_version_error);
        }

        version.setText(versionName);


        builder.setTitle(getString(R.string.dialog_about_title));
        builder.setView(helpDialogLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                });

        return builder.create();
    }
}
