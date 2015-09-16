package com.gmail.ndraiman.wifipasswords.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.ndraiman.wifipasswords.DividerItemDecoration;
import com.gmail.ndraiman.wifipasswords.database.PasswordDB;
import com.gmail.ndraiman.wifipasswords.extras.ExecuteAsRootBase;
import com.gmail.ndraiman.wifipasswords.R;
import com.gmail.ndraiman.wifipasswords.RecyclerTouchListener;
import com.gmail.ndraiman.wifipasswords.extras.L;
import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;
import com.gmail.ndraiman.wifipasswords.WifiListAdapter;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import jp.wasabeef.recyclerview.animators.LandingAnimator;
import jp.wasabeef.recyclerview.animators.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {

    private static final String STATE_WIFI_ENTRIES = "state_wifi_entries";
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private WifiListAdapter listAdapter;
    private PasswordDB mDatabase;
    private static TextView textNoData; //TODO implement in a different way
    private ArrayList<WifiEntry> mListWifi = new ArrayList<>();

    //is this App being started for the very first time?
    private boolean mFromSavedInstanceState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        textNoData = (TextView) findViewById(R.id.text_no_data);

        recyclerView = (RecyclerView) findViewById(R.id.wifiList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Setting RecyclerView Design - Divider
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);


        //Animate addition and removal of items
        //TODO Test to see if this works.
        recyclerView.setItemAnimator(new LandingAnimator());
        recyclerView.getItemAnimator().setAddDuration(1000);
        recyclerView.getItemAnimator().setRemoveDuration(1000);
        recyclerView.getItemAnimator().setMoveDuration(1000);
        recyclerView.getItemAnimator().setChangeDuration(1000);


        //Setting RecyclerView Adapter
        listAdapter = new WifiListAdapter(this);

        //Appearance animations for items in RecyclerView.Adapter
        //TODO why doesnt animate recycled items
        //TODO add appropriate line in Gradle build (check github)
        ScaleInAnimationAdapter animationAdapter = new ScaleInAnimationAdapter(listAdapter);
        animationAdapter.setDuration(400);

        recyclerView.setAdapter(animationAdapter);

        //Setting RecyclerTouchListener
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(this, recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Toast.makeText(MainActivity.this, "onClick " + position, Toast.LENGTH_SHORT).show();  //placeholder
            }

            @Override
            public void onLongClick(View view, int position) {
                Toast.makeText(MainActivity.this, "onLongClick " + position, Toast.LENGTH_SHORT).show(); //placeholder
                //TODO open overlay fragment with options:
                //copy title & pass
                //copy title
                //copy pass
                //hide
            }
        }));

        if(mDatabase == null) {
            mDatabase = new PasswordDB(this);
        }

        //getEntries will remove textNoData from layout (GONE)
        textNoData.setText("Getting Root Permission...");

        if (savedInstanceState != null) {
            //if this fragment starts after a rotation or configuration change, load the existing movies from a parcelable
            mListWifi = savedInstanceState.getParcelableArrayList(STATE_WIFI_ENTRIES);
        } else {
            //if this fragment starts for the first time, load the list of movies from a database
            mListWifi = mDatabase.getAllWifiEntries(false);
            //if the database is empty, trigger an AsycnTask to download movie list from the web
            if (mListWifi.isEmpty()) {
                L.m("FragmentBoxOffice: executing task from fragment");
                new TaskLoadWifiEntries(this).execute();
            }
        }
    }
    //TODO Implement "Hidden" table.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;

        } else if (id == R.id.action_refresh_from_file) {
            dataFromFile();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        L.T(this, "Destroying Database");
        mDatabase.deleteAll(false);
        super.onDestroy();
    }


    public void dataFromFile() {

        new DataFetcher().execute("");

    }

    /***********************************************************************/
    //Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

    /***********************************************************************/
    public class DataFetcher extends AsyncTask<String, Void, Boolean> {

        public DataFetcher() {

        }


        @Override
        protected Boolean doInBackground(String... params) {
            if(!canRunRootCommands()) {
                Log.e("DataFetcher", "No Root Access");
                cancel(true);
            }



            boolean dirCreated = createDir();
            if (!dirCreated) {
                Log.e("DataFetcher", "Failed to create app directory");
                return false;
            }
            copyFile();

            return readFile();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            //getEntries(TABLE_PASSWORDS);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            textNoData.setText("No Root Access");
        }

        /**************
         * Helper Methods
         ********************/
        private boolean createDir() {
            Log.e("DataFetcher", "Creating Dir");
            File folder = new File(Environment.getExternalStorageDirectory() + "/WifiPasswords");
            boolean dirCreated = true;
            if (!folder.exists()) {
                dirCreated = folder.mkdir();
            }
            if (!dirCreated) {
                Log.e("DataFetcher", "Failed to create directory");
                return false;
            }

            return true;
        }

        private void copyFile() {
            if (!ExecuteAsRootBase.canRunRootCommands()) {
                return;
            }

            Log.e("DataFetcher", "Copying File");
            try {
                Process suProcess = Runtime.getRuntime().exec("su -c cp /data/misc/wifi/wpa_supplicant.conf /sdcard/WifiPasswords");
                suProcess.waitFor(); //wait for SU command to finish
            } catch (IOException | InterruptedException e) {
                Log.e("DataFetcher", "copyFile Error: " + e.getClass().getName() + " " + e);
                e.printStackTrace();
            }
        }

        private boolean readFile() {

            try {

                File directory = Environment.getExternalStorageDirectory();
                File file = new File(directory + "/WifiPasswords/wpa_supplicant.conf");

                if (!file.exists()) {
                    Log.e("DataFetcher", "readFile - File not found");
                    return false;
                }

                Log.e("DataFetcher", "Starting to read");

                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line = "";
                String title = "";
                String password = "";
                String check = "";

                while ((line = bufferedReader.readLine()) != null) {
                    if (line.equals("network={")) {

                        line = bufferedReader.readLine();
                        title = line.substring(7, line.length() - 1);

                        line = bufferedReader.readLine();

                        //Log.i("DataFetcher", title + " " + line.substring(6, line.length() - 1));
                        //Log.i("DataFetcher", title + " " + line.substring(1, 4));

                        if ((line.substring(1, 4)).equals("psk")) {
                            password = line.substring(6, line.length() - 1);
                        } else {
                            password = "no password";
                        }

                        Log.e("DataFetcher", title + " " + password);

                        addEntry(TABLE_PASSWORDS, title, password);
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        /***********************************************************************/
        //Root Check method
        //Credit: http://muzikant-android.blogspot.co.il/2011/02/how-to-get-root-access-and-execute.html

        /***********************************************************************/
        private boolean canRunRootCommands() {
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
                        Log.d("ROOT", "Can't get root access or denied by user");
                    } else if (true == currUid.contains("uid=0")) {
                        retval = true;
                        exitSu = true;
                        Log.d("ROOT", "Root access granted");
                    } else {
                        retval = false;
                        exitSu = true;
                        Log.d("ROOT", "Root access rejected: " + currUid);
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
                Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
            }

            return retval;
        }
    }

}
