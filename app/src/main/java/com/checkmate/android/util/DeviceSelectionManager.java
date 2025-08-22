package com.checkmate.android.util;

import android.content.Context;
import android.util.Log;

import com.checkmate.android.AppPreference;

/**
 * Utility class to manage device selection and prevent unauthorized camera switching
 */
public class DeviceSelectionManager {
    private static final String TAG = "DeviceSelectionManager";
    
    // Preference keys for device selection management
    private static final String PREF_LAST_CAMERA_SELECTION = "LAST_CAMERA_SELECTION";
    private static final String PREF_CAMERA_SELECTION_TIMESTAMP = "CAMERA_SELECTION_TIMESTAMP";
    private static final String PREF_USB_AUTO_CONNECT = "USB_AUTO_CONNECT";
    private static final String PREF_USB_LAST_USER_SELECTION = "USB_LAST_USER_SELECTION";
    private static final String PREF_USB_SELECTION_TIMESTAMP = "USB_SELECTION_TIMESTAMP";
    
    // Timeout constants
    private static final long SELECTION_TIMEOUT_MS = 30000; // 30 seconds
    private static final long USB_SELECTION_TIMEOUT_MS = 60000; // 1 minute
    
    private static DeviceSelectionManager instance;
    private final Context context;
    
    private DeviceSelectionManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static synchronized DeviceSelectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceSelectionManager(context);
        }
        return instance;
    }
    
    /**
     * Validate current camera selection and prevent unauthorized changes
     */
    public boolean validateCameraSelection() {
        String currentPosition = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0");
        String lastSelection = AppPreference.getStr(PREF_LAST_CAMERA_SELECTION, "");
        long selectionTime = AppPreference.getLong(PREF_CAMERA_SELECTION_TIMESTAMP, 0);
        
        // Check if selection is recent and valid
        if (!lastSelection.isEmpty() && 
            lastSelection.equals(currentPosition) && 
            (System.currentTimeMillis() - selectionTime) < SELECTION_TIMEOUT_MS) {
            
            Log.d(TAG, "Valid camera selection detected: " + currentPosition);
            return true;
        }
        
        // Allow default camera on first launch
        if (lastSelection.isEmpty() && currentPosition.equals("0")) {
            Log.d(TAG, "First launch with default camera, allowing");
            return true;
        }
        
        Log.w(TAG, "Invalid camera selection detected: current=" + currentPosition + ", last=" + lastSelection);
        return false;
    }
    
    /**
     * Handle user camera selection change
     */
    public void onUserCameraSelection(String newPosition) {
        Log.d(TAG, "User selected camera: " + newPosition);
        
        // Update preferences
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, newPosition);
        AppPreference.setStr(PREF_LAST_CAMERA_SELECTION, newPosition);
        AppPreference.setLong(PREF_CAMERA_SELECTION_TIMESTAMP, System.currentTimeMillis());
        
        // Clear any conflicting USB preferences if not USB camera
        if (!newPosition.equals("2")) {
            AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, "");
        }
        
        Log.d(TAG, "User camera selection saved: " + newPosition);
    }
    
    /**
     * Reset to default camera when invalid selection detected
     */
    public void resetToDefaultCamera() {
        Log.d(TAG, "Resetting to default camera");
        
        // Clear any USB camera preferences that might cause issues
        AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, "");
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, "0");
        
        // Reset selection tracking
        AppPreference.setStr(PREF_LAST_CAMERA_SELECTION, "0");
        AppPreference.setLong(PREF_CAMERA_SELECTION_TIMESTAMP, System.currentTimeMillis());
    }
    
    /**
     * Check if USB auto-connect should be allowed
     */
    public boolean shouldAllowUSBAutoConnect() {
        // Check if auto-connect is disabled by user
        if (!AppPreference.getBool(PREF_USB_AUTO_CONNECT, true)) {
            Log.d(TAG, "Auto-connect disabled by user preference");
            return false;
        }
        
        // Check if this is a recent user selection
        String lastSelection = AppPreference.getStr(PREF_USB_LAST_USER_SELECTION, "");
        long selectionTime = AppPreference.getLong(PREF_USB_SELECTION_TIMESTAMP, 0);
        
        if (!lastSelection.isEmpty() && 
            (System.currentTimeMillis() - selectionTime) < USB_SELECTION_TIMEOUT_MS) {
            Log.d(TAG, "Recent USB selection found, allowing auto-connect");
            return true;
        }
        
        Log.d(TAG, "No recent USB selection, requiring user confirmation");
        return false;
    }
    
    /**
     * Handle user USB device selection
     */
    public void onUserUSBDeviceSelection(String deviceName) {
        Log.d(TAG, "User selected USB device: " + deviceName);
        
        // Save to preferences
        AppPreference.setStr(AppPreference.KEY.USB_CAMERA_NAME, deviceName);
        AppPreference.setStr(PREF_USB_LAST_USER_SELECTION, deviceName);
        AppPreference.setLong(PREF_USB_SELECTION_TIMESTAMP, System.currentTimeMillis());
        
        // Set camera position to USB
        AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, "2");
        
        Log.d(TAG, "User USB device selection saved: " + deviceName);
    }
    
    /**
     * Check if current selection is valid for the connected devices
     */
    public boolean isCurrentSelectionValid() {
        String currentPosition = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0");
        
        switch (currentPosition) {
            case "0": // Rear camera
            case "1": // Front camera
                return true; // Built-in cameras are always valid
                
            case "2": // USB camera
                String usbDevice = AppPreference.getStr(AppPreference.KEY.USB_CAMERA_NAME, "");
                return !usbDevice.isEmpty();
                
            case "3": // Screen cast
                return true; // Screen cast is always available
                
            case "4": // Audio only
                return true; // Audio only is always available
                
            default:
                return false;
        }
    }
    
    /**
     * Get recommended fallback camera when current selection is invalid
     */
    public String getRecommendedFallbackCamera() {
        String currentPosition = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, "0");
        
        // If USB camera is selected but no device connected, fall back to rear camera
        if (currentPosition.equals("2")) {
            String usbDevice = AppPreference.getStr(AppPreference.KEY.USB_CAMERA_NAME, "");
            if (usbDevice.isEmpty()) {
                Log.w(TAG, "USB camera selected but no device connected, falling back to rear camera");
                return "0";
            }
        }
        
        // Default fallback to rear camera
        return "0";
    }
}
