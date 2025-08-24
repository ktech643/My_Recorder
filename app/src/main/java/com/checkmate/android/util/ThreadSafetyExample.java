package com.checkmate.android.util;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.checkmate.android.AppPreference;
import com.checkmate.android.ThreadSafeAppPreference;
import com.checkmate.android.database.ThreadSafeDBManager;
import com.checkmate.android.model.Camera;

import java.io.File;
import java.util.List;

/**
 * Example implementation showing how to use thread-safe components
 * to prevent ANR issues in activities
 */
public class ThreadSafetyExample extends Activity {
    private static final String TAG = "ThreadSafetyExample";
    
    private ThreadSafeAppPreference preferences;
    private ThreadSafeDBManager dbManager;
    private ThreadSafeFileUtils fileUtils;
    private ANRHandler anrHandler;
    private CrashLogger logger;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize components
        initializeComponents();
        
        // Example: Load data safely
        loadDataSafely();
        
        // Example: Save preferences safely
        savePreferencesSafely();
        
        // Example: Perform file operation safely
        performFileOperationSafely();
    }
    
    /**
     * Initialize thread-safe components
     */
    private void initializeComponents() {
        preferences = ThreadSafeAppPreference.getInstance();
        dbManager = ThreadSafeDBManager.getInstance(this);
        fileUtils = ThreadSafeFileUtils.getInstance();
        anrHandler = ANRHandler.getInstance();
        logger = CrashLogger.getInstance();
        
        logger.i(TAG, "Thread-safe components initialized");
    }
    
    /**
     * Example: Load data from database safely
     */
    private void loadDataSafely() {
        // Show loading indicator
        showLoading(true);
        
        // Load cameras from database asynchronously
        dbManager.getCameras(cameras -> {
            // This callback runs on main thread
            showLoading(false);
            
            if (cameras != null && !cameras.isEmpty()) {
                logger.i(TAG, "Loaded " + cameras.size() + " cameras");
                displayCameras(cameras);
            } else {
                logger.w(TAG, "No cameras found");
                showEmptyState();
            }
        });
    }
    
    /**
     * Example: Save preferences safely
     */
    private void savePreferencesSafely() {
        // Single preference update
        preferences.setBoolean(AppPreference.KEY.AUTO_RECORD, true);
        
        // Batch update for multiple preferences
        preferences.batchUpdate(editor -> {
            editor.putInt(AppPreference.KEY.VIDEO_QUALITY, 1080)
                  .putInt(AppPreference.KEY.VIDEO_FRAME, 30)
                  .putBoolean(AppPreference.KEY.RECORD_AUDIO, true)
                  .putString(AppPreference.KEY.DEVICE_NAME, "MyDevice");
        });
        
        logger.i(TAG, "Preferences saved successfully");
    }
    
    /**
     * Example: Perform file operation safely
     */
    private void performFileOperationSafely() {
        File logFile = new File(getFilesDir(), "app_log.txt");
        String logContent = "Application started at: " + System.currentTimeMillis() + "\n";
        
        // Write to file asynchronously
        fileUtils.appendStringToFile(logFile, logContent, 
            new ThreadSafeFileUtils.FileOperationCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    logger.i(TAG, "Log file updated successfully");
                }
                
                @Override
                public void onError(Exception e) {
                    logger.e(TAG, "Failed to update log file", e);
                }
            });
    }
    
    /**
     * Example: Perform heavy operation with ANR protection
     */
    private void performHeavyOperation() {
        anrHandler.executeOnMainThreadSafe(
            "heavyOperation",
            () -> {
                // Simulate heavy computation
                double result = 0;
                for (int i = 0; i < 1000000; i++) {
                    result += Math.sqrt(i);
                }
                return result;
            },
            result -> {
                // Success - update UI
                Toast.makeText(this, "Result: " + result, Toast.LENGTH_SHORT).show();
            },
            error -> {
                // Error or timeout - show error
                logger.e(TAG, "Heavy operation failed", error);
                Toast.makeText(this, "Operation failed", Toast.LENGTH_SHORT).show();
            },
            3000 // 3 second timeout
        );
    }
    
    /**
     * Example: Add camera with error handling
     */
    private void addCameraSafely(Camera camera) {
        // First check if camera exists
        dbManager.isExistCamera(camera, exists -> {
            if (exists) {
                Toast.makeText(this, "Camera already exists", Toast.LENGTH_SHORT).show();
            } else {
                // Add camera
                dbManager.addCamera(camera, new ThreadSafeDBManager.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            logger.i(TAG, "Camera added successfully");
                            Toast.makeText(ThreadSafetyExample.this, 
                                "Camera added", Toast.LENGTH_SHORT).show();
                            // Refresh camera list
                            loadDataSafely();
                        }
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        logger.e(TAG, "Failed to add camera", e);
                        Toast.makeText(ThreadSafetyExample.this, 
                            "Failed to add camera", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    /**
     * Example: Delete file safely
     */
    private void deleteFileSafely(File file) {
        fileUtils.deleteFile(file, new ThreadSafeFileUtils.FileOperationCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    logger.i(TAG, "File deleted: " + file.getName());
                    Toast.makeText(ThreadSafetyExample.this, 
                        "File deleted", Toast.LENGTH_SHORT).show();
                } else {
                    logger.w(TAG, "Failed to delete file: " + file.getName());
                }
            }
            
            @Override
            public void onError(Exception e) {
                logger.e(TAG, "Error deleting file", e);
                Toast.makeText(ThreadSafetyExample.this, 
                    "Error deleting file", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Example: Background task with progress
     */
    private void performBackgroundTask() {
        anrHandler.executeBackgroundTask(
            "downloadData",
            () -> {
                // Simulate network download
                for (int i = 0; i <= 100; i += 10) {
                    final int progress = i;
                    runOnUiThread(() -> updateProgress(progress));
                    
                    try {
                        Thread.sleep(500); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Download interrupted");
                    }
                }
                return "Download complete";
            },
            result -> {
                // Success on main thread
                updateProgress(100);
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            },
            error -> {
                // Error on main thread
                logger.e(TAG, "Background task failed", error);
                Toast.makeText(this, "Task failed", Toast.LENGTH_SHORT).show();
            }
        );
    }
    
    // Helper methods
    
    private void showLoading(boolean show) {
        // Update UI to show/hide loading indicator
    }
    
    private void displayCameras(List<Camera> cameras) {
        // Update UI with camera list
    }
    
    private void showEmptyState() {
        // Show empty state UI
    }
    
    private void updateProgress(int progress) {
        // Update progress bar
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any pending operations if needed
        anrHandler.cancelAllTasks();
    }
}