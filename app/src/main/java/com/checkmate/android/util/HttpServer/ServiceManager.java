package com.checkmate.android.util.HttpServer;

import android.util.Log;

import com.checkmate.android.util.HttpServer.BgService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Manages switching between multiple BgService implementations at runtime.
 * Uses field injection so Toothpick can generate the factory via a no-arg constructor.
 */
@Singleton
public class ServiceManager {
    @Inject @Named("camera") public BgService cameraService;
    @Inject @Named("cast")   public BgService castService;
    @Inject @Named("audio")  public BgService audioService;
    @Inject @Named("usb")    public BgService usbService;

    BgService activeService;

    /**
     * No-arg constructor for Toothpick
     */
    @Inject
    public ServiceManager() {
        Log.d("ServiceManager", "ServiceManager created");
    }

    public void checkServiceAvailability() {
        if (cameraService == null) Log.e("DI", "Camera service not provisioned");
        if (castService == null) Log.e("DI", "Cast service not provisioned");
        if (audioService == null) Log.e("DI", "Audio service not provisioned");
        if (usbService == null) Log.e("DI", "USB service not provisioned");
    }
    @Inject
    void initialize() {
        Log.d("ServiceManager", "Dependencies injected: " +
                "\nCamera: " + cameraService +
                "\nCast: " + castService +
                "\nAudio: " + audioService +
                "\nUSB: " + usbService);
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
            case "cast":
                if (castService == null) throw new IllegalStateException("Cast service not initialized");
                newService = castService;
                break;
            case "audio":
                if (audioService == null) throw new IllegalStateException("Audio service not initialized");
                newService = audioService;
                break;
            case "usb":
                if (usbService == null) throw new IllegalStateException("USB service not initialized");
                newService = usbService;
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
