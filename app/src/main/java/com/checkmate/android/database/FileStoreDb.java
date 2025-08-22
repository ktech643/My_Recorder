package com.checkmate.android.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.checkmate.android.model.Media;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileStoreDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "filestore.db";
    private static final int DB_VER = 2;
    private static final String TABLE = "files";

    // Column names
    public static final String COL_ID = "_id";
    public static final String COL_FILENAME = "filename";
    public static final String COL_PATH = "path";
    public static final String COL_TIMESTAMP = "ts";
    public static final String COL_TYPE = "type";
    public static final String COL_ENCRYPTED = "enc";
    public static final String COL_DURATION = "duration";
    public static final String COL_RES_W = "res_w";
    public static final String COL_RES_H = "res_h";
    public static final String COL_FILE_SIZE = "file_size";

    public FileStoreDb(Context ctx) {
        super(ctx.getApplicationContext(), DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FILENAME + " TEXT NOT NULL, "
                + COL_PATH + " TEXT NOT NULL, "
                + COL_TIMESTAMP + " INTEGER NOT NULL, "
                + COL_TYPE + " TEXT NOT NULL, "
                + COL_ENCRYPTED + " INTEGER NOT NULL DEFAULT 0, "
                + COL_DURATION + " INTEGER, "
                + COL_RES_W + " INTEGER, "
                + COL_RES_H + " INTEGER, "
                + COL_FILE_SIZE + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    public long logFile(String filename, String path, long timestamp,
                        String type, boolean encrypted, long duration,
                        int resW, int resH, long fileSize) {
        ContentValues cv = new ContentValues();
        cv.put(COL_FILENAME, filename);
        cv.put(COL_PATH, path);
        cv.put(COL_TIMESTAMP, timestamp);
        cv.put(COL_TYPE, type);
        cv.put(COL_ENCRYPTED, encrypted ? 1 : 0);
        cv.put(COL_DURATION, duration);
        cv.put(COL_RES_W, resW);
        cv.put(COL_RES_H, resH);
        cv.put(COL_FILE_SIZE, fileSize);
        return getWritableDatabase().insert(TABLE, null, cv);
    }

    // FileStoreDb.java
    @SuppressLint("Range")
    public List<Media> getMediaForPath(String treeUri) {
        List<Media> mediaList = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection = COL_PATH + " LIKE ?";
        String[] selectionArgs = new String[]{treeUri + "%"};

        try (Cursor cursor = db.query(
                TABLE,
                null,
                selection,
                selectionArgs,
                null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Media media = new Media();
                    media.name = cursor.getString(cursor.getColumnIndex(COL_FILENAME));
                    media.contentUri = Uri.parse(cursor.getString(cursor.getColumnIndex(COL_PATH)));
                    media.date = new Date(cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));

                    // Handle media type
                    String type = cursor.getString(cursor.getColumnIndex(COL_TYPE));
                    media.type = "video".equals(type) ? Media.TYPE.VIDEO : Media.TYPE.PHOTO;

                    media.is_encrypted = cursor.getInt(cursor.getColumnIndex(COL_ENCRYPTED)) == 1;
                    media.duration = cursor.getLong(cursor.getColumnIndex(COL_DURATION));
                    media.resolutionWidth = cursor.getInt(cursor.getColumnIndex(COL_RES_W));
                    media.resolutionHeight = cursor.getInt(cursor.getColumnIndex(COL_RES_H));
                    media.fileSize = cursor.getLong(cursor.getColumnIndex(COL_FILE_SIZE));

                    mediaList.add(media);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("FileStoreDb", "Error querying media", e);
        }
        return mediaList;
    }


    public int deleteByPath(String path) {
        return getWritableDatabase()
                .delete(TABLE, COL_PATH + " = ?", new String[]{path});
    }
}