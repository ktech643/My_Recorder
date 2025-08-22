package com.checkmate.android.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.checkmate.android.AppConstant;
import com.checkmate.android.R;
import com.checkmate.android.adapter.SpinnerAdapter;
import com.checkmate.android.model.RotateModel;
import com.checkmate.android.ui.view.EnhancedSpinner;
import com.checkmate.android.util.SpinnerUtils;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced LiveFragment demonstrating the use of EnhancedSpinner
 * This replaces the old MySpinner implementation with modern, Material Design spinners
 */
public class LiveFragment_Enhanced extends Fragment {

    private static final String TAG = "LiveFragment_Enhanced";
    
    // UI Components
    private View mView;
    private ConstraintLayout frame_camera;
    private LinearLayout ly_stream;
    private LinearLayout ly_rotate;
    private LinearLayout ly_camera_type;
    private LinearLayout ly_rec;
    private LinearLayout ly_snap;
    private Button btn_refresh;
    private TextView txt_gps;
    
    // Enhanced Spinners (replacing MySpinner)
    private EnhancedSpinner spinner_camera;
    private EnhancedSpinner spinner_rotate;
    
    // Data
    private List<String> cam_spinnerArray = new ArrayList<>();
    private List<RotateModel> rotate_spinnerArray = new ArrayList<>();
    
    // ViewModel
    private SharedViewModel sharedViewModel;
    
    // Handler for UI updates
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    
    // Instance reference
    private static WeakReference<LiveFragment_Enhanced> instance;

    public static LiveFragment_Enhanced newInstance() {
        return new LiveFragment_Enhanced();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_live, container, false);
        
        // Initialize UI components
        initUIComponents();
        
        // Setup Enhanced Spinners
        setupEnhancedSpinners();
        
        // Setup click listeners
        setupClickListeners();
        
        // Initialize data
        initializeData();
        
        // Set instance reference
        instance = new WeakReference<>(this);
        
        return mView;
    }

    /**
     * Initialize all UI components
     */
    private void initUIComponents() {
        // Main layout components
        frame_camera = mView.findViewById(R.id.frame_camera);
        ly_stream = mView.findViewById(R.id.ly_stream);
        ly_rotate = mView.findViewById(R.id.ly_rotate);
        ly_camera_type = mView.findViewById(R.id.ly_camera_type);
        ly_rec = mView.findViewById(R.id.ly_rec);
        ly_snap = mView.findViewById(R.id.ly_snap);
        btn_refresh = mView.findViewById(R.id.btn_refresh);
        txt_gps = mView.findViewById(R.id.txt_gps);
        
        // Enhanced Spinners
        spinner_camera = mView.findViewById(R.id.spinner_camera);
        spinner_rotate = mView.findViewById(R.id.spinner_rotate);
        
        // Initialize ViewModel
        if (getActivity() != null) {
            sharedViewModel = new androidx.lifecycle.ViewModelProvider(getActivity()).get(SharedViewModel.class);
        }
    }

    /**
     * Setup Enhanced Spinners with modern Material Design
     * This replaces the old MySpinner setup
     */
    private void setupEnhancedSpinners() {
        // Setup Camera Spinner
        setupCameraSpinner();
        
        // Setup Rotate Spinner
        setupRotateSpinner();
    }

    /**
     * Setup Camera Spinner with EnhancedSpinner
     */
    private void setupCameraSpinner() {
        // Create camera options
        cam_spinnerArray.clear();
        cam_spinnerArray.add("Front Camera");
        cam_spinnerArray.add("Back Camera");
        cam_spinnerArray.add("USB Camera");
        
        // Setup EnhancedSpinner using utility class
        SpinnerUtils.setupEnhancedSpinner(
            spinner_camera,
            cam_spinnerArray,
            new EnhancedSpinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(int position, String item) {
                    handleCameraSelection(position, item);
                }
            }
        );
        
        // Customize appearance
        spinner_camera.setPrimaryColor(getResources().getColor(R.color.colorPrimary));
        spinner_camera.setAccentColor(getResources().getColor(R.color.colorAccent));
    }

    /**
     * Setup Rotate Spinner with EnhancedSpinner
     */
    private void setupRotateSpinner() {
        // Create rotate options
        rotate_spinnerArray.clear();
        rotate_spinnerArray.add(new RotateModel("0°", 0));
        rotate_spinnerArray.add(new RotateModel("90°", 90));
        rotate_spinnerArray.add(new RotateModel("180°", 180));
        rotate_spinnerArray.add(new RotateModel("270°", 270));
        
        // Convert RotateModel list to String list for EnhancedSpinner
        List<String> rotateStrings = new ArrayList<>();
        for (RotateModel model : rotate_spinnerArray) {
            rotateStrings.add(model.getName());
        }
        
        // Setup EnhancedSpinner
        SpinnerUtils.setupEnhancedSpinner(
            spinner_rotate,
            rotateStrings,
            new EnhancedSpinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(int position, String item) {
                    handleRotateSelection(position, item);
                }
            }
        );
        
        // Customize appearance
        spinner_rotate.setPrimaryColor(getResources().getColor(R.color.colorPrimary));
        spinner_rotate.setAccentColor(getResources().getColor(R.color.colorAccent));
    }

    /**
     * Setup click listeners for all interactive elements
     */
    private void setupClickListeners() {
        // Stream button
        ly_stream.setOnClickListener(v -> onStreamClick());
        
        // Rotate button
        ly_rotate.setOnClickListener(v -> onRotateClick());
        
        // Camera type button
        ly_camera_type.setOnClickListener(v -> onCameraTypeClick());
        
        // Record button
        ly_rec.setOnClickListener(v -> onRecordClick());
        
        // Snapshot button
        ly_snap.setOnClickListener(v -> onSnapshotClick());
        
        // Refresh button
        btn_refresh.setOnClickListener(v -> onRefreshClick());
    }

    /**
     * Initialize data for spinners and other components
     */
    private void initializeData() {
        // Set initial selections
        spinner_camera.setSelection(0); // Front Camera
        spinner_rotate.setSelection(0); // 0°
        
        // Initialize other data as needed
        // This would include camera initialization, GPS setup, etc.
    }

    /**
     * Handle camera selection from EnhancedSpinner
     */
    private void handleCameraSelection(int position, String item) {
        Log.d(TAG, "Camera selected: " + item + " at position: " + position);
        
        // Handle camera switching logic
        switch (position) {
            case 0: // Front Camera
                switchToFrontCamera();
                break;
            case 1: // Back Camera
                switchToBackCamera();
                break;
            case 2: // USB Camera
                switchToUSBCamera();
                break;
        }
        
        // Update UI or perform other actions
        updateCameraUI(position);
    }

    /**
     * Handle rotate selection from EnhancedSpinner
     */
    private void handleRotateSelection(int position, String item) {
        Log.d(TAG, "Rotate selected: " + item + " at position: " + position);
        
        // Get rotation angle from RotateModel
        if (position >= 0 && position < rotate_spinnerArray.size()) {
            RotateModel selectedModel = rotate_spinnerArray.get(position);
            int rotationAngle = selectedModel.getAngle();
            
            // Apply rotation
            applyRotation(rotationAngle);
        }
    }

    /**
     * Click handlers for UI elements
     */
    private void onStreamClick() {
        Log.d(TAG, "Stream button clicked");
        // Implement streaming logic
        Toast.makeText(getContext(), "Starting stream...", Toast.LENGTH_SHORT).show();
    }

    private void onRotateClick() {
        Log.d(TAG, "Rotate button clicked");
        // Show rotate spinner
        spinner_rotate.performClick();
    }

    private void onCameraTypeClick() {
        Log.d(TAG, "Camera type button clicked");
        // Show camera spinner
        spinner_camera.performClick();
    }

    private void onRecordClick() {
        Log.d(TAG, "Record button clicked");
        // Implement recording logic
        Toast.makeText(getContext(), "Starting recording...", Toast.LENGTH_SHORT).show();
    }

    private void onSnapshotClick() {
        Log.d(TAG, "Snapshot button clicked");
        // Implement snapshot logic
        Toast.makeText(getContext(), "Taking snapshot...", Toast.LENGTH_SHORT).show();
    }

    private void onRefreshClick() {
        Log.d(TAG, "Refresh button clicked");
        // Implement refresh logic
        refreshCameraData();
    }

    /**
     * Camera switching methods
     */
    private void switchToFrontCamera() {
        Log.d(TAG, "Switching to front camera");
        // Implement front camera switching logic
    }

    private void switchToBackCamera() {
        Log.d(TAG, "Switching to back camera");
        // Implement back camera switching logic
    }

    private void switchToUSBCamera() {
        Log.d(TAG, "Switching to USB camera");
        // Implement USB camera switching logic
    }

    /**
     * Apply rotation to camera view
     */
    private void applyRotation(int angle) {
        Log.d(TAG, "Applying rotation: " + angle + " degrees");
        // Implement rotation logic
        if (frame_camera != null) {
            frame_camera.setRotation(angle);
        }
    }

    /**
     * Update camera UI based on selection
     */
    private void updateCameraUI(int cameraPosition) {
        // Update UI elements based on selected camera
        // This could include updating icons, text, or other visual elements
    }

    /**
     * Refresh camera data
     */
    private void refreshCameraData() {
        // Implement camera data refresh logic
        // This could include re-initializing camera, updating settings, etc.
    }

    /**
     * Get current camera selection
     */
    public int getCurrentCameraSelection() {
        return spinner_camera.getSelectedPosition();
    }

    /**
     * Get current rotation selection
     */
    public int getCurrentRotationSelection() {
        return spinner_rotate.getSelectedPosition();
    }

    /**
     * Set camera selection programmatically
     */
    public void setCameraSelection(int position) {
        if (SpinnerUtils.isValidSelection(position, cam_spinnerArray.size())) {
            spinner_camera.setSelection(position);
        }
    }

    /**
     * Set rotation selection programmatically
     */
    public void setRotationSelection(int position) {
        if (SpinnerUtils.isValidSelection(position, rotate_spinnerArray.size())) {
            spinner_rotate.setSelection(position);
        }
    }

    /**
     * Get camera name at specific position
     */
    public String getCameraName(int position) {
        return SpinnerUtils.getItemSafely(cam_spinnerArray, position);
    }

    /**
     * Get rotation name at specific position
     */
    public String getRotationName(int position) {
        if (position >= 0 && position < rotate_spinnerArray.size()) {
            return rotate_spinnerArray.get(position).getName();
        }
        return "";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cleanup
        if (instance != null && instance.get() == this) {
            instance.clear();
        }
    }

    /**
     * Get instance reference (for external access)
     */
    public static LiveFragment_Enhanced getInstance() {
        if (instance != null) {
            return instance.get();
        }
        return null;
    }
}
