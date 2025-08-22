package com.checkmate.android.viewmodels;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.checkmate.android.model.SurfaceModel;
import com.checkmate.android.util.MainActivity;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.wmspanel.libstream.AudioConfig;
import com.wmspanel.libstream.VideoConfig;

import org.w3c.dom.Text;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SharedViewModel extends ViewModel {

    private boolean isUsbRecording;
    private boolean isCameraOpened;
    private boolean isAudioOpened;
    private boolean isScreenCast;
    private SurfaceModel surfaceModel;
    private List<String> resolutions;
    VideoConfig videoConfigLocal;
    AudioConfig audioConfigLocal;

//    private int textureViewId = -1;

    private final MutableLiveData<Event<EventPayload>> eventLiveData = new MutableLiveData<>();

    public LiveData<Event<EventPayload>> getEventLiveData() {
        return eventLiveData;
    }

    public void postEvent(EventType eventType, Object data) {
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventLiveData.setValue(new Event<>(new EventPayload(eventType, data)));
                }
            });
        }
    }

    // Wrapper class to handle one-time events
    public static class Event<T> {
        private final T content;
        private boolean hasBeenHandled = false;

        public Event(T content) {
            this.content = content;
        }

        public T getContentIfNotHandled() {
            hasBeenHandled = true;
            return content;
        }

        public T peekContent() {
            return content;
        }
    }

    // Payload class to hold event type and data
    public static class EventPayload {
        private final EventType eventType;
        private final Object data;

        public EventPayload(EventType eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
        }

        public EventType getEventType() {
            return eventType;
        }

        public Object getData() {
            return data;
        }
    }

    public void saveCameraResolution(List<String> resolution) {
        Set<String> uniqueResolutions = new HashSet<>(resolution);
        resolutions = new ArrayList<>(uniqueResolutions);
        sortResolutions(resolutions);
    }

    public List<String> getCameraResolution() {
        if (resolutions == null) {
            // Return an empty list (or initialize resolutions appropriately)
            return new ArrayList<>();
        }
        Set<String> uniqueResolutions = new HashSet<>(resolutions);
        List<String> sortedResolutions = new ArrayList<>(uniqueResolutions);
        sortResolutions(sortedResolutions);
        return sortedResolutions;
    }

    private void sortResolutions(List<String> resolutions) {
        resolutions.sort(new Comparator<String>() {
            @Override
            public int compare(String res1, String res2) {
                String[] parts1 = res1.split("x");
                String[] parts2 = res2.split("x");
                int width1 = Integer.parseInt(parts1[0]);
                int height1 = Integer.parseInt(parts1[1]);
                int width2 = Integer.parseInt(parts2[0]);
                int height2 = Integer.parseInt(parts2[1]);

                if (width1 != width2) {
                    return Integer.compare(width2, width1);
                } else {
                    return Integer.compare(height2, height1);
                }
            }
        });
    }

    public void setSurfaceModel(SurfaceTexture surface ,int width , int height) {
        surfaceModel = new SurfaceModel(surface,width,height);
    }

    public SurfaceModel getSurfaceModel(){
        return surfaceModel;
    }
    public void setUsbRecording(boolean isUsbRecording) {
        this.isUsbRecording = isUsbRecording;
    }

    public boolean isUsbRecording() {
        return isUsbRecording;
    }

    public void setUsbStreaming(boolean isUsbRecording) {
        this.isScreenCast = false;
        this.isCameraOpened = false;
        this.isAudioOpened = false;
        this.isUsbRecording = isUsbRecording;
    }

    public boolean isUsbStreaming() {
        return isUsbRecording;
    }

    public void setCameraOpened(boolean isCameraOpened) {
        this.isScreenCast = false;
        this.isUsbRecording = false;
        this.isAudioOpened = false;
        this.isCameraOpened = isCameraOpened;
    }

    public boolean isCameraOpened() {
        return isCameraOpened;
    }

    public void seAudioOpened(boolean isAudioOpened) {
        this.isScreenCast = false;
        this.isUsbRecording = false;
        this.isCameraOpened = false;
        this.isAudioOpened = isAudioOpened;
    }

    public boolean isAudioOpened() {
        return isAudioOpened;
    }

    public void setScreenCastOpened(boolean isCameraOpened) {
        this.isCameraOpened = false;
        this.isUsbRecording = false;
        this.isAudioOpened = false;
        this.isScreenCast = isCameraOpened;
    }

    public boolean isScreenCastOpened() {
        return isScreenCast;
    }

    private WeakReference<TextureView> textureViewWeakReference;
    private WeakReference<TextureView> mtextureViewWeakReference;

    public void setTextureView(TextureView textureView) {
        this.textureViewWeakReference = new WeakReference<>(textureView);
    }

    public TextureView getTextureView() {
        return textureViewWeakReference != null ? textureViewWeakReference.get() : null;
    }

    public void setmTextureView(TextureView textureView) {
        this.mtextureViewWeakReference = new WeakReference<>(textureView);
        if (MainActivity.getInstance() != null) {
            if (MainActivity.getInstance().mUSBService != null) {
                TextureView tv = mtextureViewWeakReference.get();
                MainActivity.getInstance().mUSBService.setPreviewSurface(tv.getSurfaceTexture(),tv.getWidth(),tv.getHeight());
            }
        }
    }

    public void setScreenCastVideoConfig (VideoConfig videoConfig, AudioConfig audioConfig) {
        videoConfigLocal = videoConfig;
        audioConfigLocal = audioConfig;
    }

    public VideoConfig getVideoConfig() {
        return videoConfigLocal;
    }

    public AudioConfig getAudioConfig() {
        return audioConfigLocal;
    }

    public TextureView getmTextureView() {
        return mtextureViewWeakReference != null ? mtextureViewWeakReference.get() : null;
    }

}
