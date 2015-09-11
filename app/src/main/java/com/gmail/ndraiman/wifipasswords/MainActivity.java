package com.gmail.ndraiman.wifipasswords;

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

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private WifiListAdapter listAdapter;
    private TextView textNoData;

    private boolean isDbExists = false;
    SQLiteDatabase passwordsDB = null;
    private final String DB_NAME = "WifiPasswords";
    private final String TABLE_PASSWORDS = "passwords";
    private final String TABLE_HIDDEN = "hidden"; //will hold hidden passwords?

    private ArrayList<WifiEntry> wifiData = new ArrayList<>();

    //TODO possible way to solve layout draw issue - check if app running for first time.
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

        //open or create tables.
        openOrCreateDatabase();

        //getEntries will remove textNoData from layout (GONE)
        textNoData.setText("Retrieving Data...");

        if (passwordsDB != null) {

            if (dbIsEmpty(passwordsDB, TABLE_PASSWORDS)) {
                //TODO leave commented until drawing issue is solved (StackOverflow)
                //dataFromFile(); //onPostExecute calls getEntries()

            } else {
                Log.e("onCreate", "DB isnt empty! - loading data from DB");
                getEntries(TABLE_PASSWORDS);
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
        Toast.makeText(this, "Destroying Database", Toast.LENGTH_SHORT).show();
        passwordsDB.execSQL("DROP TABLE " + TABLE_PASSWORDS); //TODO Debugging reading file method
        passwordsDB.close();
        super.onDestroy();
    }


    public boolean dbIsEmpty(SQLiteDatabase db, String table) {
        Cursor mCursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
            int count = mCursor.getInt(0);

            if (count > 0) {
                return false;
            }

            mCursor.close();
        }
        return true;
    }

    public void dataFromFile() {

        DataFetcher dataFetcher = new DataFetcher();
        dataFetcher.execute("");

    }

    /***********************************************************************/
    // SQLite Methods

    /***********************************************************************/
    public void openOrCreateDatabase() {
        try {
            passwordsDB = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
            if (isDbExists) {
                if (passwordsDB.isOpen()) {
                    Toast.makeText(this, "Database Opened", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            passwordsDB.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_PASSWORDS
                    + " (id integer primary key, name VARCHAR, pass VARCHAR);");


            //File database = getApplicationContext().getDatabasePath("WifiPasswords.db");
            if (passwordsDB.isOpen()) { //database.exists()
                Log.e("SQLite DB", "Database created successfully");
                isDbExists = true;
            } else {
                Log.e("SQLite DB", "Error creating Database");
                isDbExists = false;
            }

        } catch (Exception e) {
            Log.e("SQLite DB", "SQLite createDatabase() exception" + e);
            e.printStackTrace();
        }
    }

    public void addEntry(String table, String title, String password) {

        passwordsDB.execSQL("INSERT INTO " + table + " (name, pass) VALUES ('" +
                title + "', '" + password + "');");
    }

    public void getEntries(String table) {
        // A Cursor provides read and write access to database results
        Cursor cursor = passwordsDB.rawQuery("SELECT * FROM " + table, null);

        // Get the index for the column name provided
        int idColumn = cursor.getColumnIndex("id");
        int nameColumn = cursor.getColumnIndex("name");
        int passColumn = cursor.getColumnIndex("pass");

        cursor.moveToFirst();

        if (cursor != null && (cursor.getCount() > 0)) {
            textNoData.setVisibility(View.GONE);

            do {
                // Get the results and store them in a String
                String id = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn);
                String password = cursor.getString(passColumn);

                WifiEntry current = new WifiEntry(id, name, password);
                wifiData.add(current);

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());

            //notify RecyclerView Adapter
            listAdapter.setWifiList(wifiData);

            cursor.close();

        } else {
            Toast.makeText(this, "No Results to Show", Toast.LENGTH_SHORT).show();
            Log.e("getEntries", "Cursor is null");
        }

        Log.i("getEntries", "getEntries finished");
    }

    /***********************************************************************/
    //Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

    /***********************************************************************/
    public class DataFetcher extends AsyncTask<String, Void, Boolean> {

        public DataFetcher() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(!canRunRootCommands()) {
                Log.e("DataFetcher", "No Root Access");
                cancel(true);
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
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
            getEntries(TABLE_PASSWORDS);
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
