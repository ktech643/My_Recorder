package com.checkmate.android.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

/**
 * DialogSynchronizer ensures that only one dialog is shown at a time
 * to prevent UI freezing and window leaks.
 */
public class DialogSynchronizer {
    private static final String TAG = "DialogSynchronizer";
    private static final long DIALOG_DELAY_MS = 300; // Delay between dialogs
    
    private final WeakReference<Activity> activityRef;
    private final Handler mainHandler;
    private final Queue<DialogRequest> dialogQueue;
    private Dialog currentDialog;
    private boolean isShowingDialog;
    private final Object lock = new Object();
    
    public interface DialogBuilder {
        Dialog buildDialog(Activity activity);
    }
    
    public interface DialogCallback {
        void onDialogShown(Dialog dialog);
        void onDialogDismissed();
    }
    
    private static class DialogRequest {
        DialogBuilder builder;
        DialogCallback callback;
        String tag;
        
        DialogRequest(DialogBuilder builder, DialogCallback callback, String tag) {
            this.builder = builder;
            this.callback = callback;
            this.tag = tag;
        }
    }
    
    public DialogSynchronizer(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.dialogQueue = new LinkedList<>();
        this.isShowingDialog = false;
    }
    
    /**
     * Show a dialog in a synchronized manner
     */
    public void showDialog(DialogBuilder builder, DialogCallback callback, String tag) {
        synchronized (lock) {
            DialogRequest request = new DialogRequest(builder, callback, tag);
            dialogQueue.offer(request);
            
            if (!isShowingDialog) {
                processNextDialog();
            }
        }
    }
    
    /**
     * Show a dialog with default callback
     */
    public void showDialog(DialogBuilder builder, String tag) {
        showDialog(builder, null, tag);
    }
    
    /**
     * Process the next dialog in the queue
     */
    private void processNextDialog() {
        synchronized (lock) {
            if (dialogQueue.isEmpty() || isShowingDialog) {
                return;
            }
            
            final Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                Log.w(TAG, "Activity is not available, clearing dialog queue");
                dialogQueue.clear();
                return;
            }
            
            isShowingDialog = true;
            final DialogRequest request = dialogQueue.poll();
            
            if (request == null) {
                isShowingDialog = false;
                return;
            }
            
            mainHandler.postDelayed(() -> {
                try {
                    showDialogInternal(request);
                } catch (Exception e) {
                    Log.e(TAG, "Error showing dialog: " + request.tag, e);
                    synchronized (lock) {
                        isShowingDialog = false;
                    }
                    processNextDialog();
                }
            }, DIALOG_DELAY_MS);
        }
    }
    
    /**
     * Show dialog internally
     */
    private void showDialogInternal(DialogRequest request) {
        final Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            synchronized (lock) {
                isShowingDialog = false;
            }
            processNextDialog();
            return;
        }
        
        try {
            currentDialog = request.builder.buildDialog(activity);
            if (currentDialog == null) {
                Log.e(TAG, "Dialog builder returned null for: " + request.tag);
                synchronized (lock) {
                    isShowingDialog = false;
                }
                processNextDialog();
                return;
            }
            
            // Set dismiss listener to process next dialog
            currentDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    synchronized (lock) {
                        currentDialog = null;
                        isShowingDialog = false;
                    }
                    
                    if (request.callback != null) {
                        try {
                            request.callback.onDialogDismissed();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in dialog dismiss callback", e);
                        }
                    }
                    
                    // Process next dialog after a delay
                    mainHandler.postDelayed(() -> processNextDialog(), 200);
                }
            });
            
            // Show the dialog
            currentDialog.show();
            Log.d(TAG, "Showing dialog: " + request.tag);
            
            if (request.callback != null) {
                try {
                    request.callback.onDialogShown(currentDialog);
                } catch (Exception e) {
                    Log.e(TAG, "Error in dialog shown callback", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + request.tag, e);
            synchronized (lock) {
                currentDialog = null;
                isShowingDialog = false;
            }
            processNextDialog();
        }
    }
    
    /**
     * Dismiss current dialog if any
     */
    public void dismissCurrentDialog() {
        synchronized (lock) {
            if (currentDialog != null && currentDialog.isShowing()) {
                try {
                    currentDialog.dismiss();
                } catch (Exception e) {
                    Log.e(TAG, "Error dismissing dialog", e);
                }
                currentDialog = null;
            }
        }
    }
    
    /**
     * Clear all pending dialogs
     */
    public void clearAllDialogs() {
        synchronized (lock) {
            dialogQueue.clear();
            dismissCurrentDialog();
            isShowingDialog = false;
        }
    }
    
    /**
     * Check if a dialog is currently showing
     */
    public boolean isShowingDialog() {
        synchronized (lock) {
            return isShowingDialog;
        }
    }
    
    /**
     * Get the number of pending dialogs
     */
    public int getPendingDialogCount() {
        synchronized (lock) {
            return dialogQueue.size();
        }
    }
}