package com.checkmate.android.util;

import android.app.Activity;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dialog Manager to prevent window leaks and manage dialog lifecycle properly
 */
public class DialogManager {
    private static final String TAG = "DialogManager";
    
    private final WeakReference<Activity> activityRef;
    private final ConcurrentHashMap<String, AlertDialog> activeDialogs = new ConcurrentHashMap<>();
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    
    public DialogManager(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }
    
    /**
     * Show a dialog with automatic lifecycle management
     */
    public void showDialog(String dialogId, AlertDialog dialog) {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || isDestroyed.get()) {
            Log.w(TAG, "Cannot show dialog - activity is not in valid state");
            return;
        }
        
        try {
            // Dismiss any existing dialog with the same ID
            dismissDialog(dialogId);
            
            // Set up dialog dismissal listener to clean up our reference
            dialog.setOnDismissListener(dialogInterface -> {
                activeDialogs.remove(dialogId);
                Log.d(TAG, "Dialog dismissed and removed: " + dialogId);
            });
            
            // Store the dialog reference
            activeDialogs.put(dialogId, dialog);
            
            // Show the dialog
            dialog.show();
            Log.d(TAG, "Dialog shown: " + dialogId);
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing dialog: " + dialogId, e);
            activeDialogs.remove(dialogId);
        }
    }
    
    /**
     * Dismiss a specific dialog
     */
    public void dismissDialog(String dialogId) {
        AlertDialog dialog = activeDialogs.get(dialogId);
        if (dialog != null) {
            try {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } catch (Exception e) {
                Log.w(TAG, "Error dismissing dialog: " + dialogId, e);
            } finally {
                activeDialogs.remove(dialogId);
            }
        }
    }
    
    /**
     * Dismiss all active dialogs
     */
    public void dismissAllDialogs() {
        Log.d(TAG, "Dismissing all dialogs. Count: " + activeDialogs.size());
        
        for (String dialogId : activeDialogs.keySet()) {
            dismissDialog(dialogId);
        }
        
        activeDialogs.clear();
    }
    
    /**
     * Check if a specific dialog is showing
     */
    public boolean isDialogShowing(String dialogId) {
        AlertDialog dialog = activeDialogs.get(dialogId);
        return dialog != null && dialog.isShowing();
    }
    
    /**
     * Check if any dialog is showing
     */
    public boolean hasActiveDialogs() {
        return !activeDialogs.isEmpty();
    }
    
    /**
     * Get the count of active dialogs
     */
    public int getActiveDialogCount() {
        return activeDialogs.size();
    }
    
    /**
     * Check if the activity is still valid for showing dialogs
     */
    public boolean isActivityValid() {
        Activity activity = activityRef.get();
        return activity != null && !activity.isFinishing() && !activity.isDestroyed() && !isDestroyed.get();
    }
    
    /**
     * Clean up all dialogs and mark as destroyed
     */
    public void cleanup() {
        Log.d(TAG, "Cleaning up DialogManager");
        
        isDestroyed.set(true);
        dismissAllDialogs();
        
        // Clear the activity reference
        Activity activity = activityRef.get();
        if (activity != null) {
            activityRef.clear();
        }
    }
    
    /**
     * Get dialog builder with automatic lifecycle management
     */
    public AlertDialog.Builder createDialogBuilder() {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed() || isDestroyed.get()) {
            throw new IllegalStateException("Cannot create dialog - activity is not in valid state");
        }
        
        return new AlertDialog.Builder(activity);
    }
}