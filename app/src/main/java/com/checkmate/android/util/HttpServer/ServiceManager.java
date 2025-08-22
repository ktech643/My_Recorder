package com.checkmate.android.util.HttpServer;

import android.util.Log;

import com.checkmate.android.util.HttpServer.BgService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Manages switching between multiple BgService implementations at runtime.
 * Uses constructor injection for Hilt dependency injection.
 */
@Singleton
public class ServiceManager {
    private final BgService cameraService;

    BgService activeService;

    /**
     * Constructor for Hilt dependency injection
     */
    @Inject
    public ServiceManager(
            @Named("camera") BgService cameraService,
            @Named("cast") BgService castService,
            @Named("usb") BgService usbService) {
        this.cameraService = cameraService;

        Log.d("ServiceManager", "ServiceManager created");
        Log.d("ServiceManager", "Dependencies injected: " +
                "\nCamera: " + cameraService);
    }

    public void checkServiceAvailability() {
        if (cameraService == null) Log.e("DI", "Camera service not provisioned");
    }

    /**
     * Switches the active BgService instance.  Stops any currently running service.
     * @param type one of "camera", "cast", "audio", or "usb"
     */
    public void switchServiceDI(String type) {
        // Stop previous service if exists
        if (activeService != null) {
            activeService.stopServiceDI();
        }

        // Select new service with null check
        BgService newService;
        switch (type) {
            case "camera":
                if (cameraService == null) throw new IllegalStateException("Camera service not initialized");
                newService = cameraService;
                break;
            default:
                throw new IllegalArgumentException("Unknown service type: " + type);
        }

        activeService = newService;
        activeService.startServiceDI();
    }
    /** Starts streaming on the currently active service. */
    public void startStreamDI() {
        ensureActive();
        activeService.startStreamDI();
    }

    /** Stops streaming on the currently active service. */
    public void stopStreamDI() {
        ensureActive();
        activeService.stopStreamDI();
    }

    /** Starts recording on the currently active service. */
    public void startRecordDI() {
        ensureActive();
        activeService.startRecordingDI();
    }

    /** Stops recording on the currently active service. */
    public void stopRecordDI() {
        ensureActive();
        activeService.stopRecordingDI();
    }

    /** Toggles audio capture on/off on the currently active service. */
    public void switchAudioDI(boolean enable) {
        ensureActive();
        activeService.switchAudioDI(enable);
    }

    private void ensureActive() {
        if (activeService == null) {
            throw new IllegalStateException("No active service. Call switchServiceDI() first.");
        }
    }
}
