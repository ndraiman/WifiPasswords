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
import android.widget.Toast;

import java.io.BufferedReader;
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

    private boolean isDbExists = false;
    SQLiteDatabase passwordsDB = null;
    private final String DB_NAME = "WifiPasswords";
    private final String TABLE_PASSWORDS = "passwords";
    private final String TABLE_HIDDEN = "hidden"; //will hold hidden passwords?

    private ArrayList<WifiEntry> wifiData = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

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

        //TODO create SQLite database to hold Wifi Passwords
        //TODO Check if database is empty:
        //TODO  if yes, load from wpa_supplicant.conf
        //TODO  if no, load from database

        //TODO Refresh database from wpa_supplicant.conf

        openOrCreateDatabase();
        //TODO !!!!! Find out why RecyclerView wont update view after getEntries calls setWifiList which calls notify
        dataFromFile();
        getEntries(TABLE_PASSWORDS);

        //TODO Handle the following commented lines after fixing recyclerview updating new data
//        if (passwordsDB != null) {
//
//            if (dbIsEmpty(passwordsDB, TABLE_PASSWORDS)) {
//                dataFromFile();
//
//            } else { //db isnt empty
//                Toast.makeText(this, "onCreate() - DB isnt empty! - loading data from DB", Toast.LENGTH_SHORT).show(); //placeholder
//                getEntries(TABLE_PASSWORDS);
//            }
//        }
    }


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
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
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
                Toast.makeText(this, "Database created successfully", Toast.LENGTH_SHORT).show();
                isDbExists = true;
            } else {
                Toast.makeText(this, "Error creating Database", Toast.LENGTH_SHORT).show();
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

        //TODO Add each entry to a RecyclerView row
        if (cursor != null && (cursor.getCount() > 0)) {

            do {
                // Get the results and store them in a String
                String id = cursor.getString(idColumn);
                String name = cursor.getString(nameColumn);
                String password = cursor.getString(passColumn);

                WifiEntry current = new WifiEntry(id, name, password);
                wifiData.add(current);

                // Keep getting results as long as they exist
            } while (cursor.moveToNext());

            //notify our RecyclerView Adapter
            listAdapter.setWifiList(wifiData); //TODO check if its better to call setWifiList after we add all the entries

            cursor.close();

        } else {
            Toast.makeText(this, "No Results to Show", Toast.LENGTH_SHORT).show();
            Log.e("getEntries", "Cursor is null");
        }


    }

    /***********************************************************************/
    //Copy wpa_supplicant.conf from /data/misc/wifi to sdcard/WifiPasswords

    /***********************************************************************/
    public class DataFetcher extends AsyncTask<String, Void, Boolean> {

        public DataFetcher() {

        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean dirCreated = createDir();
            if (!dirCreated) {
                Toast.makeText(getParent(), "Failed to create app directory", Toast.LENGTH_LONG).show();
                return false;
            }
            copyFile();
            readFile();

            return true;
        }


        private boolean createDir() {

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
                Runtime.getRuntime().exec("su -c cp /data/misc/wifi/wpa_supplicant.conf /sdcard/WifiPasswords");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void readFile() {

            try {

                File directory = Environment.getExternalStorageDirectory();
                File file = new File(directory + "/WifiPasswords/wpa_supplicant.conf");
                if (!file.exists()) {
                    Log.e("DataFetcher", "readFile - File not found");
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
            }
        }
    }

}
