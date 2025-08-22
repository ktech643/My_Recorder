package com.checkmate.android.ui.popup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RotateSpinnerPopup extends BaseSpinnerPopup {
    private boolean isNormalSelected = true; // Default to normal selected

    public RotateSpinnerPopup(Context context) {
        super(context);
        initOptions();
        // Set normal as default selected
        selectedItems.add("normal");
        
        // Check current rotation state from preferences or activity
        updateCurrentSelection();
    }
    
    /**
     * Update the current selection based on the actual camera state
     */
    private void updateCurrentSelection() {
        // This will be called by the fragment to set the current state
        // For now, default to normal
        selectedItems.clear();
        selectedItems.add("normal");
    }
    
    /**
     * Set the current rotation state from the fragment
     */
    public void setCurrentRotationState(String currentRotation, boolean isFlipped, boolean isMirrored) {
        selectedItems.clear();
        
        // Debug logging for input values
        android.util.Log.d("RotateSpinnerPopup", "Input - Rotation: " + currentRotation + ", Flipped: " + isFlipped + ", Mirrored: " + isMirrored);
        
        boolean hasTransformations = false;
        
        if (isFlipped) {
            selectedItems.add("flip");
            hasTransformations = true;
        }
        if (isMirrored) {
            selectedItems.add("mirror");
            hasTransformations = true;
        }
        
        // Handle rotation values - AppConstant rotation of 0 means "normal", other values are actual rotations
        if (currentRotation != null) {
            try {
                int rotationConstant = Integer.parseInt(currentRotation);
                if (rotationConstant != 0) { // Only non-zero rotations are transformations
                    String popupRotationValue = convertRotationConstantToPopupValue(currentRotation);
                    if (popupRotationValue != null) {
                        selectedItems.add(popupRotationValue);
                        hasTransformations = true;
                    }
                }
            } catch (NumberFormatException e) {
                android.util.Log.w("RotateSpinnerPopup", "Invalid rotation value: " + currentRotation);
            }
        }
        
        // If no transformations are applied, select "normal"
        if (!hasTransformations) {
            selectedItems.add("normal");
            android.util.Log.d("RotateSpinnerPopup", "No transformations detected, adding 'normal' to selection");
        }
        
        // Debug logging
        android.util.Log.d("RotateSpinnerPopup", "Final selected items: " + selectedItems.toString());
        
        // Update the adapter
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            android.util.Log.d("RotateSpinnerPopup", "Adapter updated successfully");
        } else {
            android.util.Log.w("RotateSpinnerPopup", "Adapter is null, will update when onCreate is called");
        }
    }
    
    /**
     * Convert AppConstant rotation value to popup rotation value
     */
    private String convertRotationConstantToPopupValue(String rotationConstant) {
        try {
            int rotation = Integer.parseInt(rotationConstant);
            switch (rotation) {
                case 0: return "0";
                case 1: return "90";
                case 2: return "180";
                case 3: return "270";
                default: return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void initOptions() {
        options.clear();
        options.add(new BaseSpinnerPopup.SpinnerOption("0", "0°", R.drawable.ic_rotate_0));
        options.add(new BaseSpinnerPopup.SpinnerOption("90", "90°", R.drawable.ic_rotate_90));
        options.add(new BaseSpinnerPopup.SpinnerOption("180", "180°", R.drawable.ic_rotate_180));
        options.add(new BaseSpinnerPopup.SpinnerOption("270", "270°", R.drawable.ic_rotate_270));
        options.add(new BaseSpinnerPopup.SpinnerOption("flip", "Flip", R.drawable.ic_flip));
        options.add(new BaseSpinnerPopup.SpinnerOption("mirror", "Mirror", R.drawable.ic_mirror));
        options.add(new BaseSpinnerPopup.SpinnerOption("normal", "Normal", R.drawable.ic_normal));
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        
        // Override the adapter to use RotateAdapter
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            RotateAdapter adapter = new RotateAdapter();
            recyclerView.setAdapter(adapter);
            
            // Store reference to adapter for later updates
            this.adapter = adapter;
            
            // Ensure the adapter reflects current selection state
            adapter.notifyDataSetChanged();
            android.util.Log.d("RotateSpinnerPopup", "Adapter created and updated with current state");
            
            // Notify that popup is ready for state updates
            if (onPopupReadyListener != null) {
                onPopupReadyListener.onPopupReady();
            }
        }
    }
    
    // Interface to notify when popup is ready
    public interface OnPopupReadyListener {
        void onPopupReady();
    }
    
    private OnPopupReadyListener onPopupReadyListener;
    
    public void setOnPopupReadyListener(OnPopupReadyListener listener) {
        this.onPopupReadyListener = listener;
    }
    
    private RotateAdapter adapter;

    public interface OnItemSelectedListener {
        void onItemSelected(String id);
        void onItemsSelected(Set<String> ids);
    }

    private class RotateAdapter extends RecyclerView.Adapter<RotateAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spinner, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BaseSpinnerPopup.SpinnerOption option = options.get(position);
            holder.title.setText(option.title);
            holder.icon.setImageResource(option.iconResId);

            boolean isSelected = selectedItems.contains(option.id);
            holder.check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.title.setTextColor(getContext().getResources().getColor(isSelected ? R.color.blue : android.R.color.black));

            // Debug logging for selection state
            android.util.Log.d("RotateSpinnerPopup", "Position " + position + ": " + option.id + " selected: " + isSelected);

            holder.itemView.setOnClickListener(v -> {
                handleItemClick(option.id);
                notifyDataSetChanged();
            });
        }

        private void handleItemClick(String itemId) {
            android.util.Log.d("RotateSpinnerPopup", "Item clicked: " + itemId);
            
            if (itemId.equals("normal")) {
                // Normal resets everything
                selectedItems.clear();
                selectedItems.add("normal");
                isNormalSelected = true;
                android.util.Log.d("RotateSpinnerPopup", "Normal selected, selectedItems: " + selectedItems.toString());
            } else if (itemId.equals("flip") || itemId.equals("mirror")) {
                // Flip/Mirror can be combined with each other but not with rotation
                if (selectedItems.contains("normal")) {
                    selectedItems.remove("normal");
                    isNormalSelected = false;
                }
                // Toggle flip/mirror (multi-selectable)
                if (selectedItems.contains(itemId)) {
                    selectedItems.remove(itemId);
                } else {
                    selectedItems.add(itemId);
                }
                // Remove any rotation selections when flip/mirror is applied
                selectedItems.remove("0");
                selectedItems.remove("90");
                selectedItems.remove("180");
                selectedItems.remove("270");
                android.util.Log.d("RotateSpinnerPopup", "Flip/Mirror handled, selectedItems: " + selectedItems.toString());
            } else {
                // Rotation values (0, 90, 180, 270) - single selection only
                if (selectedItems.contains("normal")) {
                    selectedItems.remove("normal");
                    isNormalSelected = false;
                }
                // Remove other rotation selections (single selection)
                selectedItems.remove("0");
                selectedItems.remove("90");
                selectedItems.remove("180");
                selectedItems.remove("270");
                // Add current rotation
                selectedItems.add(itemId);
                
                // Remove flip/mirror when rotation is applied
                selectedItems.remove("flip");
                selectedItems.remove("mirror");
                android.util.Log.d("RotateSpinnerPopup", "Rotation handled, selectedItems: " + selectedItems.toString());
            }

            // Call listener with the selected item
            if (listener != null) {
                // For rotation, we only care about the main selection
                if (itemId.equals("normal")) {
                    listener.onItemSelected("normal");
                } else if (itemId.equals("flip") || itemId.equals("mirror")) {
                    // For flip/mirror, send the current state
                    listener.onItemSelected(itemId);
                } else {
                    // For rotation values, send the angle
                    listener.onItemSelected(itemId);
                }
            }
            
            // Auto-dismiss popup after selection
            dismiss();
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            ImageView check;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.icon);
                title = itemView.findViewById(R.id.title);
                check = itemView.findViewById(R.id.check);
            }
        }
    }
}
