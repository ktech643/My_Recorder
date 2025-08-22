package com.checkmate.android.service.SharedEGL;

import toothpick.config.Module;

public class GraphicsModule extends Module {
    public GraphicsModule() {
        bind(SharedEglManager.class).singleton();
        // Add other graphics-related bindings as needed
    }
}