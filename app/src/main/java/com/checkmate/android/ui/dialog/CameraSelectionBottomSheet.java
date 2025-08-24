package com.checkmate.android.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.model.Camera;
import com.checkmate.android.model.RotateModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import android.text.TextUtils;

public class CameraSelectionBottomSheet extends BottomSheetDialogFragment {
    
    private static final String TAG = "CameraSelectionBottomSheet";
    
    private WeakReference<CameraSelectionListener> mListenerRef;
    private List<RotateModel> mCameraModels;
    private CameraAdapter mAdapter;
    private String mCurrentSelection;
    private boolean mIsDismissed = false;
    
    public interface CameraSelectionListener {
        void onCameraSelected(String cameraName, int position);
        void onCameraSelectionDismissed();
    }
    
    public static CameraSelectionBottomSheet newInstance() {
        return new CameraSelectionBottomSheet();
    }
    
    public void setCameraSelectionListener(CameraSelectionListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog);
        setRetainInstance(false); // Prevent memory leaks
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_camera_selection, container, false);
        
        RecyclerView recyclerView = view.findViewById(R.id.recycler_cameras);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize camera models
        initCameraModels();
        
        // Set adapter
        mAdapter = new CameraAdapter(mCameraModels, mCurrentSelection);
        recyclerView.setAdapter(mAdapter);
        
        return view;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Ensure proper cleanup if fragment is destroyed
        if (getActivity() == null || !isAdded()) {
            dismissAllowingStateLoss();
        }
    }
    
    private void initCameraModels() {
        mCameraModels = new ArrayList<>();
        
        // Get current selection
        mCurrentSelection = AppPreference.getStr(AppPreference.KEY.SELECTED_POSITION, AppConstant.REAR_CAMERA);
        
        // Add built-in cameras
        if (AppPreference.getBool(AppPreference.KEY.CAM_REAR_FACING, true)) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.rear_camera), 
                TextUtils.equals(mCurrentSelection, AppConstant.REAR_CAMERA)));
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_FRONT_FACING, true)) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.front_camera), 
                TextUtils.equals(mCurrentSelection, AppConstant.FRONT_CAMERA)));
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_USB, false)) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.usb_camera), 
                TextUtils.equals(mCurrentSelection, AppConstant.USB_CAMERA)));
        }
        
        if (AppPreference.getBool(AppPreference.KEY.CAM_CAST, false)) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.screen_cast), 
                TextUtils.equals(mCurrentSelection, AppConstant.SCREEN_CAST)));
        }
        
        if (AppPreference.getBool(AppPreference.KEY.AUDIO_ONLY, false)) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.audio_only_text), 
                TextUtils.equals(mCurrentSelection, AppConstant.AUDIO_ONLY)));
        }
        
        // Add WiFi cameras from database
        List<Camera> dbCameras = DBManager.getInstance().getCameras();
        for (Camera camera : dbCameras) {
            mCameraModels.add(new RotateModel(R.mipmap.ic_camera, camera.camera_name, 
                TextUtils.equals(mCurrentSelection, camera.camera_name)));
        }
    }
    
    private class CameraAdapter extends RecyclerView.Adapter<CameraAdapter.CameraViewHolder> {
        
        private List<RotateModel> mModels;
        private String mSelectedCamera;
        
        public CameraAdapter(List<RotateModel> models, String selectedCamera) {
            mModels = models;
            mSelectedCamera = selectedCamera;
        }
        
        @NonNull
        @Override
        public CameraViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_camera_selection, parent, false);
            return new CameraViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull CameraViewHolder holder, int position) {
            RotateModel model = mModels.get(position);
            holder.bind(model, position);
        }
        
        @Override
        public int getItemCount() {
            return mModels.size();
        }
        
        class CameraViewHolder extends RecyclerView.ViewHolder {
            
            private ImageView ivIcon;
            private TextView tvTitle;
            private ImageView ivSelected;
            
            public CameraViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_camera_icon);
                tvTitle = itemView.findViewById(R.id.tv_camera_title);
                ivSelected = itemView.findViewById(R.id.iv_selected);
                
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && !mIsDismissed) {
                        onCameraSelected(position);
                    }
                });
            }
            
            public void bind(RotateModel model, int position) {
                if (ivIcon != null) ivIcon.setImageResource(model.resourceId);
                if (tvTitle != null) tvTitle.setText(model.title);
                if (ivSelected != null) ivSelected.setVisibility(model.is_selected ? View.VISIBLE : View.GONE);
            }
            
            private void onCameraSelected(int position) {
                if (mIsDismissed) return;
                
                // Update selection in models
                for (int i = 0; i < mModels.size(); i++) {
                    mModels.get(i).is_selected = (i == position);
                }
                
                // Update selected camera
                mSelectedCamera = mModels.get(position).title;
                
                // Notify adapter
                notifyDataSetChanged();
                
                // Notify listener
                CameraSelectionListener listener = mListenerRef != null ? mListenerRef.get() : null;
                if (listener != null) {
                    listener.onCameraSelected(mSelectedCamera, position);
                }
                
                // Dismiss bottom sheet safely
                safeDismiss();
            }
        }
    }
    
    private void safeDismiss() {
        if (mIsDismissed) return;
        mIsDismissed = true;
        
        try {
            if (isAdded() && !isDetached()) {
                dismissAllowingStateLoss();
            }
        } catch (Exception e) {
            // Handle any dismissal errors
            if (getActivity() != null && !getActivity().isFinishing()) {
                try {
                    super.dismissAllowingStateLoss();
                } catch (Exception ex) {
                    // Final fallback
                }
            }
        }
    }
    
    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        if (mIsDismissed) return;
        mIsDismissed = true;
        
        super.onDismiss(dialog);
        
        CameraSelectionListener listener = mListenerRef != null ? mListenerRef.get() : null;
        if (listener != null) {
            listener.onCameraSelectionDismissed();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        if (mAdapter != null) {
            mAdapter = null;
        }
        if (mCameraModels != null) {
            mCameraModels.clear();
            mCameraModels = null;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clear weak reference
        if (mListenerRef != null) {
            mListenerRef.clear();
            mListenerRef = null;
        }
    }
    
    public void show(FragmentManager fragmentManager) {
        if (fragmentManager != null && !fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
            show(fragmentManager, TAG);
        }
    }
}
