package com.checkmate.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.checkmate.android.AppPreference;
import com.checkmate.android.model.Camera;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private static DBManager instance;

    private DBOpenHelper mdbhelper;
    private final String SELECT_DB_STR = "select * from ";
    private final String DELETE_DB_STR = "delete from ";

    public DBManager(Context context) {
        this.mdbhelper = new DBOpenHelper(context);
        synchronized (DBOpenHelper.DB_LOCK) {
            SQLiteDatabase db = mdbhelper.getWritableDatabase();
            this.mdbhelper.onUpgrade(db, AppPreference.getInt(AppPreference.KEY.DB_VERSION, 4), 5);
        }
        instance = this;
    }

    public static DBManager getInstance() {
        return instance;
    }

    synchronized public void addCamera(Camera model) {

        synchronized (DBOpenHelper.DB_LOCK) {

            ContentValues values = new ContentValues();
            values.put(DBConstant.CAM_NAME, model.camera_name);
            values.put(DBConstant.CAM_WIFI_TYPE, model.camera_wifi_type);
            values.put(DBConstant.CAM_URL, model.url);
            values.put(DBConstant.CAM_USERNAME, model.username);
            values.put(DBConstant.CAM_PASSWORD, model.password);
            values.put(DBConstant.CAM_PORT, model.port);
            values.put(DBConstant.CAM_URI, model.uri);
            values.put(DBConstant.CAM_WIFI_SSID, model.wifi_ssid);
            values.put(DBConstant.CAM_RTSP_TYPE, model.rtsp_type);
            values.put(DBConstant.CAM_WIFI_IN, model.wifi_in);
            values.put(DBConstant.CAM_WIFI_OUT, model.wifi_out);
            values.put(DBConstant.CAM_FULL_ADDRESS, model.use_full_address ? 1 : 0);
            values.put(DBConstant.CAM_WIFI_PASSWORD, model.wifi_password);

            SQLiteDatabase mdb = mdbhelper.getWritableDatabase();
            if (mdb != null) {
                mdb.insert(DBConstant.TBL_CAMERAS, null, values);
                mdb.close();
            }
        }
    }

    synchronized public void updateCamera(Camera model) {

        synchronized (DBOpenHelper.DB_LOCK) {
            ContentValues values = new ContentValues();
            values.put(DBConstant.CAM_NAME, model.camera_name);
            values.put(DBConstant.CAM_WIFI_TYPE, model.camera_wifi_type);
            values.put(DBConstant.CAM_URL, model.url);
            values.put(DBConstant.CAM_USERNAME, model.username);
            values.put(DBConstant.CAM_PASSWORD, model.password);
            values.put(DBConstant.CAM_PORT, model.port);
            values.put(DBConstant.CAM_URI, model.uri);
            values.put(DBConstant.CAM_WIFI_SSID, model.wifi_ssid);
            values.put(DBConstant.CAM_RTSP_TYPE, model.rtsp_type);
            values.put(DBConstant.CAM_WIFI_IN, model.wifi_in);
            values.put(DBConstant.CAM_WIFI_OUT, model.wifi_out);
            values.put(DBConstant.CAM_FULL_ADDRESS, model.use_full_address ? 1 : 0);
            values.put(DBConstant.CAM_WIFI_PASSWORD, model.wifi_password);

            SQLiteDatabase mdb = mdbhelper.getWritableDatabase();
            if (mdb != null) {
                mdb.update(DBConstant.TBL_CAMERAS, values, "id=" + model.id, null);
                mdb.close();
            }
        }
    }

    synchronized public void deleteCamera(int id) {

        synchronized (DBOpenHelper.DB_LOCK) {
            SQLiteDatabase mdb = mdbhelper.getWritableDatabase();
            if (mdb != null) {
                String sql_str = DELETE_DB_STR + DBConstant.TBL_CAMERAS + " where "
                        + DBConstant.ID + " = " + id + "";
                mdb.execSQL(sql_str);
                mdb.close();
            }
        }
    }

    synchronized public boolean isExistCamera(Camera camera) {
        List<Camera> data = getCameras();
        for (Camera item : data) {
            if (TextUtils.equals(item.url, camera.url)) {
                return true;
            }
        }
        return false;
    }


    synchronized public List<Camera> getCameras() {
        synchronized (DBOpenHelper.DB_LOCK) {
            SQLiteDatabase mdb = mdbhelper.getReadableDatabase();
            ArrayList<Camera> modelList = new ArrayList<>();

            if (mdb != null) {
                String sql_str = SELECT_DB_STR + DBConstant.TBL_CAMERAS;

                Cursor cursor = mdb.rawQuery(sql_str, null);
                if (cursor.getCount() > 0) {
                    if (cursor.moveToFirst()) {
                        while (!cursor.isAfterLast()) {
                            Camera model = new Camera();

                            int columIndex = cursor.getColumnIndex(DBConstant.ID);
                            model.id = cursor.getInt(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_NAME);
                            model.camera_name = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_TYPE);
                            model.camera_wifi_type = cursor.getInt(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_URL);
                            model.url = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_USERNAME);
                            model.username = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_PASSWORD);
                            model.password = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_PORT);
                            model.port = cursor.getInt(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_URI);
                            model.uri = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_SSID);
                            model.wifi_ssid = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_RTSP_TYPE);
                            model.rtsp_type = cursor.getInt(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_IN);
                            model.wifi_in = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_OUT);
                            model.wifi_out = cursor.getString(columIndex);

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_FULL_ADDRESS);
                            model.use_full_address = cursor.getInt(columIndex) == 1;

                            columIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_PASSWORD);
                            model.wifi_password = cursor.getString(columIndex);

                            modelList.add(model);
                            cursor.moveToNext();
                        }
                    }
                }
            }
            return modelList;
        }
    }
}
