# Synchronized Permission Implementation Guide

## Overview

This implementation provides a fully synchronized permission request system for Android that prevents UI freezing, crashes, and window leaks when permission popups appear.

## Key Components

### 1. PermissionManager (`com.checkmate.android.util.PermissionManager`)

The core class that handles all permission requests in a synchronized manner:

- **Sequential Processing**: Requests permissions one at a time with delays between each request
- **Queue System**: Uses a queue to manage multiple permission requests
- **Lifecycle Aware**: Properly handles activity lifecycle to prevent leaks
- **Callback System**: Provides callbacks for permission results

### 2. DialogSynchronizer (`com.checkmate.android.util.DialogSynchronizer`)

Manages dialog display to prevent multiple dialogs from appearing simultaneously:

- **Dialog Queue**: Queues dialogs to show them sequentially
- **Automatic Cleanup**: Handles dialog dismissal and cleanup
- **Activity Lifecycle**: Checks activity state before showing dialogs

### 3. Updated SplashActivity

The main activity has been refactored with:

- **Thread-Safe Operations**: All dialog and permission operations are synchronized
- **Lifecycle Management**: Proper cleanup in onDestroy() and onStop()
- **Window Stability**: Enhanced window flags to prevent UI flickering
- **UI Thread Safety**: All UI operations run on the main thread

## Key Features

### 1. Synchronized Permission Flow

```java
// Permission requests are queued and processed sequentially
permissionManager.requestAllPermissions();
```

### 2. Thread-Safe Dialog Management

```java
synchronized (dialogLock) {
    if (is_dialog_show) {
        return;
    }
    is_dialog_show = true;
}
```

### 3. Lifecycle-Aware Cleanup

```java
@Override
protected void onDestroy() {
    super.onDestroy();
    if (permissionManager != null) {
        permissionManager.cancelPermissionFlow();
    }
}
```

## Implementation Details

### Permission Request Flow

1. **Essential Permissions** (Camera, Audio, etc.)
2. **Storage Permissions** (Media access)
3. **Network Permissions** (Location, WiFi)
4. **Notification Permission** (Android 13+)
5. **Special Permissions** (Overlay, Battery optimization)

### Delay Timings

- **Between Permissions**: 300ms delay
- **Between Dialogs**: 300ms delay
- **After Special Permissions**: 1000ms delay

### Error Handling

- Activity state checks before showing dialogs
- Try-catch blocks around critical operations
- Graceful degradation if permissions are denied

## Testing

Use `PermissionTestActivity` to verify:

1. No UI freezing during permission requests
2. No crashes when rotating device
3. No window leaks in memory profiler
4. Proper handling of permission denials

## Best Practices

1. **Always Check Activity State**: Before showing dialogs or requesting permissions
2. **Use Synchronized Blocks**: For shared state variables
3. **Run UI Operations on Main Thread**: Use `runOnUiThread()`
4. **Handle All Lifecycle Events**: Proper cleanup in onDestroy()
5. **Add Delays Between Operations**: Prevents UI overload

## Migration from Old Implementation

### Before:
```java
ActivityCompat.requestPermissions(activity, PERMISSIONS, REQUEST_CODE);
requestDrawOverAppsPermission(activity);
requestIgnoreBatteryOptimizationsPermission(activity);
// Multiple simultaneous requests
```

### After:
```java
permissionManager.requestAllPermissions();
// Sequential, synchronized requests
```

## Troubleshooting

### UI Freezing
- Check for synchronized blocks
- Ensure delays between operations
- Verify UI thread usage

### Window Leaks
- Check onDestroy() cleanup
- Verify dialog dismissal
- Monitor activity references

### Crashes
- Check for null activity references
- Verify permission array contents
- Monitor callback exceptions

## Performance Considerations

- Sequential processing adds ~2-3 seconds to permission flow
- Trade-off between stability and speed
- Can be optimized by grouping compatible permissions

## Future Improvements

1. Group compatible permissions to reduce delays
2. Add progress indicator during permission flow
3. Implement retry mechanism for failed permissions
4. Add analytics for permission grant rates