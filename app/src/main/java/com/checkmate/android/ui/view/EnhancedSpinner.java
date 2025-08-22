package com.checkmate.android.ui.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.checkmate.android.R;

import java.util.List;

/**
 * Enhanced Spinner Component with Material Design styling
 * Provides a modern, visually attractive alternative to standard Android Spinner
 */
public class EnhancedSpinner extends FrameLayout {
    
    private CardView cardView;
    private TextView textView;
    private ImageView arrowIcon;
    private OnItemSelectedListener listener;
    private List<String> items;
    private ArrayAdapter<String> adapter;
    private int selectedPosition = -1;
    private boolean isExpanded = false;
    
    // Material Design colors
    private int primaryColor;
    private int accentColor;
    private int textColor;
    private int backgroundColor;
    
    public interface OnItemSelectedListener {
        void onItemSelected(int position, String item);
    }
    
    public EnhancedSpinner(@NonNull Context context) {
        super(context);
        init(context, null);
    }
    
    public EnhancedSpinner(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public EnhancedSpinner(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        // Initialize colors
        primaryColor = ContextCompat.getColor(context, R.color.colorPrimary);
        accentColor = ContextCompat.getColor(context, R.color.colorAccent);
        textColor = ContextCompat.getColor(context, android.R.color.black);
        backgroundColor = ContextCompat.getColor(context, android.R.color.white);
        
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.enhanced_spinner_layout, this, true);
        
        // Initialize views
        cardView = findViewById(R.id.spinner_card);
        textView = findViewById(R.id.spinner_text);
        arrowIcon = findViewById(R.id.spinner_arrow);
        
        // Set initial styling
        updateStyling();
        
        // Set click listener
        setOnClickListener(v -> toggleDropdown());
    }
    
    private void updateStyling() {
        // Card styling
        cardView.setCardBackgroundColor(backgroundColor);
        cardView.setCardElevation(4f);
        cardView.setRadius(8f);
        
        // Text styling
        textView.setTextColor(textColor);
        textView.setTextSize(16);
        
        // Arrow icon styling
        Drawable arrowDrawable = ContextCompat.getDrawable(getContext(), android.R.drawable.arrow_down_float);
        if (arrowDrawable != null) {
            DrawableCompat.setTint(arrowDrawable, primaryColor);
            arrowIcon.setImageDrawable(arrowDrawable);
        }
        
        // Update arrow rotation based on state
        arrowIcon.setRotation(isExpanded ? 180 : 0);
    }
    
    public void setItems(List<String> items) {
        this.items = items;
        if (items != null && !items.isEmpty()) {
            adapter = new ArrayAdapter<>(getContext(), R.layout.enhanced_spinner_item, items);
            setSelection(0);
        }
    }
    
    public void setSelection(int position) {
        if (items != null && position >= 0 && position < items.size()) {
            selectedPosition = position;
            textView.setText(items.get(position));
            if (listener != null) {
                listener.onItemSelected(position, items.get(position));
            }
        }
    }
    
    public int getSelectedPosition() {
        return selectedPosition;
    }
    
    public String getSelectedItem() {
        if (selectedPosition >= 0 && items != null && selectedPosition < items.size()) {
            return items.get(selectedPosition);
        }
        return null;
    }
    
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
    
    private void toggleDropdown() {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        isExpanded = !isExpanded;
        updateStyling();
        
        // Show custom dropdown dialog
        showCustomDropdown();
    }
    
    private void showCustomDropdown() {
        // Create and show a custom dropdown dialog
        EnhancedSpinnerDialog dialog = new EnhancedSpinnerDialog(getContext(), items, selectedPosition);
        dialog.setOnItemSelectedListener((position, item) -> {
            setSelection(position);
            isExpanded = false;
            updateStyling();
        });
        dialog.show();
    }
    
    // Custom dropdown dialog class
    private static class EnhancedSpinnerDialog extends android.app.AlertDialog {
        private List<String> items;
        private int selectedPosition;
        private OnItemSelectedListener listener;
        
        public EnhancedSpinnerDialog(Context context, List<String> items, int selectedPosition) {
            super(context);
            this.items = items;
            this.selectedPosition = selectedPosition;
            setupDialog();
        }
        
        private void setupDialog() {
            setTitle("Select Option");
            
            String[] itemArray = items.toArray(new String[0]);
            setSingleChoiceItems(itemArray, selectedPosition, (dialog, which) -> {
                if (listener != null) {
                    listener.onItemSelected(which, items.get(which));
                }
                dismiss();
            });
            
            setNegativeButton("Cancel", (dialog, which) -> dismiss());
        }
        
        public void setOnItemSelectedListener(OnItemSelectedListener listener) {
            this.listener = listener;
        }
    }
    
    // Getters and setters for customization
    public void setPrimaryColor(int color) {
        this.primaryColor = color;
        updateStyling();
    }
    
    public void setAccentColor(int color) {
        this.accentColor = color;
        updateStyling();
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
        updateStyling();
    }
    
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        updateStyling();
    }
}
