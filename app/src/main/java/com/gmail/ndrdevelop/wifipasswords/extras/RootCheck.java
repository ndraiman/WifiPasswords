package com.gmail.ndrdevelop.wifipasswords.extras;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;


public class RootCheck {
    
    private static final String TAG = "ROOT";

    /***********************************************************************/
    //Root Check method
    //Credit: http://muzikant-android.blogspot.co.il/2011/02/how-to-get-root-access-and-execute.html

    /***********************************************************************/
    public static boolean canRunRootCommands() {
        boolean retval = false;
        Process suProcess;

        try {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (null != os && null != osRes) {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                boolean exitSu = false;
                if (null == currUid) {
                    retval = false;
                    exitSu = false;
                    Log.d(TAG, "Can't get root access or denied by user");
                } else if (true == currUid.contains("uid=0")) {
                    retval = true;
                    exitSu = true;
                    Log.d(TAG, "Root access granted");
                } else {
                    retval = false;
                    exitSu = true;
                    Log.d(TAG, "Root access rejected: " + currUid);
                }

                if (exitSu) {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        } catch (Exception e) {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output stream (os) after su failed, meaning that the device is not rooted

            retval = false;
            Log.d(TAG, "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }

}
