# Thread Safety and ANR Prevention Guide

## Overview

This guide documents the thread-safe implementations and ANR prevention mechanisms implemented in the CheckMate Android application.

## Key Components

### 1. ThreadSafeAppPreference

A thread-safe replacement for the original AppPreference class with the following features:

- **Thread Safety**: Uses ReadWriteLock for concurrent access
- **Type Safety**: Strongly typed getter/setter methods with null safety
- **Caching**: In-memory cache for frequently accessed values
- **Non-blocking Writes**: Uses apply() instead of commit() for asynchronous writes
- **Fallback Values**: Recovery mechanism for critical preferences
- **Batch Updates**: Atomic batch operations for multiple preference changes

**Usage Example:**
```java
// Initialize once in Application class
ThreadSafeAppPreference.initialize(sharedPreferences);

// Use throughout the app
ThreadSafeAppPreference.getInstance().setBoolean(KEY.IS_APP_BACKGROUND, false);
boolean isBackground = ThreadSafeAppPreference.getInstance().getBoolean(KEY.IS_APP_BACKGROUND, false);

// Batch updates
ThreadSafeAppPreference.getInstance().batchUpdate(editor -> {
    editor.putInt(KEY.VIDEO_QUALITY, 1080)
          .putBoolean(KEY.RECORD_AUDIO, true)
          .putString(KEY.DEVICE_ID, deviceId);
});
```

### 2. CrashLogger

An internal crash tracking and logging system with:

- **Session-based Logging**: New log file for each app session
- **Automatic File Rotation**: Manages log file size and count
- **ANR Detection**: Built-in ANR watchdog with 5-second timeout
- **Thread-safe Operations**: All logging operations are queued and processed asynchronously
- **Comprehensive Logging**: Captures device info, app version, and stack traces

**Usage Example:**
```java
// Initialize in Application class
CrashLogger.initialize(context);

// Use throughout the app
CrashLogger logger = CrashLogger.getInstance();
logger.i(TAG, "Information message");
logger.e(TAG, "Error message", exception);
logger.logCrash(TAG, "Critical error", throwable);
```

### 3. ANRHandler

Advanced ANR prevention and recovery system with:

- **Timeout Protection**: Wraps main thread operations with configurable timeouts
- **Recovery Strategies**: Multiple recovery mechanisms (cache clearing, GC, thread priority)
- **Background Execution**: Automatic offloading of heavy operations
- **Task Management**: Tracks and can cancel long-running operations

**Usage Example:**
```java
ANRHandler handler = ANRHandler.getInstance();

// Execute with timeout protection
handler.executeOnMainThreadSafe(
    "updateUI",
    () -> {
        // Heavy UI operation
        return processData();
    },
    result -> {
        // Success callback
        updateUI(result);
    },
    error -> {
        // Error callback
        showError(error);
    }
);

// Execute in background with main thread callback
handler.executeBackgroundTask(
    "loadData",
    () -> {
        // Background operation
        return loadFromNetwork();
    },
    data -> {
        // Called on main thread
        displayData(data);
    },
    error -> {
        // Error handling on main thread
        handleError(error);
    }
);
```

### 4. ThreadSafeDBManager

Thread-safe database operations with:

- **Background Execution**: All database operations run on dedicated thread
- **Timeout Protection**: 3-second timeout for database operations
- **Asynchronous API**: Callback-based interface for all operations
- **Synchronous Fallback**: Limited sync methods with timeout protection

**Usage Example:**
```java
ThreadSafeDBManager dbManager = ThreadSafeDBManager.getInstance(context);

// Add camera asynchronously
dbManager.addCamera(camera, new DatabaseCallback<Boolean>() {
    @Override
    public void onSuccess(Boolean result) {
        // Handle success on main thread
    }
    
    @Override
    public void onError(Exception e) {
        // Handle error on main thread
    }
});

// Get cameras asynchronously
dbManager.getCameras(cameras -> {
    // Process cameras on main thread
    updateCameraList(cameras);
});
```

### 5. ThreadSafeFileUtils

Thread-safe file operations with:

- **Non-blocking I/O**: All file operations execute on background threads
- **Timeout Protection**: 5-second timeout for file operations
- **Stream Operations**: Efficient stream copying with buffering
- **Error Recovery**: Graceful error handling and logging

**Usage Example:**
```java
ThreadSafeFileUtils fileUtils = ThreadSafeFileUtils.getInstance();

// Write file asynchronously
fileUtils.writeStringToFile(file, content, new FileOperationCallback<Boolean>() {
    @Override
    public void onSuccess(Boolean result) {
        // File written successfully
    }
    
    @Override
    public void onError(Exception e) {
        // Handle error
    }
});

// Read file asynchronously
fileUtils.readStringFromFile(file, content -> {
    // Process file content on main thread
    processContent(content);
});
```

## Best Practices for ANR Prevention

### 1. Main Thread Operations
- Keep main thread operations under 100ms
- Use ANRHandler for operations that might exceed this
- Offload heavy computations to background threads

### 2. Database Operations
- Always use ThreadSafeDBManager instead of direct database access
- Avoid synchronous database calls on main thread
- Use batch operations when possible

### 3. File Operations
- Use ThreadSafeFileUtils for all file I/O
- Avoid large file operations on main thread
- Consider streaming for large files

### 4. Preference Access
- Use ThreadSafeAppPreference for all preference operations
- Take advantage of caching for frequently accessed values
- Use batch updates for multiple changes

### 5. Network Operations
- Always perform on background threads
- Use timeouts to prevent indefinite blocking
- Implement proper error handling and recovery

### 6. UI Updates
- Use Handler or runOnUiThread for UI updates from background threads
- Batch UI updates when possible
- Consider using RecyclerView for large lists

## Migration Guide

### Migrating from AppPreference to ThreadSafeAppPreference

1. Replace initialization:
```java
// Old
AppPreference.initialize(preferences);

// New
ThreadSafeAppPreference.initialize(preferences);
```

2. Update method calls:
```java
// Old
AppPreference.setBool(KEY.IS_APP_BACKGROUND, false);
boolean value = AppPreference.getBool(KEY.IS_APP_BACKGROUND, false);

// New
ThreadSafeAppPreference.getInstance().setBoolean(KEY.IS_APP_BACKGROUND, false);
boolean value = ThreadSafeAppPreference.getInstance().getBoolean(KEY.IS_APP_BACKGROUND, false);
```

### Migrating from DBManager to ThreadSafeDBManager

1. Replace synchronous calls with asynchronous:
```java
// Old
List<Camera> cameras = DBManager.getInstance().getCameras();
processCameras(cameras);

// New
ThreadSafeDBManager.getInstance(context).getCameras(cameras -> {
    processCameras(cameras);
});
```

2. Add error handling:
```java
// New pattern with error handling
dbManager.addCamera(camera, new DatabaseCallback<Boolean>() {
    @Override
    public void onSuccess(Boolean result) {
        if (result) {
            showSuccess("Camera added");
        }
    }
    
    @Override
    public void onError(Exception e) {
        showError("Failed to add camera: " + e.getMessage());
    }
});
```

## Monitoring and Debugging

### Crash Logs
- Location: `<app_files_dir>/crash_logs/`
- Format: `crash_log_YYYYMMDD_HHMMSS.txt`
- Contains: Crashes, ANRs, errors, warnings

### ANR Detection
- Automatic detection with 5-second threshold
- Logs thread states and stack traces
- Attempts automatic recovery

### Performance Monitoring
- Use Android Studio Profiler to identify bottlenecks
- Monitor crash logs for ANR patterns
- Review thread usage in crash reports

## Conclusion

These thread-safe implementations provide a robust foundation for preventing ANRs and crashes. Always prioritize using these utilities over direct operations that might block the main thread. Regular monitoring of crash logs will help identify and address any remaining issues.