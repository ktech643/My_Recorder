package com.checkmate.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.ThreadSafeAppPreference;
import com.checkmate.android.model.Camera;
import com.checkmate.android.util.ANRHandler;
import com.checkmate.android.util.CrashLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe database manager that prevents ANR by executing all database operations
 * on a background thread with timeout protection.
 */
public class ThreadSafeDBManager {
    private static final String TAG = "ThreadSafeDBManager";
    private static final long DB_OPERATION_TIMEOUT = 3000; // 3 seconds timeout
    
    private static volatile ThreadSafeDBManager instance;
    private final DBOpenHelper dbHelper;
    private final ExecutorService dbExecutor;
    private final ANRHandler anrHandler;
    private final CrashLogger crashLogger;
    
    private final String SELECT_DB_STR = "select * from ";
    private final String DELETE_DB_STR = "delete from ";
    
    /**
     * Private constructor
     */
    private ThreadSafeDBManager(Context context) {
        this.dbHelper = new DBOpenHelper(context);
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "DatabaseThread");
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
        this.anrHandler = ANRHandler.getInstance();
        this.crashLogger = CrashLogger.getInstance();
        
        // Initialize database on background thread
        initializeDatabase();
    }
    
    /**
     * Get singleton instance
     */
    public static ThreadSafeDBManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ThreadSafeDBManager.class) {
                if (instance == null) {
                    instance = new ThreadSafeDBManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize database on background thread
     */
    private void initializeDatabase() {
        dbExecutor.execute(() -> {
            try {
                synchronized (DBOpenHelper.DB_LOCK) {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int currentVersion = ThreadSafeAppPreference.getInstance()
                            .getInt(ThreadSafeAppPreference.KEY.DB_VERSION, 4);
                    dbHelper.onUpgrade(db, currentVersion, 5);
                    db.close();
                }
            } catch (Exception e) {
                crashLogger.e(TAG, "Failed to initialize database", e);
            }
        });
    }
    
    /**
     * Add camera with thread safety
     */
    public void addCamera(@NonNull Camera model, @Nullable DatabaseCallback<Boolean> callback) {
        executeDbOperation("addCamera", () -> {
            synchronized (DBOpenHelper.DB_LOCK) {
                ContentValues values = createCameraContentValues(model);
                
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    try {
                        long rowId = db.insert(DBConstant.TBL_CAMERAS, null, values);
                        return rowId != -1;
                    } finally {
                        db.close();
                    }
                }
                return false;
            }
        }, callback);
    }
    
    /**
     * Update camera with thread safety
     */
    public void updateCamera(@NonNull Camera model, @Nullable DatabaseCallback<Boolean> callback) {
        executeDbOperation("updateCamera", () -> {
            synchronized (DBOpenHelper.DB_LOCK) {
                ContentValues values = createCameraContentValues(model);
                
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    try {
                        int rowsAffected = db.update(DBConstant.TBL_CAMERAS, values, 
                                "id=" + model.id, null);
                        return rowsAffected > 0;
                    } finally {
                        db.close();
                    }
                }
                return false;
            }
        }, callback);
    }
    
    /**
     * Delete camera with thread safety
     */
    public void deleteCamera(int id, @Nullable DatabaseCallback<Boolean> callback) {
        executeDbOperation("deleteCamera", () -> {
            synchronized (DBOpenHelper.DB_LOCK) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                if (db != null) {
                    try {
                        String sql = DELETE_DB_STR + DBConstant.TBL_CAMERAS + 
                                " where " + DBConstant.ID + " = " + id;
                        db.execSQL(sql);
                        return true;
                    } finally {
                        db.close();
                    }
                }
                return false;
            }
        }, callback);
    }
    
    /**
     * Check if camera exists with thread safety
     */
    public void isExistCamera(@NonNull Camera camera, @NonNull DatabaseCallback<Boolean> callback) {
        getCameras(cameras -> {
            if (cameras != null) {
                for (Camera item : cameras) {
                    if (TextUtils.equals(item.url, camera.url)) {
                        callback.onSuccess(true);
                        return;
                    }
                }
            }
            callback.onSuccess(false);
        });
    }
    
    /**
     * Get all cameras with thread safety
     */
    public void getCameras(@NonNull DatabaseCallback<List<Camera>> callback) {
        executeDbOperation("getCameras", () -> {
            synchronized (DBOpenHelper.DB_LOCK) {
                ArrayList<Camera> modelList = new ArrayList<>();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                
                if (db != null) {
                    Cursor cursor = null;
                    try {
                        String sql = SELECT_DB_STR + DBConstant.TBL_CAMERAS;
                        cursor = db.rawQuery(sql, null);
                        
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                Camera model = parseCameraFromCursor(cursor);
                                modelList.add(model);
                            } while (cursor.moveToNext());
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        db.close();
                    }
                }
                return modelList;
            }
        }, callback);
    }
    
    /**
     * Get cameras synchronously with timeout protection (use sparingly)
     */
    @Nullable
    public List<Camera> getCamerasSync() {
        try {
            Future<List<Camera>> future = dbExecutor.submit(() -> {
                synchronized (DBOpenHelper.DB_LOCK) {
                    ArrayList<Camera> modelList = new ArrayList<>();
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    
                    if (db != null) {
                        Cursor cursor = null;
                        try {
                            String sql = SELECT_DB_STR + DBConstant.TBL_CAMERAS;
                            cursor = db.rawQuery(sql, null);
                            
                            if (cursor != null && cursor.moveToFirst()) {
                                do {
                                    Camera model = parseCameraFromCursor(cursor);
                                    modelList.add(model);
                                } while (cursor.moveToNext());
                            }
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                            }
                            db.close();
                        }
                    }
                    return modelList;
                }
            });
            
            return future.get(DB_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            crashLogger.e(TAG, "Failed to get cameras synchronously", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Execute database operation with timeout protection
     */
    private <T> void executeDbOperation(@NonNull String operationName,
                                       @NonNull Callable<T> operation,
                                       @Nullable DatabaseCallback<T> callback) {
        dbExecutor.execute(() -> {
            try {
                T result = operation.call();
                if (callback != null) {
                    anrHandler.executeOnMainThreadSafe(
                        "dbCallback_" + operationName,
                        () -> result,
                        callback::onSuccess,
                        callback::onError,
                        1000 // 1 second timeout for callbacks
                    );
                }
            } catch (Exception e) {
                crashLogger.e(TAG, "Database operation failed: " + operationName, e);
                if (callback != null) {
                    anrHandler.executeOnMainThreadSafe(
                        "dbErrorCallback_" + operationName,
                        () -> e,
                        callback::onError,
                        error -> {},
                        1000
                    );
                }
            }
        });
    }
    
    /**
     * Create ContentValues for camera
     */
    private ContentValues createCameraContentValues(Camera model) {
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
        return values;
    }
    
    /**
     * Parse camera from cursor
     */
    private Camera parseCameraFromCursor(Cursor cursor) {
        Camera model = new Camera();
        
        int columnIndex = cursor.getColumnIndex(DBConstant.ID);
        if (columnIndex != -1) model.id = cursor.getInt(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_NAME);
        if (columnIndex != -1) model.camera_name = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_TYPE);
        if (columnIndex != -1) model.camera_wifi_type = cursor.getInt(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_URL);
        if (columnIndex != -1) model.url = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_USERNAME);
        if (columnIndex != -1) model.username = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_PASSWORD);
        if (columnIndex != -1) model.password = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_PORT);
        if (columnIndex != -1) model.port = cursor.getInt(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_URI);
        if (columnIndex != -1) model.uri = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_SSID);
        if (columnIndex != -1) model.wifi_ssid = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_RTSP_TYPE);
        if (columnIndex != -1) model.rtsp_type = cursor.getInt(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_IN);
        if (columnIndex != -1) model.wifi_in = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_OUT);
        if (columnIndex != -1) model.wifi_out = cursor.getString(columnIndex);
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_FULL_ADDRESS);
        if (columnIndex != -1) model.use_full_address = cursor.getInt(columnIndex) == 1;
        
        columnIndex = cursor.getColumnIndex(DBConstant.CAM_WIFI_PASSWORD);
        if (columnIndex != -1) model.wifi_password = cursor.getString(columnIndex);
        
        return model;
    }
    
    /**
     * Shutdown database manager
     */
    public void shutdown() {
        dbExecutor.shutdown();
        try {
            if (!dbExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dbExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dbExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Database callback interface
     */
    public interface DatabaseCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}