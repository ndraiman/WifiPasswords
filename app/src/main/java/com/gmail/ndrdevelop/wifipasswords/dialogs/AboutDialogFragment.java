package com.gmail.ndrdevelop.wifipasswords.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.activities.IntroActivity;


public class AboutDialogFragment extends DialogFragment {


    public static AboutDialogFragment getInstance() {
        return new AboutDialogFragment();
    }

    public AboutDialogFragment() {

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
        TextView libraryRippleEffect = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_library_rippleeffect);
        TextView helpIntro = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_help_intro);
        TextView helpReadme = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_help_readme);
        TextView helpEmail = (TextView) helpDialogLayout.findViewById(R.id.dialog_about_help_feedback);

        info.setMovementMethod(LinkMovementMethod.getInstance());
        github.setMovementMethod(LinkMovementMethod.getInstance());
        libraryProgressBar.setMovementMethod(LinkMovementMethod.getInstance());
        libraryAppIntro.setMovementMethod(LinkMovementMethod.getInstance());
        libraryRippleEffect.setMovementMethod(LinkMovementMethod.getInstance());
        helpReadme.setMovementMethod(LinkMovementMethod.getInstance());
        helpEmail.setMovementMethod(LinkMovementMethod.getInstance());

        helpIntro.setOnClickListener(v -> startActivity(new Intent(v.getContext(), IntroActivity.class)));

        String versionName;

        try {
            versionName = version.getText().toString() + " " + parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = getString(R.string.dialog_about_version_error);
        }

        version.setText(versionName);


        builder.setTitle(getString(R.string.dialog_about_title));
        builder.setView(helpDialogLayout)
                .setPositiveButton(R.string.dialog_about_button, (dialog, which) -> {
                    //do nothing
                });

        return builder.create();
    }
}
