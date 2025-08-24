package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.AppConstant;
import com.checkmate.android.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class RotationSelectionBottomSheet extends BottomSheetDialogFragment {

    private RadioGroup rgRotation;
    private RadioButton rbNormal, rbRotate90, rbRotate180, rbRotate270;
    private CheckBox cbFlip, cbMirror;
    private Button btnApply;
    private ImageView btnClose;
    
    private OnRotationSelectionListener listener;
    
    // Current states
    private int currentRotation = AppConstant.is_rotated_0;
    private boolean currentFlip = false;
    private boolean currentMirror = false;

    public interface OnRotationSelectionListener {
        void onRotationApplied(int rotation, boolean flip, boolean mirror);
        void onBottomSheetDismissed();
    }

    public static RotationSelectionBottomSheet newInstance(int rotation, boolean flip, boolean mirror) {
        RotationSelectionBottomSheet fragment = new RotationSelectionBottomSheet();
        Bundle args = new Bundle();
        args.putInt("rotation", rotation);
        args.putBoolean("flip", flip);
        args.putBoolean("mirror", mirror);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnRotationSelectionListener(OnRotationSelectionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentRotation = getArguments().getInt("rotation", AppConstant.is_rotated_0);
            currentFlip = getArguments().getBoolean("flip", false);
            currentMirror = getArguments().getBoolean("mirror", false);
        }
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
        View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_rotation_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupCurrentState();
        setupClickListeners();
        setupRotationLogic();
    }

    private void initViews(View view) {
        rgRotation = view.findViewById(R.id.rg_rotation);
        rbNormal = view.findViewById(R.id.rb_normal);
        rbRotate90 = view.findViewById(R.id.rb_rotate_90);
        rbRotate180 = view.findViewById(R.id.rb_rotate_180);
        rbRotate270 = view.findViewById(R.id.rb_rotate_270);
        
        cbFlip = view.findViewById(R.id.cb_flip);
        cbMirror = view.findViewById(R.id.cb_mirror);
        
        btnApply = view.findViewById(R.id.btn_apply_rotation);
        btnClose = view.findViewById(R.id.btn_close_rotation);
    }

    private void setupCurrentState() {
        // Set rotation selection
        switch (currentRotation) {
            case AppConstant.is_rotated_0:
                rbNormal.setChecked(true);
                break;
            case AppConstant.is_rotated_90:
                rbRotate90.setChecked(true);
                break;
            case AppConstant.is_rotated_180:
                rbRotate180.setChecked(true);
                break;
            case AppConstant.is_rotated_270:
                rbRotate270.setChecked(true);
                break;
        }
        
        // Set transform checkboxes
        cbFlip.setChecked(currentFlip);
        cbMirror.setChecked(currentMirror);
    }

    private void setupRotationLogic() {
        rgRotation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_normal) {
                // When Normal is selected, uncheck flip and mirror
                cbFlip.setChecked(false);
                cbMirror.setChecked(false);
                currentRotation = AppConstant.is_rotated_0;
                currentFlip = false;
                currentMirror = false;
            } else {
                // Update rotation based on selection
                if (checkedId == R.id.rb_rotate_90) {
                    currentRotation = AppConstant.is_rotated_90;
                } else if (checkedId == R.id.rb_rotate_180) {
                    currentRotation = AppConstant.is_rotated_180;
                } else if (checkedId == R.id.rb_rotate_270) {
                    currentRotation = AppConstant.is_rotated_270;
                }
            }
        });
        
        // Handle flip/mirror checkboxes
        cbFlip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentFlip = isChecked;
            if (isChecked) {
                // If flip or mirror is checked, uncheck Normal
                if (rbNormal.isChecked()) {
                    rbRotate90.setChecked(true); // Default to 90 degrees
                    currentRotation = AppConstant.is_rotated_90;
                }
            }
        });
        
        cbMirror.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentMirror = isChecked;
            if (isChecked) {
                // If flip or mirror is checked, uncheck Normal
                if (rbNormal.isChecked()) {
                    rbRotate90.setChecked(true); // Default to 90 degrees
                    currentRotation = AppConstant.is_rotated_90;
                }
            }
        });
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> dismiss());
        
        btnApply.setOnClickListener(v -> {
            // Get current selections
            int selectedRotation = currentRotation;
            boolean selectedFlip = cbFlip.isChecked();
            boolean selectedMirror = cbMirror.isChecked();
            
            // Notify listener
            if (listener != null) {
                listener.onRotationApplied(selectedRotation, selectedFlip, selectedMirror);
            }
            
            dismiss();
        });
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (listener != null) {
            listener.onBottomSheetDismissed();
        }
    }
}