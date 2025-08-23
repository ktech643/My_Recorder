package com.checkmate.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.R;
import com.checkmate.android.ui.components.EnhancedSpinner;
import com.checkmate.android.util.SpinnerUtils;

public class LiveFragment_Enhanced extends BaseFragment {
    
    private EnhancedSpinner qualitySpinner;
    private EnhancedSpinner resolutionSpinner;
    private EnhancedSpinner bitrateSpinner;
    private EnhancedSpinner framerateSpinner;
    
    @Override
    protected String getFragmentTag() {
        return "LiveFragment";
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_enhanced, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponents();
        setupUI();
    }
    
    @Override
    protected void setupUI() {
        // Setup all spinners with utilities
        SpinnerUtils.setupQualitySpinner(qualitySpinner, getContext());
        SpinnerUtils.setupResolutionSpinner(resolutionSpinner, getContext());
        SpinnerUtils.setupBitrateSpinner(bitrateSpinner, getContext());
        SpinnerUtils.setupFramerateSpinner(framerateSpinner, getContext());
        
        // Apply consistent theming
        SpinnerUtils.applyMaterialDesignTheme(qualitySpinner);
        SpinnerUtils.applyMaterialDesignTheme(resolutionSpinner);
        SpinnerUtils.applyMaterialDesignTheme(bitrateSpinner);
        SpinnerUtils.applyMaterialDesignTheme(framerateSpinner);
        
        // Setup listeners
        setupSpinnerListeners();
    }
    
    @Override
    protected void initializeComponents() {
        qualitySpinner = getView().findViewById(R.id.spinner_quality);
        resolutionSpinner = getView().findViewById(R.id.spinner_resolution);
        bitrateSpinner = getView().findViewById(R.id.spinner_bitrate);
        framerateSpinner = getView().findViewById(R.id.spinner_framerate);
    }
    
    private void setupSpinnerListeners() {
        qualitySpinner.setOnItemSelectedListener((position, item) -> {
            Toast.makeText(getContext(), "Quality: " + item, Toast.LENGTH_SHORT).show();
            // Handle quality selection logic
        });
        
        resolutionSpinner.setOnItemSelectedListener((position, item) -> {
            Toast.makeText(getContext(), "Resolution: " + item, Toast.LENGTH_SHORT).show();
            // Handle resolution selection logic
        });
        
        bitrateSpinner.setOnItemSelectedListener((position, item) -> {
            Toast.makeText(getContext(), "Bitrate: " + item, Toast.LENGTH_SHORT).show();
            // Handle bitrate selection logic
        });
        
        framerateSpinner.setOnItemSelectedListener((position, item) -> {
            Toast.makeText(getContext(), "Framerate: " + item, Toast.LENGTH_SHORT).show();
            // Handle framerate selection logic
        });
    }
}
