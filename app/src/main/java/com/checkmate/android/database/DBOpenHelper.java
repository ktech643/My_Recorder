package com.checkmate.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.checkmate.android.AppPreference;

public class DBOpenHelper extends SQLiteOpenHelper {
    public static Object DB_LOCK = new Object();
    private static DBOpenHelper mdbopenhelper = null;
    private static SQLiteDatabase mwritabledb = null;
    private static SQLiteDatabase mreadabledb = null;

    public DBOpenHelper(Context context) {
        super(context, DBConstant.DATABASE_NAME, null,
                DBConstant.DATABASE_VERSION);
    }

    public static SQLiteDatabase writeDB() {
        if ((mwritabledb == null) && (mdbopenhelper != null))
            mwritabledb = mdbopenhelper.getWritableDatabase();
        return mwritabledb;
    }

    public static DBOpenHelper createDB(Context context) {
        if (mdbopenhelper == null)
            mdbopenhelper = new DBOpenHelper(context);
        return mdbopenhelper;
    }

    public static SQLiteDatabase readDB() {
        if ((mreadabledb == null) && (mdbopenhelper != null))
            mreadabledb = mdbopenhelper.getReadableDatabase();
        return mreadabledb;
    }

    @Override
    public final void onCreate(SQLiteDatabase db) {
        // servers table
        db.execSQL(DBConstant.CREATE_DB_SQL_PREFIX + DBConstant.TBL_CAMERAS
                + " (id integer primary key autoincrement, "
                + DBConstant.CAM_NAME + " text, "
                + DBConstant.CAM_WIFI_TYPE + " integer, "
                + DBConstant.CAM_URL + " text, "
                + DBConstant.CAM_USERNAME + " text, "
                + DBConstant.CAM_PORT + " integer, "
                + DBConstant.CAM_URI + " text, "
                + DBConstant.CAM_WIFI_SSID + " text, "
                + DBConstant.CAM_WIFI_IN + " text, "
                + DBConstant.CAM_WIFI_OUT + " text, "
                + DBConstant.CAM_WIFI_PASSWORD + " text, "
                + DBConstant.CAM_FULL_ADDRESS + " integer, "
                + DBConstant.CAM_RTSP_TYPE + " integer, "
                + DBConstant.CAM_PASSWORD + " text)");
    }

    public final void onUpgrade(SQLiteDatabase db, int oldVersion,
                                int newVersion) {
        Log.w("---[DEBUG]----", "Upgrading database from version " + oldVersion
                + " to " + newVersion + ", which will destroy all old data");

        if (newVersion > oldVersion) {
            db.execSQL(DBConstant.DELETE_DB_SQL_PREFIX + DBConstant.TBL_CAMERAS);
            AppPreference.setInt(AppPreference.KEY.DB_VERSION, newVersion);
        }
        onCreate(db);
    }

    public synchronized int deleteAllData(String tablename) {
        SQLiteDatabase db = getWritableDatabase();
        int answer = db.delete(tablename, null, null);
        db.close();
        return answer;
    }
}