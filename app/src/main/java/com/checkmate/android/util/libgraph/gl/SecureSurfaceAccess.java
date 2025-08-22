package com.checkmate.android.util.libgraph.gl;

import java.util.Arrays;

public class SecureSurfaceAccess {
    private static final String[] ALLOWED_PATHS = {
            "/dev/graphics/",  // More general path
            "/sys/class/graphics/",
            "SurfaceTexture"   // Allow SurfaceTexture sources
    };

    public static void validateAccess(String path) {
        for (String allowed : ALLOWED_PATHS) {
            if (allowed.endsWith("/") && path.startsWith(allowed)) return;
            else if (path.contains(allowed)) return; // Keep for "SurfaceTexture" if needed
        }
        throw new SecurityException("Unauthorized surface access: " + path);
    }
}