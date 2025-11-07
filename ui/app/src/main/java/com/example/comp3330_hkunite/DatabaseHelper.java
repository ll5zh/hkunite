package com.example.comp3330_hkunite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "hkunite.db";
    private static final int DB_VERSION = 1;
    private static DatabaseHelper instance;
    private final Context context;
    private static final String DB_PATH_SUFFIX = "/databases/";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
        copyDatabaseIfNeeded();
    }

    private void copyDatabaseIfNeeded() {
        String dbPath = context.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DB_NAME;
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            this.getReadableDatabase(); // creates empty db
            try {
                InputStream is = context.getAssets().open(DB_NAME);
                OutputStream os = new FileOutputStream(dbPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                os.flush();
                os.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No-op: using prebuilt database
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle schema upgrades if needed
    }

    public List<Event> getAllPublicEvents() {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM EVENT WHERE PUBLIC = 1", null);
        if (cursor.moveToFirst()) {
            do {
                int eid = cursor.getInt(cursor.getColumnIndexOrThrow("EID"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("TITLE"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPTION"));
                String image = cursor.getString(cursor.getColumnIndexOrThrow("IMAGE"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("DATE"));

                events.add(new Event(eid, title, description, image, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return events;
    }

    public Event getEvent(int eid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM EVENT WHERE EID = ?", new String[]{String.valueOf(eid)});

        if (cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow("TITLE"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("DESCRIPTION"));
            String image = cursor.getString(cursor.getColumnIndexOrThrow("IMAGE"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("DATE"));

            cursor.close();
            return new Event(eid, title, description, image, date);
        }

        cursor.close();
        return null;
    }

    public boolean joinEvent(int uid, int eid) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if already joined
        Cursor cursor = db.rawQuery("SELECT * FROM EVENT_PARTICIPANT WHERE UID = ? AND EID = ?", new String[]{String.valueOf(uid), String.valueOf(eid)});
        boolean alreadyJoined = cursor.moveToFirst();
        cursor.close();

        if (alreadyJoined) return false;

        ContentValues values = new ContentValues();
        values.put("UID", uid);
        values.put("EID", eid);
        db.insert("EVENT_PARTICIPANT", null, values);
        return true;
    }

    public boolean hasJoinedEvent(int uid, int eid) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM EVENT_PARTICIPANT WHERE UID = ? AND EID = ?", new String[]{String.valueOf(uid), String.valueOf(eid)});
        boolean joined = cursor.moveToFirst();
        cursor.close();
        return joined;
    }



}
