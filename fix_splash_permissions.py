import re

with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'r') as f:
    content = f.read()

# Define the old method
old_method = '''    private void requestAdditionalPermissions(Activity activity) {
        // Use a handler to delay requests slightly to avoid conflicts
        new Handler().postDelayed(() -> {
            requestDrawOverAppsPermission(activity);
        }, 100);
        
        new Handler().postDelayed(() -> {
            requestIgnoreBatteryOptimizationsPermission(activity);
        }, 200);
        
        new Handler().postDelayed(() -> {
            requestDoNotDisturbPermission(activity);
        }, 300);
        
        // Storage permissions are handled separately in continueWithActivation
        // Removed verifyStoragePermissions to prevent infinite loop
        new Handler().postDelayed(() -> {
            requestModifySystemSettingsPermission(activity);
        }, 500);
        
        new Handler().postDelayed(() -> {
            gotoNextView();
        }, 1000);
    }'''

# Define the new sequential method
new_method = '''    private void requestAdditionalPermissions(Activity activity) {
        Log.d(TAG, "Starting sequential permission flow to prevent system overload");
        
        // Request permissions one at a time with proper callbacks to avoid overwhelming the system
        requestOverlayPermissionSequential(activity, () -> {
            requestBatteryOptimizationSequential(activity, () -> {
                requestSystemSettingsSequential(activity, () -> {
                    Log.d(TAG, "All additional permissions processed, continuing to main");
                    gotoNextView();
                });
            });
        });
    }
    
    private void requestOverlayPermissionSequential(Activity activity, Runnable onComplete) {
        if (!Settings.canDrawOverlays(activity)) {
            Log.d(TAG, "Requesting overlay permission");
            try {
                requestDrawOverAppsPermission(activity);
                new Handler().postDelayed(onComplete, 3000);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting overlay permission", e);
                onComplete.run();
            }
        } else {
            onComplete.run();
        }
    }
    
    private void requestBatteryOptimizationSequential(Activity activity, Runnable onComplete) {
        try {
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                Log.d(TAG, "Requesting battery optimization");
                requestIgnoreBatteryOptimizationsPermission(activity);
                new Handler().postDelayed(onComplete, 3000);
            } else {
                onComplete.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting battery optimization", e);
            onComplete.run();
        }
    }
    
    private void requestSystemSettingsSequential(Activity activity, Runnable onComplete) {
        try {
            if (!Settings.System.canWrite(activity)) {
                Log.d(TAG, "Requesting system settings access");
                requestModifySystemSettingsPermission(activity);
                new Handler().postDelayed(onComplete, 3000);
            } else {
                onComplete.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting system settings", e);
            onComplete.run();
        }
    }'''

# Replace the method
content = content.replace(old_method, new_method)

with open('app/src/main/java/com/checkmate/android/ui/activity/SplashActivity.java', 'w') as f:
    f.write(content)

print("Successfully updated permission flow")
