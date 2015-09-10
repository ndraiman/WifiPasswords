package com.gmail.ndraiman.wifipasswords;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView listWifi;
    private WifiListAdapter listAdapter;

    private boolean isDbExists = false;
    SQLiteDatabase passwordsDB = null;
    private final String DB_NAME = "WifiPasswords";
    private final String TABLE_NAME = "passwords";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);

        listWifi = (RecyclerView) findViewById(R.id.wifiList);
        listWifi.setLayoutManager(new LinearLayoutManager(this));
        listAdapter = new WifiListAdapter(this);
        listWifi.setAdapter(listAdapter);

        //TODO create SQLite database to hold Wifi Passwords
        //TODO Check if database is empty:
        //TODO  if yes, load from wpa_supplicant.conf
        //TODO  if no, load from database

        //TODO Refresh database from wpa_supplicant.conf

        openOrCreateDatabase();

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
        passwordsDB.close();
        super.onDestroy();
    }

    /******************
     * SQLite Methods
     ******************/
    public void openOrCreateDatabase() {
        try {
            passwordsDB = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
            if(isDbExists) {
                if(passwordsDB.isOpen()) {
                    Toast.makeText(this, "Database Opened", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            passwordsDB.execSQL("CREATE TABLE IF NOT EXISTS "+ TABLE_NAME
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

    public void addEntry(String title, String password) {

        passwordsDB.execSQL("INSERT INTO " + TABLE_NAME + " (name, pass) VALUES ('" +
                title + "', '" + password + "');");
    }

    public void getEntries() {
        // A Cursor provides read and write access to database results
        Cursor cursor = passwordsDB.rawQuery("SELECT * FROM " + TABLE_NAME, null);

        // Get the index for the column name provided
        int idColumn = cursor.getColumnIndex("id");
        int nameColumn = cursor.getColumnIndex("name");
        int passColumn = cursor.getColumnIndex("pass");

        cursor.moveToFirst();

        //TODO Add each entry to a RecyclerView row
    }


}
