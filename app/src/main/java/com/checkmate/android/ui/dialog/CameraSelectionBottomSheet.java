package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.adapter.CameraSelectionAdapter;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.model.Camera;
import com.checkmate.android.model.CameraSelectionModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class CameraSelectionBottomSheet extends BottomSheetDialogFragment 
        implements CameraSelectionAdapter.OnCameraSelectedListener {

    private RecyclerView rvCameras;
    private ImageView btnClose;
    private CameraSelectionAdapter adapter;
    private List<CameraSelectionModel> cameraList;
    private OnCameraSelectionListener listener;

    public interface OnCameraSelectionListener {
        void onCameraSelected(CameraSelectionModel camera, int position);
        void onBottomSheetDismissed();
    }

    public static CameraSelectionBottomSheet newInstance() {
        return new CameraSelectionBottomSheet();
    }

    public void setOnCameraSelectionListener(OnCameraSelectionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupFullHeight(bottomSheetDialog);
        });
        return dialog;
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        // This ensures the bottom sheet opens fully expanded
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_camera_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupCameraList();
        setupClickListeners();
    }

    private void initViews(View view) {
        rvCameras = view.findViewById(R.id.rv_cameras);
        btnClose = view.findViewById(R.id.btn_close);
        
        rvCameras.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupCameraList() {
        cameraList = new ArrayList<>();
        
        // Add built-in cameras
        if (AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
            CameraSelectionModel rearCamera = new CameraSelectionModel(
                getString(R.string.rear_camera), 
                AppConstant.REAR_CAMERA, 
                R.mipmap.ic_camera
            );
            cameraList.add(rearCamera);
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
            CameraSelectionModel frontCamera = new CameraSelectionModel(
                getString(R.string.front_camera), 
                AppConstant.FRONT_CAMERA, 
                R.mipmap.ic_camera
            );
            cameraList.add(frontCamera);
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_USB, false)) {
            CameraSelectionModel usbCamera = new CameraSelectionModel(
                getString(R.string.usb_camera), 
                AppConstant.USB_CAMERA, 
                R.mipmap.ic_camera
            );
            cameraList.add(usbCamera);
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_CAST, false)) {
            CameraSelectionModel screenCast = new CameraSelectionModel(
                getString(R.string.screen_cast), 
                AppConstant.SCREEN_CAST, 
                R.mipmap.ic_camera
            );
            cameraList.add(screenCast);
        }
        
        if (AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, false)) {
            CameraSelectionModel audioOnly = new CameraSelectionModel(
                getString(R.string.audio_only_text), 
                AppConstant.AUDIO_ONLY, 
                R.mipmap.ic_camera
            );
            cameraList.add(audioOnly);
        }
        
        // Add WiFi cameras from database
        List<Camera> wifiCameras = DBManager.getInstance().getCameras();
        for (Camera camera : wifiCameras) {
            CameraSelectionModel wifiCamera = new CameraSelectionModel(
                camera.camera_name, 
                "WIFI_CAMERA", 
                R.mipmap.ic_camera,
                camera
            );
            cameraList.add(wifiCamera);
        }
        
        // Set up adapter
        adapter = new CameraSelectionAdapter(cameraList, this);
        rvCameras.setAdapter(adapter);
        
        // Set current selection
        setCurrentSelection();
    }

    private void setCurrentSelection() {
        String currentCamera = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        
        for (int i = 0; i < cameraList.size(); i++) {
            CameraSelectionModel camera = cameraList.get(i);
            
            // Check if this matches the current selection
            boolean isSelected = false;
            if (camera.getWifiCamera() != null) {
                // WiFi camera - match by name
                isSelected = currentCamera.equals(camera.getWifiCamera().camera_name);
            } else {
                // Built-in camera - match by type
                if (AppConstant.REAR_CAMERA.equals(currentCamera) && 
                    camera.getType().equals(AppConstant.REAR_CAMERA)) {
                    isSelected = true;
                } else if (AppConstant.FRONT_CAMERA.equals(currentCamera) && 
                          camera.getType().equals(AppConstant.FRONT_CAMERA)) {
                    isSelected = true;
                } else if (AppConstant.USB_CAMERA.equals(currentCamera) && 
                          camera.getType().equals(AppConstant.USB_CAMERA)) {
                    isSelected = true;
                } else if (AppConstant.SCREEN_CAST.equals(currentCamera) && 
                          camera.getType().equals(AppConstant.SCREEN_CAST)) {
                    isSelected = true;
                } else if (AppConstant.AUDIO_ONLY.equals(currentCamera) && 
                          camera.getType().equals(AppConstant.AUDIO_ONLY)) {
                    isSelected = true;
                }
            }
            
            if (isSelected) {
                adapter.setSelectedPosition(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onCameraSelected(CameraSelectionModel camera, int position) {
        // Save selection to preferences and database if needed
        saveSelection(camera);
        
        // Notify listener
        if (listener != null) {
            listener.onCameraSelected(camera, position);
        }
        
        // Dismiss after a short delay to show selection
        btnClose.postDelayed(this::dismiss, 200);
    }

    private void saveSelection(CameraSelectionModel camera) {
        if (camera.getWifiCamera() != null) {
            // WiFi camera
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, camera.getWifiCamera().camera_name);
        } else {
            // Built-in camera
            AppPreference.setStr(AppPreference.KEY.SELECTED_POSITION, camera.getType());
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.onBottomSheetDismissed();
        }
    }
}