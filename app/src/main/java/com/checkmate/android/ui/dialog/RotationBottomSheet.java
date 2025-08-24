package com.checkmate.android.ui.dialog;

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
import com.checkmate.android.R;
import com.checkmate.android.model.RotateModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class RotationBottomSheet extends BottomSheetDialogFragment {
    
    private static final String TAG = "RotationBottomSheet";
    
    private WeakReference<RotationSelectionListener> mListenerRef;
    private List<RotateModel> mRotationModels;
    private RotationAdapter mAdapter;
    
    // Current rotation state
    private int mCurrentRotation = AppConstant.is_rotated_0;
    private boolean mIsFlipped = false;
    private boolean mIsMirrored = false;
    private boolean mIsDismissed = false;
    
    public interface RotationSelectionListener {
        void onRotationSelected(int rotation, boolean isFlipped, boolean isMirrored);
        void onRotationSelectionDismissed();
    }
    
    public static RotationBottomSheet newInstance(int currentRotation, boolean isFlipped, boolean isMirrored) {
        RotationBottomSheet fragment = new RotationBottomSheet();
        Bundle args = new Bundle();
        args.putInt("rotation", currentRotation);
        args.putBoolean("flipped", isFlipped);
        args.putBoolean("mirrored", isMirrored);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setRotationSelectionListener(RotationSelectionListener listener) {
        mListenerRef = new WeakReference<>(listener);
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog);
        setRetainInstance(false);
        
        if (getArguments() != null) {
            mCurrentRotation = getArguments().getInt("rotation", AppConstant.is_rotated_0);
            mIsFlipped = getArguments().getBoolean("flipped", false);
            mIsMirrored = getArguments().getBoolean("mirrored", false);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_rotation, container, false);
        
        RecyclerView recyclerView = view.findViewById(R.id.recycler_rotation);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize rotation models
        initRotationModels();
        
        // Set adapter
        mAdapter = new RotationAdapter(mRotationModels);
        recyclerView.setAdapter(mAdapter);
        
        return view;
    }
    
    private void initRotationModels() {
        mRotationModels = new ArrayList<>();
        
        // Rotation options (single select - only one can be chosen)
        mRotationModels.add(new RotateModel(R.mipmap.ic_rotate_90, getString(R.string.rotate_90), 
            mCurrentRotation == AppConstant.is_rotated_90));
        mRotationModels.add(new RotateModel(R.mipmap.ic_rotate_90, getString(R.string.rotate_180), 
            mCurrentRotation == AppConstant.is_rotated_180));
        mRotationModels.add(new RotateModel(R.mipmap.ic_rotate_90, getString(R.string.rotate_270), 
            mCurrentRotation == AppConstant.is_rotated_270));
        
        // Flip and Mirror options (multi-select - can be combined)
        mRotationModels.add(new RotateModel(R.mipmap.ic_flip, getString(R.string.flip), mIsFlipped));
        mRotationModels.add(new RotateModel(R.mipmap.ic_mirror, getString(R.string.mirror), mIsMirrored));
        
        // Normal option (deselects all others when selected)
        mRotationModels.add(new RotateModel(R.mipmap.ic_camera, getString(R.string.normal), 
            mCurrentRotation == AppConstant.is_rotated_0 && !mIsFlipped && !mIsMirrored));
    }
    
    private class RotationAdapter extends RecyclerView.Adapter<RotationAdapter.RotationViewHolder> {
        
        private List<RotateModel> mModels;
        
        public RotationAdapter(List<RotateModel> models) {
            mModels = models;
        }
        
        @NonNull
        @Override
        public RotationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rotation_selection, parent, false);
            return new RotationViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull RotationViewHolder holder, int position) {
            RotateModel model = mModels.get(position);
            holder.bind(model, position);
        }
        
        @Override
        public int getItemCount() {
            return mModels.size();
        }
        
        class RotationViewHolder extends RecyclerView.ViewHolder {
            
            private ImageView ivIcon;
            private TextView tvTitle;
            private ImageView ivSelected;
            
            public RotationViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_rotation_icon);
                tvTitle = itemView.findViewById(R.id.tv_rotation_title);
                ivSelected = itemView.findViewById(R.id.iv_rotation_selected);
                
                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && !mIsDismissed) {
                        onRotationSelected(position);
                    }
                });
            }
            
            public void bind(RotateModel model, int position) {
                if (ivIcon != null) {
                    ivIcon.setImageResource(model.resourceId);
                    // Set different icons for different rotation types
                    if (position < 3) { // Rotation options
                        ivIcon.setImageResource(R.mipmap.ic_rotate_90);
                    } else if (position == 3) { // Flip
                        ivIcon.setImageResource(R.mipmap.ic_flip);
                    } else if (position == 4) { // Mirror
                        ivIcon.setImageResource(R.mipmap.ic_mirror);
                    } else { // Normal
                        ivIcon.setImageResource(R.mipmap.ic_camera);
                    }
                }
                
                if (tvTitle != null) {
                    tvTitle.setText(model.title);
                }
                
                // Highlight selected items
                if (ivSelected != null) {
                    ivSelected.setVisibility(model.is_selected ? View.VISIBLE : View.GONE);
                }
                
                // Highlight the text color for selected items
                if (tvTitle != null) {
                    tvTitle.setTextColor(model.is_selected ? 
                        getResources().getColor(R.color.teal) : 
                        getResources().getColor(R.color.text_normal));
                    
                    // Set text style using Typeface
                    if (model.is_selected) {
                        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    } else {
                        tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                
                // Set item background state
                itemView.setSelected(model.is_selected);
            }
            
            private void onRotationSelected(int position) {
                if (mIsDismissed) return;
                
                if (position == 5) { // Normal option
                    // Reset all to normal - this deselects all other options
                    resetToNormal();
                } else if (position < 3) { // Rotation options (0-2: 90, 180, 270)
                    // Single select for rotation - only one rotation can be chosen
                    selectRotationOnly(position);
                } else { // Flip/Mirror options (3-4)
                    // Toggle flip/mirror - these can be combined
                    toggleFlipMirror(position);
                }
                
                // Update UI to reflect changes
                notifyDataSetChanged();
                
                // Notify listener immediately
                RotationSelectionListener listener = mListenerRef != null ? mListenerRef.get() : null;
                if (listener != null) {
                    listener.onRotationSelected(mCurrentRotation, mIsFlipped, mIsMirrored);
                }
                
                // Auto-dismiss bottom sheet after selection
                safeDismiss();
            }
            
            private void resetToNormal() {
                // Reset all rotation, flip, and mirror states
                mCurrentRotation = AppConstant.is_rotated_0;
                mIsFlipped = false;
                mIsMirrored = false;
                
                // Update all models - only normal is selected, everything else is deselected
                for (int i = 0; i < mModels.size(); i++) {
                    mModels.get(i).is_selected = (i == 5); // Only normal (index 5) is selected
                }
            }
            
            private void selectRotationOnly(int position) {
                // Reset all rotation options first
                for (int i = 0; i < 3; i++) {
                    mModels.get(i).is_selected = (i == position);
                }
                
                // Keep flip and mirror when rotation is selected (they can be combined)
                // Don't reset flip/mirror - they can work together with rotation
                mModels.get(5).is_selected = false; // Normal is deselected when rotation is chosen
                
                // Update rotation state based on selection
                switch (position) {
                    case 0:
                        mCurrentRotation = AppConstant.is_rotated_90;
                        break;
                    case 1:
                        mCurrentRotation = AppConstant.is_rotated_180;
                        break;
                    case 2:
                        mCurrentRotation = AppConstant.is_rotated_270;
                        break;
                }
                
                // Keep flip and mirror when rotation is selected
                // (rotation and flip/mirror can be combined)
            }
            
            private void toggleFlipMirror(int position) {
                if (position == 3) { // Flip
                    mIsFlipped = !mIsFlipped;
                    mModels.get(3).is_selected = mIsFlipped;
                } else if (position == 4) { // Mirror
                    mIsMirrored = !mIsMirrored;
                    mModels.get(4).is_selected = mIsMirrored;
                }
                
                // If any flip/mirror is selected, deselect normal
                if (mIsFlipped || mIsMirrored) {
                    mModels.get(5).is_selected = false;
                }
                
                // Keep current rotation when flip/mirror is toggled
                // (rotation and flip/mirror can be combined)
                // Don't change rotation state - it can work with flip/mirror
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
        
        RotationSelectionListener listener = mListenerRef != null ? mListenerRef.get() : null;
        if (listener != null) {
            listener.onRotationSelectionDismissed();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        if (mAdapter != null) {
            mAdapter = null;
        }
        if (mRotationModels != null) {
            mRotationModels.clear();
            mRotationModels = null;
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
