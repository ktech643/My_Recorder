package com.checkmate.android.ui.components;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.checkmate.android.R;

import java.util.List;

public class EnhancedSpinner extends LinearLayout {
    private CardView cardContainer;
    private Spinner spinner;
    private TextView titleText;
    
    private int primaryColor = Color.parseColor("#2196F3");
    private int accentColor = Color.parseColor("#FF5722");
    private int textColor = Color.parseColor("#333333");
    private int backgroundColor = Color.WHITE;
    
    public interface OnItemSelectedListener {
        void onItemSelected(int position, String item);
    }
    
    private OnItemSelectedListener listener;
    
    public EnhancedSpinner(Context context) {
        super(context);
        init(context);
    }
    
    public EnhancedSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public EnhancedSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.enhanced_spinner_layout, this, true);
        
        cardContainer = findViewById(R.id.card_container);
        spinner = findViewById(R.id.spinner);
        titleText = findViewById(R.id.title_text);
        
        setupSpinner();
        applyDefaultStyling();
    }
    
    private void setupSpinner() {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (listener != null && position >= 0) {
                    String item = parent.getItemAtPosition(position).toString();
                    listener.onItemSelected(position, item);
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void applyDefaultStyling() {
        cardContainer.setCardBackgroundColor(backgroundColor);
        cardContainer.setCardElevation(8f);
        cardContainer.setRadius(12f);
        titleText.setTextColor(textColor);
    }
    
    // Public methods for customization
    public void setTitle(String title) {
        titleText.setText(title);
        titleText.setVisibility(title.isEmpty() ? GONE : VISIBLE);
    }
    
    public void setItems(List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), 
            R.layout.enhanced_spinner_item, items) {
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(position % 2 == 0 ? Color.WHITE : Color.parseColor("#F5F5F5"));
                return view;
            }
        };
        adapter.setDropDownViewResource(R.layout.enhanced_spinner_item);
        spinner.setAdapter(adapter);
    }
    
    public void setSelection(int position) {
        spinner.setSelection(position);
    }
    
    public int getSelectedItemPosition() {
        return spinner.getSelectedItemPosition();
    }
    
    public String getSelectedItem() {
        Object item = spinner.getSelectedItem();
        return item != null ? item.toString() : "";
    }
    
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
    
    // Color customization methods
    public void setPrimaryColor(int color) {
        this.primaryColor = color;
        // Apply color to relevant elements
    }
    
    public void setAccentColor(int color) {
        this.accentColor = color;
        // Apply color to relevant elements
    }
    
    public void setTextColor(int color) {
        this.textColor = color;
        titleText.setTextColor(color);
    }
    
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        cardContainer.setCardBackgroundColor(color);
    }
}
