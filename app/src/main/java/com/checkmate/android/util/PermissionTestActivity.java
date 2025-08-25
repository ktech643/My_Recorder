package com.checkmate.android.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.checkmate.android.R;

import java.util.List;

/**
 * Test activity to verify permission flow implementation
 * This can be used to test for UI freezing, crashes, and window leaks
 */
public class PermissionTestActivity extends AppCompatActivity {
    private static final String TAG = "PermissionTest";
    
    private TextView statusText;
    private Button testButton;
    private PermissionManager permissionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple test layout
        setContentView(android.R.layout.simple_list_item_2);
        
        statusText = new TextView(this);
        testButton = new Button(this);
        testButton.setText("Test Permissions");
        
        // Initialize permission manager
        permissionManager = new PermissionManager(this, new PermissionManager.PermissionCallback() {
            @Override
            public void onAllPermissionsGranted() {
                Log.d(TAG, "All permissions granted!");
                updateStatus("All permissions granted!");
            }
            
            @Override
            public void onPermissionsDenied(List<String> deniedPermissions) {
                Log.w(TAG, "Permissions denied: " + deniedPermissions);
                updateStatus("Some permissions denied: " + deniedPermissions.size());
            }
            
            @Override
            public void onPermissionRequestCancelled() {
                Log.w(TAG, "Permission request cancelled");
                updateStatus("Permission request cancelled");
            }
        });
        
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPermissionFlow();
            }
        });
    }
    
    private void testPermissionFlow() {
        updateStatus("Starting permission flow...");
        
        // Log current permission status
        logPermissionStatus();
        
        // Start permission flow
        permissionManager.requestAllPermissions();
    }
    
    private void logPermissionStatus() {
        Log.d(TAG, "=== Current Permission Status ===");
        
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION
        };
        
        for (String permission : permissions) {
            boolean granted = ContextCompat.checkSelfPermission(this, permission) 
                == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, permission + ": " + (granted ? "GRANTED" : "DENIED"));
        }
        
        Log.d(TAG, "==============================");
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(TAG, "onRequestPermissionsResult - requestCode: " + requestCode);
        
        // Let permission manager handle the result
        if (permissionManager != null) {
            permissionManager.handlePermissionResult(requestCode, permissions, grantResults);
        }
    }
    
    private void updateStatus(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    statusText.setText(status);
                }
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (permissionManager != null) {
            permissionManager.cancelPermissionFlow();
        }
    }
}