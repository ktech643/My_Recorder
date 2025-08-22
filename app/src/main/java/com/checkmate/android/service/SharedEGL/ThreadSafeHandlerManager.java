package com.checkmate.android.service.SharedEGL;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadSafeHandlerManager {
    private static final String TAG = "ThreadSafeHandlerManager";

    public Handler getMainThreadHandler(){
        return  mainThreadHandler;
    }
    // Main thread handler for UI updates
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d(TAG, "Handling message on main thread");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    // Background thread handler for non-UI tasks
    private Handler backgroundHandler;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    public ThreadSafeHandlerManager() {
        backgroundExecutor.execute(() -> {
            Looper.prepare();
            backgroundHandler = new Handler(Looper.myLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 2:
                            Log.d(TAG, "Handling message on background thread");
                            break;
                        default:
                            super.handleMessage(msg);
                    }
                }
            };
            Looper.loop();
        });
    }

    // === Main Thread Methods ===
    public void post(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainThreadHandler.post(task);
        }
    }

    public void postDelayed(Runnable task, long delayMillis) {
        mainThreadHandler.postDelayed(task, delayMillis);
    }

    public void postAtTime(Runnable task, long uptimeMillis) {
        mainThreadHandler.postAtTime(task, uptimeMillis);
    }

    public void removeCallbacks(Runnable task) {
        mainThreadHandler.removeCallbacks(task);
    }

    // === Background Thread Methods ===
    public void postToBackgroundThread(Runnable task) {
        if (backgroundHandler != null) {
            backgroundHandler.post(task);
        } else {
            backgroundExecutor.execute(task);
        }
    }

    public void postDelayedToBackgroundThread(Runnable task, long delayMillis) {
        if (backgroundHandler != null) {
            backgroundHandler.postDelayed(task, delayMillis);
        } else {
            backgroundExecutor.execute(() -> {
                try {
                    Thread.sleep(delayMillis);
                    task.run();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Background task interrupted", e);
                }
            });
        }
    }

    public void postAtTimeToBackgroundThread(Runnable task, long uptimeMillis) {
        if (backgroundHandler != null) {
            backgroundHandler.postAtTime(task, uptimeMillis);
        } else {
            long delay = uptimeMillis - SystemClock.uptimeMillis();
            if (delay > 0) {
                postDelayedToBackgroundThread(task, delay);
            } else {
                postToBackgroundThread(task);
            }
        }
    }

    public void removeCallbacksFromBackgroundThread(Runnable task) {
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacks(task);
        }
    }

    // Cleanup
    public void shutdown() {
        if (backgroundHandler != null) {
            backgroundHandler.getLooper().quitSafely();
        }
        backgroundExecutor.shutdown();
    }
}
