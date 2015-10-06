package com.gmail.ndraiman.wifipasswords.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.gmail.ndraiman.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;
import java.util.Date;


public class PasswordDB {

    private PasswordHelper mHelper;
    private SQLiteDatabase mDatabase;
    private static final String TAG = "PasswordDB";


    public PasswordDB(Context context) {
        mHelper = new PasswordHelper(context);
        mDatabase = mHelper.getWritableDatabase();

    }

    /******************************************************/
    /****************** Update Methods ********************/
    /******************************************************/

    public void insertWifiTags(ArrayList<WifiEntry> listWifi, String tag) {

        ContentValues values = new ContentValues();

        values.put(PasswordHelper.COLUMN_TAG, tag);

        String[] columns = new String[]{PasswordHelper.COLUMN_UID, PasswordHelper.COLUMN_TITLE};
        String selection = PasswordHelper.COLUMN_TITLE + " = ?";
        String[] selectionArgs = new String[listWifi.size()];

        for (int i = 0; i < listWifi.size(); i++) {

            selectionArgs[i] = listWifi.get(i).getTitle();
        }

        Cursor cursor = mDatabase.query(PasswordHelper.TABLE_MAIN, columns, selection, selectionArgs, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {

            do {

                int id = cursor.getInt(cursor.getColumnIndex(PasswordHelper.COLUMN_UID));
                mDatabase.update(PasswordHelper.TABLE_MAIN, values, PasswordHelper.COLUMN_UID + " = ?", new String[]{id + ""});

            } while(cursor.moveToNext());

            cursor.close();
        }


    }


    /******************************************************/
    /****************** Insert Methods ********************/
    /******************************************************/

    public void insertWifiEntries(ArrayList<WifiEntry> listWifi, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_HIDDEN : PasswordHelper.TABLE_MAIN;
        Log.d(TAG, "insertWifiEntries - isHidden=" + isHidden + " table=" + table);

        ContentValues values = new ContentValues();

        String[] columns = new String[]{PasswordHelper.COLUMN_UID, PasswordHelper.COLUMN_TITLE};
        String selection = PasswordHelper.COLUMN_TITLE + " = ?";

        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);

            values.clear();
            values.put(PasswordHelper.COLUMN_TITLE, current.getTitle());
            values.put(PasswordHelper.COLUMN_PASSWORD, current.getPassword());
            values.put(PasswordHelper.COLUMN_TAG, current.getTag());

            if(!isHidden) {
                //Main Table - Check for duplicates
                String[] selectionArgs = new String[]{current.getTitle()};
                Cursor cursor = mDatabase.query(table, columns, selection, selectionArgs, null, null, null);

                if (cursor.moveToFirst()) {
                Log.e(TAG, "Updating Entry - " + current.getTitle());

                    int id = cursor.getInt(cursor.getColumnIndex(PasswordHelper.COLUMN_UID));
                    mDatabase.update(table, values, PasswordHelper.COLUMN_UID + " = ?", new String[]{id + ""});

                } else {
                Log.e(TAG, "Inserting Entry - " + current.getTitle());
                    mDatabase.insert(table, null, values);

                }

                cursor.close();

            } else {
                mDatabase.insert(table, null, values);
            }
        }

        Log.d(TAG, "inserting entries " + listWifi.size() + new Date(System.currentTimeMillis()));
    }

    /***************************************************/
    /****************** Get Methods ********************/
    /***************************************************/

    public ArrayList<WifiEntry> getAllWifiEntries(boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_HIDDEN : PasswordHelper.TABLE_MAIN;
        Log.d(TAG, "getAllWifiEntries - isHidden=" + isHidden + " table=" + table);

        String selection = null;

        if (!isHidden) {
            selection = PasswordHelper.COLUMN_TITLE + " NOT IN (SELECT "
                    + PasswordHelper.COLUMN_TITLE
                    + " FROM " + PasswordHelper.TABLE_HIDDEN + ")";
        }

        ArrayList<WifiEntry> listWifi = new ArrayList<>();


        Cursor cursor = mDatabase.query(table, null, selection, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "loading entries " + cursor.getCount() + new Date(System.currentTimeMillis()));

            do {

                WifiEntry wifiEntry = new WifiEntry();

                wifiEntry.setTitle(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TITLE)));
                wifiEntry.setPassword(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_PASSWORD)));
                wifiEntry.setTag(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TAG)));

                listWifi.add(wifiEntry);

            } while (cursor.moveToNext());

            cursor.close();
        }
        return listWifi;
    }

    public ArrayList<WifiEntry> getWifiEntries(String whereClause, String[] whereArgs, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_HIDDEN : PasswordHelper.TABLE_MAIN;
        Log.d(TAG, "getWifiEntries - isHidden=" + isHidden + " table=" + table);

        ArrayList<WifiEntry> listWifi = new ArrayList<>();

        Cursor cursor = mDatabase.query(table, null, whereClause, whereArgs, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "loading entries " + cursor.getCount() + new Date(System.currentTimeMillis()));

            do {
                WifiEntry entry = new WifiEntry();

                entry.setTitle(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TITLE)));
                entry.setPassword(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_PASSWORD)));
                entry.setTag(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TAG)));

                listWifi.add(entry);

            }while(cursor.moveToNext());

            cursor.close();
        }

        return listWifi;
    }

    /******************************************************/
    /****************** Delete Methods ********************/
    /******************************************************/

    public void deleteAll(boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_HIDDEN : PasswordHelper.TABLE_MAIN;
        Log.d(TAG, "deleteAll - isHidden=" + isHidden + " table=" + table);

        mDatabase.delete(table, null, null);
    }


    public void deleteWifiEntries(ArrayList<WifiEntry> listWifi, boolean isHidden) {

        String table = isHidden ? PasswordHelper.TABLE_HIDDEN : PasswordHelper.TABLE_MAIN;
        Log.d(TAG, "deleteWifiEntries - isHidden=" + isHidden + " table=" + table);

        String whereClause = PasswordHelper.COLUMN_TITLE + " = ?";
        String[] whereArgs;

        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);
            whereArgs = new String[]{current.getTitle()};
            mDatabase.delete(table, whereClause, whereArgs);
        }

    }

    public void close() {
        mDatabase.close();
    }


    /*************************************************************/
    /****************** Database Helper Class ********************/
    /*************************************************************/
    public static class PasswordHelper extends SQLiteOpenHelper {

        public static final String DB_NAME = "passwords_db";
        public static final int DB_VERSION = 1;

        public static final String TABLE_MAIN = "passwords_main";
        public static final String TABLE_HIDDEN = "passwords_hidden";

        public static final String COLUMN_UID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_TAG = "tag";

        public static final String CREATE_TABLE_MAIN = "CREATE TABLE " + TABLE_MAIN
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_TAG + " TEXT"
                + ");";

        public static final String CREATE_TABLE_HIDDEN = "CREATE TABLE " + TABLE_HIDDEN
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_TAG + " TEXT"
                + ");";

        private Context mContext;


        public PasswordHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            mContext = context;

        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            try {
                db.execSQL(CREATE_TABLE_MAIN);
                Log.d(TAG, "create table main executed");
                db.execSQL(CREATE_TABLE_HIDDEN);
                Log.d(TAG, "create table hidden executed");

            } catch (SQLiteException e) {
                Log.e(TAG, "ERROR: Helper onCreate - " + e);
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            try {
                Log.e(TAG, "upgrade table box office executed");
                db.execSQL("DROP TABLE " + TABLE_MAIN + " IF EXISTS;");
                db.execSQL("DROP TABLE " + TABLE_HIDDEN + " IF EXISTS;");
                onCreate(db);

            } catch (SQLiteException e) {
                Log.e(TAG, "ERROR: Helper onUpgrade - " + e);
            }

        }
    }


}
