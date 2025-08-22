# Shared EGL Manager Implementation

## Overview

The `SharedEglManager` has been implemented as a robust singleton pattern to ensure only one EGL context is active across all background services in the Android application. This prevents crashes and improves performance by avoiding the creation and destruction of EGL contexts when switching between services.

## Key Features

### 1. Singleton Pattern
- Only one instance of `SharedEglManager` exists at any time
- Thread-safe initialization and access
- Proper lifecycle management

### 2. Service Management
- Services register themselves with the shared EGL manager
- Automatic service switching when new services become active
- Proper cleanup when services are destroyed

### 3. EGL Context Persistence
- EGL context remains active across service switches
- Only surface updates are performed, not full EGL recreation
- Improved performance and stability

## Implementation Details

### Service Registration

Each service must implement the `getServiceType()` method and register itself:

```java
@Override
protected ServiceType getServiceType() {
    return ServiceType.BgCamera; // or BgAudio, BgScreenCast, BgUSBCamera
}
```

The service is automatically registered in `BaseBackgroundService.onCreate()` and unregistered in `onDestroy()`.

### Surface Updates

When a service needs to update its display surface, use:

```java
// For the currently active service
updateServiceSurface(surfaceTexture, width, height);

// Or directly set preview surface
setPreviewSurface(surfaceTexture, width, height);
```

### Service Types

- `BgCamera`: Background camera service
- `BgAudio`: Background audio service  
- `BgScreenCast`: Screen casting service
- `BgUSBCamera`: USB camera service

## Usage in Services

### 1. BgCameraService
```java
@Override
protected ServiceType getServiceType() {
    return ServiceType.BgCamera;
}

// The service automatically gets the singleton instance
// mEglManager = SharedEglManager.getInstance();
```

### 2. BgAudioService
```java
@Override
protected ServiceType getServiceType() {
    return ServiceType.BgAudio;
}
```

### 3. BgCastService
```java
@Override
protected ServiceType getServiceType() {
    return ServiceType.BgScreenCast;
}
```

### 4. BgUSBService
```java
@Override
protected ServiceType getServiceType() {
    return ServiceType.BgUSBCamera;
}
```

## Benefits

1. **Crash Prevention**: No more EGL context recreation crashes
2. **Performance**: Faster service switching
3. **Memory Efficiency**: Single EGL context shared across services
4. **Stability**: Robust error handling and state management
5. **Maintainability**: Centralized EGL management

## Error Handling

The implementation includes comprehensive error handling:

- Initialization state tracking
- Shutdown state management
- Service registration validation
- EGL context state verification
- Automatic cleanup of dead service references

## Thread Safety

All critical operations are thread-safe:

- Singleton instance creation
- Service registration/unregistration
- EGL context management
- Surface updates

## Lifecycle Management

1. **Initialization**: EGL context created once when first service registers
2. **Service Switching**: Only surface updates, no EGL recreation
3. **Cleanup**: Automatic cleanup when all services are destroyed
4. **Recovery**: Proper state reset for re-initialization

## Migration Notes

- Existing service code continues to work without changes
- The `@Inject` annotation for `SharedEglManager` is no longer needed
- Services automatically get the singleton instance
- Surface update methods remain the same
- All existing EGL operations are preserved 