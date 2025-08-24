package com.checkmate.android.service;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.checkmate.android.util.CrashLogger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe and null-safe enhancements for BgCameraService
 * This class provides helper methods and patterns for safe camera operations
 */
public class BgCameraServiceEnhanced {
    private static final String TAG = "BgCameraServiceEnhanced";
    private static final CrashLogger crashLogger = CrashLogger.getInstance();
    
    // Thread safety locks
    private final ReentrantLock cameraLock = new ReentrantLock();
    private final ReentrantLock sessionLock = new ReentrantLock();
    private final ReentrantLock surfaceLock = new ReentrantLock();
    
    /**
     * Safe camera operation wrapper with null checks and thread safety
     */
    public interface CameraOperation {
        void execute(@NonNull CameraDevice camera) throws Exception;
    }
    
    /**
     * Safe session operation wrapper
     */
    public interface SessionOperation {
        void execute(@NonNull CameraCaptureSession session) throws Exception;
    }
    
    /**
     * Execute camera operation safely
     */
    public static void executeCameraOperation(@Nullable CameraDevice camera, 
                                            @NonNull CameraOperation operation,
                                            @NonNull String operationName) {
        if (camera == null) {
            Log.w(TAG, operationName + ": Camera device is null");
            if (crashLogger != null) {
                crashLogger.logWarning(TAG, operationName + " called with null camera");
            }
            return;
        }
        
        try {
            operation.execute(camera);
        } catch (Exception e) {
            Log.e(TAG, "Error in " + operationName, e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, operationName, e);
            }
        }
    }
    
    /**
     * Execute session operation safely
     */
    public static void executeSessionOperation(@Nullable CameraCaptureSession session,
                                             @NonNull SessionOperation operation,
                                             @NonNull String operationName) {
        if (session == null) {
            Log.w(TAG, operationName + ": Camera session is null");
            if (crashLogger != null) {
                crashLogger.logWarning(TAG, operationName + " called with null session");
            }
            return;
        }
        
        try {
            operation.execute(session);
        } catch (Exception e) {
            Log.e(TAG, "Error in " + operationName, e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, operationName, e);
            }
        }
    }
    
    /**
     * Safe handler post with null check
     */
    public static void postToHandler(@Nullable Handler handler, @NonNull Runnable runnable) {
        if (handler == null) {
            Log.w(TAG, "Handler is null, cannot post runnable");
            return;
        }
        
        try {
            handler.post(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error in handler runnable", e);
                    if (crashLogger != null) {
                        crashLogger.logError(TAG, "Handler runnable error", e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error posting to handler", e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, "postToHandler", e);
            }
        }
    }
    
    /**
     * Safe delayed handler post
     */
    public static void postDelayedToHandler(@Nullable Handler handler, 
                                          @NonNull Runnable runnable, 
                                          long delayMillis) {
        if (handler == null) {
            Log.w(TAG, "Handler is null, cannot post delayed runnable");
            return;
        }
        
        try {
            handler.postDelayed(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed handler runnable", e);
                    if (crashLogger != null) {
                        crashLogger.logError(TAG, "Delayed handler runnable error", e);
                    }
                }
            }, delayMillis);
        } catch (Exception e) {
            Log.e(TAG, "Error posting delayed to handler", e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, "postDelayedToHandler", e);
            }
        }
    }
    
    /**
     * Thread-safe camera close operation
     */
    public void closeCamera(@Nullable CameraDevice camera, @NonNull ReentrantLock lock) {
        if (camera == null) {
            return;
        }
        
        lock.lock();
        try {
            camera.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing camera", e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, "closeCamera", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Thread-safe session close operation
     */
    public void closeSession(@Nullable CameraCaptureSession session, @NonNull ReentrantLock lock) {
        if (session == null) {
            return;
        }
        
        lock.lock();
        try {
            session.close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing session", e);
            if (crashLogger != null) {
                crashLogger.logError(TAG, "closeSession", e);
            }
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Safe camera state callback wrapper
     */
    public static abstract class SafeCameraStateCallback extends CameraDevice.StateCallback {
        private final String deviceId;
        
        public SafeCameraStateCallback(String deviceId) {
            this.deviceId = deviceId;
        }
        
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                onCameraOpened(camera);
            } catch (Exception e) {
                Log.e(TAG, "Error in onOpened callback", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "SafeCameraStateCallback.onOpened", e);
                }
            }
        }
        
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            try {
                onCameraDisconnected(camera);
            } catch (Exception e) {
                Log.e(TAG, "Error in onDisconnected callback", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "SafeCameraStateCallback.onDisconnected", e);
                }
            }
        }
        
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            try {
                Log.e(TAG, "Camera error for device " + deviceId + ": " + error);
                onCameraError(camera, error);
            } catch (Exception e) {
                Log.e(TAG, "Error in onError callback", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "SafeCameraStateCallback.onError", e);
                }
            }
        }
        
        // Abstract methods to be implemented by subclass
        protected abstract void onCameraOpened(@NonNull CameraDevice camera);
        protected abstract void onCameraDisconnected(@NonNull CameraDevice camera);
        protected abstract void onCameraError(@NonNull CameraDevice camera, int error);
    }
    
    /**
     * Safe session state callback wrapper
     */
    public static abstract class SafeSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                onSessionConfigured(session);
            } catch (Exception e) {
                Log.e(TAG, "Error in onConfigured callback", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "SafeSessionStateCallback.onConfigured", e);
                }
            }
        }
        
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            try {
                Log.e(TAG, "Session configuration failed");
                onSessionConfigureFailed(session);
            } catch (Exception e) {
                Log.e(TAG, "Error in onConfigureFailed callback", e);
                if (crashLogger != null) {
                    crashLogger.logError(TAG, "SafeSessionStateCallback.onConfigureFailed", e);
                }
            }
        }
        
        // Abstract methods to be implemented by subclass
        protected abstract void onSessionConfigured(@NonNull CameraCaptureSession session);
        protected abstract void onSessionConfigureFailed(@NonNull CameraCaptureSession session);
    }
}