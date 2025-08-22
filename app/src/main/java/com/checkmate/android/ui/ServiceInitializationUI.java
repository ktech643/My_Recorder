package com.checkmate.android.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.checkmate.android.R;

import java.lang.ref.WeakReference;

/**
 * UI manager for showing initialization progress during service recovery
 */
public class ServiceInitializationUI {
    private static final String TAG = "ServiceInitializationUI";
    
    private static volatile ServiceInitializationUI instance;
    
    private WeakReference<Context> contextRef;
    private Dialog loadingDialog;
    private TextView statusText;
    private ProgressBar progressBar;
    private TextView progressText;
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private ServiceInitializationUI() {}
    
    public static ServiceInitializationUI getInstance() {
        if (instance == null) {
            synchronized (ServiceInitializationUI.class) {
                if (instance == null) {
                    instance = new ServiceInitializationUI();
                }
            }
        }
        return instance;
    }
    
    /**
     * Show initialization loader
     */
    public void showLoader(@NonNull Context context, String serviceName) {
        this.contextRef = new WeakReference<>(context);
        
        mainHandler.post(() -> {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    updateStatus("Initializing " + serviceName + "...");
                    return;
                }
                
                Context ctx = contextRef.get();
                if (ctx == null) return;
                
                // Create custom dialog
                loadingDialog = new Dialog(ctx);
                loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                loadingDialog.setCancelable(false);
                loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                
                // Inflate custom layout
                LayoutInflater inflater = LayoutInflater.from(ctx);
                View dialogView = inflater.inflate(R.layout.dialog_service_initialization, null);
                
                // Initialize views
                statusText = dialogView.findViewById(R.id.tv_status);
                progressBar = dialogView.findViewById(R.id.progress_bar);
                progressText = dialogView.findViewById(R.id.tv_progress);
                
                // Set initial status
                statusText.setText("Initializing " + serviceName + "...");
                progressText.setText("Please wait...");
                
                loadingDialog.setContentView(dialogView);
                
                // Adjust dialog size
                Window window = loadingDialog.getWindow();
                if (window != null) {
                    window.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                }
                
                loadingDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Update initialization status
     */
    public void updateStatus(String status) {
        mainHandler.post(() -> {
            if (statusText != null) {
                statusText.setText(status);
            }
        });
    }
    
    /**
     * Update progress
     */
    public void updateProgress(String progress) {
        mainHandler.post(() -> {
            if (progressText != null) {
                progressText.setText(progress);
            }
        });
    }
    
    /**
     * Show error during initialization
     */
    public void showError(String error) {
        mainHandler.post(() -> {
            if (statusText != null) {
                statusText.setText("Error: " + error);
                statusText.setTextColor(Color.RED);
            }
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            
            // Auto dismiss after 3 seconds
            mainHandler.postDelayed(this::hideLoader, 3000);
        });
    }
    
    /**
     * Hide loader
     */
    public void hideLoader() {
        mainHandler.post(() -> {
            try {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                loadingDialog = null;
                statusText = null;
                progressBar = null;
                progressText = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Show recovery notification
     */
    public void showRecoveryNotification(Context context, String serviceName, boolean success) {
        if (context == null) return;
        
        mainHandler.post(() -> {
            try {
                String message = success ? 
                    serviceName + " recovered successfully" : 
                    "Failed to recover " + serviceName;
                
                // You can show a toast or snackbar here
                // For now, just update the loader if it's showing
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    updateStatus(message);
                    if (success) {
                        progressText.setText("✓ Ready");
                        progressText.setTextColor(Color.GREEN);
                    } else {
                        progressText.setText("✗ Failed");
                        progressText.setTextColor(Color.RED);
                    }
                    
                    // Hide after 2 seconds
                    mainHandler.postDelayed(this::hideLoader, 2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Check if loader is showing
     */
    public boolean isLoaderShowing() {
        return loadingDialog != null && loadingDialog.isShowing();
    }
}