package com.checkmate.android.util.libgraph;

import android.os.Handler;
import android.os.HandlerThread;

public class GlThread extends HandlerThread {
    private Handler handler;

    public GlThread() { super("GLThread"); start(); // ensure looper
        handler = new Handler(getLooper());
    }

    public void post(Runnable task) {
        handler.post(task);
    }

    @Override
    public void onLooperPrepared() {
        handler = new Handler(getLooper());
    }
}