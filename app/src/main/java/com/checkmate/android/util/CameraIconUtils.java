package com.checkmate.android.util;

import android.widget.ImageView;
import com.checkmate.android.R;
import com.checkmate.android.model.Camera;

public class CameraIconUtils {
    
    /**
     * Set the appropriate camera icon based on camera type
     * @param imageView The ImageView to set the icon
     * @param cameraType The camera type
     */
    public static void setCameraIcon(ImageView imageView, Camera.TYPE cameraType) {
        if (imageView == null) return;
        
        switch (cameraType) {
            case REAR_CAMERA:
                imageView.setImageResource(R.mipmap.ic_camera);
                break;
            case FRONT_CAMERA:
                imageView.setImageResource(R.mipmap.ic_camera);
                break;
            case USB_CAMERA:
                imageView.setImageResource(R.mipmap.ic_refresh);
                break;
            case WIFI_CAMERA:
                imageView.setImageResource(R.mipmap.ic_stream);
                break;
            case SCREEN_CAST:
                imageView.setImageResource(R.mipmap.ic_cast);
                break;
            case AUDIO_ONLY:
                imageView.setImageResource(R.mipmap.ic_radio);
                break;
            default:
                imageView.setImageResource(R.mipmap.ic_camera);
                break;
        }
    }
    
    /**
     * Set camera icon based on camera type string
     * @param imageView The ImageView to set the icon
     * @param cameraTypeString The camera type as string
     */
    public static void setCameraIconByString(ImageView imageView, String cameraTypeString) {
        if (imageView == null || cameraTypeString == null) return;
        
        if (cameraTypeString.contains("USB") || cameraTypeString.contains("usb")) {
            imageView.setImageResource(R.mipmap.ic_refresh);
        } else if (cameraTypeString.contains("WiFi") || cameraTypeString.contains("wifi") || 
                   cameraTypeString.contains("IP") || cameraTypeString.contains("ip")) {
            imageView.setImageResource(R.mipmap.ic_stream);
        } else if (cameraTypeString.contains("Front") || cameraTypeString.contains("front")) {
            imageView.setImageResource(R.mipmap.ic_camera);
        } else if (cameraTypeString.contains("Back") || cameraTypeString.contains("back")) {
            imageView.setImageResource(R.mipmap.ic_camera);
        } else if (cameraTypeString.contains("Cast") || cameraTypeString.contains("cast")) {
            imageView.setImageResource(R.mipmap.ic_cast);
        } else if (cameraTypeString.contains("Audio") || cameraTypeString.contains("audio")) {
            imageView.setImageResource(R.mipmap.ic_radio);
        } else {
            imageView.setImageResource(R.mipmap.ic_camera);
        }
    }
    
    /**
     * Get the appropriate icon resource ID based on camera type
     * @param cameraType The camera type
     * @return The icon resource ID
     */
    public static int getCameraIconResource(Camera.TYPE cameraType) {
        switch (cameraType) {
            case REAR_CAMERA:
            case FRONT_CAMERA:
                return R.mipmap.ic_camera;
            case USB_CAMERA:
                return R.mipmap.ic_refresh;
            case WIFI_CAMERA:
                return R.mipmap.ic_stream;
            case SCREEN_CAST:
                return R.mipmap.ic_cast;
            case AUDIO_ONLY:
                return R.mipmap.ic_radio;
            default:
                return R.mipmap.ic_camera;
        }
    }
    
    /**
     * Get the appropriate icon resource ID based on camera type string
     * @param cameraTypeString The camera type as string
     * @return The icon resource ID
     */
    public static int getCameraIconResourceByString(String cameraTypeString) {
        if (cameraTypeString == null) return R.mipmap.ic_camera;
        
        if (cameraTypeString.contains("USB") || cameraTypeString.contains("usb")) {
            return R.mipmap.ic_refresh;
        } else if (cameraTypeString.contains("WiFi") || cameraTypeString.contains("wifi") || 
                   cameraTypeString.contains("IP") || cameraTypeString.contains("ip")) {
            return R.mipmap.ic_stream;
        } else if (cameraTypeString.contains("Front") || cameraTypeString.contains("front")) {
            return R.mipmap.ic_camera;
        } else if (cameraTypeString.contains("Back") || cameraTypeString.contains("back")) {
            return R.mipmap.ic_camera;
        } else if (cameraTypeString.contains("Cast") || cameraTypeString.contains("cast")) {
            return R.mipmap.ic_cast;
        } else if (cameraTypeString.contains("Audio") || cameraTypeString.contains("audio")) {
            return R.mipmap.ic_radio;
        } else {
            return R.mipmap.ic_camera;
        }
    }
}
