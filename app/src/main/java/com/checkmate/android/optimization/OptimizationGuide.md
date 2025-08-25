# Live Fragment Optimization Guide

This document describes the optimizations implemented for seamless streaming and recording in the CheckMate application.

## Overview

The optimization focuses on:
1. Early EGL initialization
2. Seamless service transitions
3. Dynamic configuration updates
4. Time overlay for transitions
5. Zero-downtime switching

## Key Components

### 1. EglInitializer
- **Location**: `com.checkmate.android.util.EglInitializer`
- **Purpose**: Initialize EGL context early in MainActivity to reduce service startup time
- **Usage**: Automatically called in MainActivity.onCreate()

### 2. OptimizedServiceSwitcher
- **Location**: `com.checkmate.android.service.OptimizedServiceSwitcher`
- **Purpose**: Provides seamless transitions between services without blank screens
- **Features**:
  - Preloads services before switching
  - Uses transition overlay during switches
  - Maintains active streams/recordings
  - Atomic service switching

### 3. TransitionOverlay
- **Location**: `com.checkmate.android.util.TransitionOverlay`
- **Purpose**: Displays time-stamped frames during transitions
- **Features**:
  - Shows current time
  - Displays transition status
  - Maintains stream continuity

### 4. DynamicConfigManager
- **Location**: `com.checkmate.android.util.DynamicConfigManager`
- **Purpose**: Updates streaming/recording configurations without interruption
- **Features**:
  - Dynamic resolution changes
  - Bitrate adjustments
  - Frame rate updates
  - Audio configuration changes

## Implementation Details

### Early EGL Initialization

```java
// In MainActivity.onCreate()
private void initializeEglEarly() {
    EglInitializer.getInstance().initializeAsync(this);
}
```

### Service Switching

```java
// In LiveFragment
OptimizedServiceSwitcher.switchServiceOptimized(activity, targetServiceType, 
    new OptimizedServiceSwitcher.ServiceSwitchCallback() {
        @Override
        public void onServiceSwitched(boolean success, String message) {
            // Handle switch result
        }
    });
```

### Dynamic Configuration Updates

```java
// Update resolution while streaming
DynamicConfigManager.getInstance().updateVideoResolution(1920, 1080, 
    new DynamicConfigManager.ConfigUpdateCallback() {
        @Override
        public void onUpdateComplete(boolean success, String message) {
            // Handle update result
        }
    });

// Batch updates
DynamicConfigManager.ConfigUpdate updates = new DynamicConfigManager.ConfigUpdate()
    .setVideoResolution(1920, 1080)
    .setVideoBitrate(5000000)
    .setFrameRate(30);

DynamicConfigManager.getInstance().batchUpdate(updates, callback);
```

### Transition Overlay

```java
// Show overlay during transition
TransitionOverlay.show(context);
TransitionOverlay.setStatus("Switching to front camera...");

// Hide after transition
TransitionOverlay.hide();
```

## Testing Checklist

### Basic Functionality
- [ ] EGL initializes on app start
- [ ] No crashes during initialization
- [ ] Services start without delay

### Service Transitions
- [ ] Switch between rear and front camera
- [ ] Switch to USB camera
- [ ] Switch to screen cast
- [ ] Switch to audio-only mode
- [ ] No blank screens during transitions
- [ ] Time overlay appears during transitions

### Streaming/Recording
- [ ] Stream continues during service switch
- [ ] Recording continues during service switch
- [ ] No frame drops during transitions
- [ ] Audio remains synchronized

### Dynamic Configuration
- [ ] Change resolution while streaming
- [ ] Change bitrate while streaming
- [ ] Change frame rate while recording
- [ ] Audio settings update without interruption

### Edge Cases
- [ ] Multiple rapid service switches
- [ ] Switch while both streaming and recording
- [ ] Switch with poor network conditions
- [ ] Memory pressure scenarios
- [ ] Background/foreground transitions

## Performance Metrics

### Target Goals
- Service switch time: < 500ms
- Frame drop during switch: 0
- Memory overhead: < 50MB
- CPU usage spike: < 20%

### Monitoring
1. Use Android Studio Profiler to monitor:
   - CPU usage during transitions
   - Memory allocation patterns
   - GPU rendering performance

2. Log analysis:
   - Look for "EGL initialization complete"
   - Check "Service switch" timing logs
   - Monitor "Configuration updated" messages

## Troubleshooting

### Common Issues

1. **Blank screen during transition**
   - Check if TransitionOverlay is properly initialized
   - Verify EGL context is valid
   - Ensure surface dimensions are correct

2. **Stream interruption**
   - Verify SharedEglManager maintains encoder state
   - Check network connectivity
   - Monitor encoder queue status

3. **Configuration not updating**
   - Ensure encoder supports dynamic updates
   - Check if update is called on correct thread
   - Verify new parameters are valid

### Debug Flags

Add these to AppPreference for debugging:
```java
// Enable detailed transition logging
AppPreference.setBool("debug_transitions", true);

// Force transition overlay
AppPreference.setBool("force_transition_overlay", true);

// Log EGL operations
AppPreference.setBool("debug_egl", true);
```

## Future Enhancements

1. **Predictive Preloading**
   - Analyze user patterns
   - Preload likely next service
   - Reduce switch time further

2. **Advanced Overlays**
   - Custom transition animations
   - User-defined overlay content
   - Picture-in-picture transitions

3. **Smart Configuration**
   - Auto-adjust based on network
   - Adaptive quality settings
   - ML-based optimization

## Conclusion

These optimizations ensure a seamless experience for live streaming and recording operations. The modular design allows for easy maintenance and future enhancements while maintaining backward compatibility.