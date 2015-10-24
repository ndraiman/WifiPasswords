package com.gmail.ndrdevelop.wifipasswords.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.gmail.ndrdevelop.wifipasswords.R;
import com.gmail.ndrdevelop.wifipasswords.extras.MyApplication;
import com.gmail.ndrdevelop.wifipasswords.pojo.WifiEntry;

import java.util.ArrayList;


public class PasswordDB {

    PasswordHelper mHelper;
    SQLiteDatabase mDatabase;

    public static int mNewEntriesOnLastInsert = 0;


    public PasswordDB(Context context) {
        mHelper = new PasswordHelper(context);
        mDatabase = mHelper.getWritableDatabase();

    }

    /******************************************************/
    /****************** Update Methods ********************/
    /******************************************************/

    public void updateWifiTags(ArrayList<WifiEntry> listWifi, String tag) {

        ContentValues values = new ContentValues();

        values.put(PasswordHelper.COLUMN_TAG, tag);

        String[] columns = new String[]{PasswordHelper.COLUMN_UID, PasswordHelper.COLUMN_TITLE};
        String selection = PasswordHelper.COLUMN_TITLE + " = ?";
        String[] selectionArgs = new String[listWifi.size()];

        for (int i = 0; i < selectionArgs.length; i++) {

            if (i > 0) {
                selection += " OR " + PasswordHelper.COLUMN_TITLE + " = ?";
            }
            selectionArgs[i] = listWifi.get(i).getTitle();
        }


        Cursor cursor = mDatabase.query(PasswordHelper.TABLE_MAIN, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            do {

                int id = cursor.getInt(cursor.getColumnIndex(PasswordHelper.COLUMN_UID));
                mDatabase.update(PasswordHelper.TABLE_MAIN, values, PasswordHelper.COLUMN_UID + " = ?", new String[]{id + ""});

            } while (cursor.moveToNext());

            cursor.close();
        }


    }


    /******************************************************/
    /****************** Insert Methods ********************/
    /******************************************************/

    public void insertWifiEntries(ArrayList<WifiEntry> listWifi, boolean updateTags, boolean archive) {
        mNewEntriesOnLastInsert = 0;

        String table = archive ? PasswordHelper.TABLE_ARCHIVE : PasswordHelper.TABLE_MAIN;

        ContentValues values = new ContentValues();

        String[] columns = new String[]{PasswordHelper.COLUMN_UID, PasswordHelper.COLUMN_TITLE};
        String selection = PasswordHelper.COLUMN_TITLE + " = ?";


        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);

            values.clear();
            values.put(PasswordHelper.COLUMN_TITLE, current.getTitle());
            values.put(PasswordHelper.COLUMN_PASSWORD, current.getPassword());

            if (updateTags) {
                values.put(PasswordHelper.COLUMN_TAG, current.getTag());

            }

            if (!archive) {
                //Main Table - Check for duplicates
                String[] selectionArgs = new String[]{current.getTitle()};
                Cursor cursor = mDatabase.query(table, columns, selection, selectionArgs, null, null, null);

                if (cursor.moveToFirst()) {

                    int id = cursor.getInt(cursor.getColumnIndex(PasswordHelper.COLUMN_UID));
                    mDatabase.update(table, values, PasswordHelper.COLUMN_UID + " = ?", new String[]{id + ""});

                } else {

                    mDatabase.insert(table, null, values);
                    mNewEntriesOnLastInsert++;
                }

                cursor.close();

            } else {
                mDatabase.insert(table, null, values);
            }
        }

    }


    //Holds Deleted Wifi Entries
    public void insertDeleted(ArrayList<WifiEntry> listWifi) {

        ContentValues values = new ContentValues();

        for (int i = 0; i < listWifi.size(); i++) {
            WifiEntry current = listWifi.get(i);

            values.clear();
            values.put(PasswordHelper.COLUMN_TITLE, current.getTitle());
            values.put(PasswordHelper.COLUMN_PASSWORD, current.getPassword());
            values.put(PasswordHelper.COLUMN_TAG, current.getTag());

            mDatabase.insert(PasswordHelper.TABLE_DELETED, null, values);
        }
    }

    /***************************************************/
    /****************** Get Methods ********************/
    /***************************************************/

    public ArrayList<WifiEntry> getAllWifiEntries(boolean archive) {

        String table = archive ? PasswordHelper.TABLE_ARCHIVE : PasswordHelper.TABLE_MAIN;

        String selection;

        if (!archive) {
            selection = PasswordHelper.COLUMN_TITLE + " NOT IN (SELECT "
                    + PasswordHelper.COLUMN_TITLE
                    + " FROM " + PasswordHelper.TABLE_ARCHIVE + ")";

        } else {
            selection = PasswordHelper.COLUMN_TITLE + " NOT IN (SELECT "
                    + PasswordHelper.COLUMN_TITLE
                    + " FROM " + PasswordHelper.TABLE_DELETED + ")";
        }

        ArrayList<WifiEntry> listWifi = new ArrayList<>();


        Cursor cursor = mDatabase.query(table, null, selection, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            do {

                WifiEntry wifiEntry = new WifiEntry();

                wifiEntry.setTitle(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TITLE)));
                wifiEntry.setPassword(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_PASSWORD)));

                String tag = cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TAG));
                wifiEntry.setTag(tag == null ? "" : tag);

                boolean showNoPassword = PreferenceManager.getDefaultSharedPreferences(mHelper.mContext)
                        .getBoolean(mHelper.mContext.getString(R.string.pref_show_no_password_key), true);

                if (!wifiEntry.getPassword().equals(MyApplication.NO_PASSWORD_TEXT) || showNoPassword) {
                    listWifi.add(wifiEntry);
                }

            } while (cursor.moveToNext());

            cursor.close();
        }
        return listWifi;
    }

    public ArrayList<WifiEntry> getWifiEntries(String whereClause, String[] whereArgs, boolean archive) {

        String table = archive ? PasswordHelper.TABLE_ARCHIVE : PasswordHelper.TABLE_MAIN;

        ArrayList<WifiEntry> listWifi = new ArrayList<>();

        Cursor cursor = mDatabase.query(table, null, whereClause, whereArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {

            do {
                WifiEntry entry = new WifiEntry();

                entry.setTitle(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TITLE)));
                entry.setPassword(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_PASSWORD)));
                entry.setTag(cursor.getString(cursor.getColumnIndex(PasswordHelper.COLUMN_TAG)));

                listWifi.add(entry);

            } while (cursor.moveToNext());

            cursor.close();
        }

        return listWifi;
    }

    /******************************************************/
    /****************** Delete Methods ********************/
    /******************************************************/

    public void purgeDatabase() {

        deleteAll(false);
        deleteAll(true);
        mDatabase.delete(PasswordHelper.TABLE_DELETED, null, null);
    }

    public void deleteAll(boolean archive) {

        String table = archive ? PasswordHelper.TABLE_ARCHIVE : PasswordHelper.TABLE_MAIN;

        mDatabase.delete(table, null, null);
    }


    public void deleteWifiEntries(ArrayList<WifiEntry> listWifi, boolean archive) {

        String table = archive ? PasswordHelper.TABLE_ARCHIVE : PasswordHelper.TABLE_MAIN;

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
        public static final String TABLE_ARCHIVE = "passwords_archive";
        public static final String TABLE_DELETED = "passwords_deleted";

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

        public static final String CREATE_TABLE_ARCHIVE = "CREATE TABLE " + TABLE_ARCHIVE
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_TAG + " TEXT"
                + ");";

        public static final String CREATE_TABLE_DELETED = "CREATE TABLE " + TABLE_DELETED
                + " ("
                + COLUMN_UID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT UNIQUE,"
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
                db.execSQL(CREATE_TABLE_ARCHIVE);
                db.execSQL(CREATE_TABLE_DELETED);

            } catch (SQLiteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            try {
                db.execSQL("DROP TABLE " + TABLE_MAIN + " IF EXISTS;");
                db.execSQL("DROP TABLE " + TABLE_ARCHIVE + " IF EXISTS;");
                db.execSQL("DROP TABLE " + TABLE_DELETED + " IF EXISTS;");
                onCreate(db);

            } catch (SQLiteException e) {
                e.printStackTrace();
            }

        }
    }


}
