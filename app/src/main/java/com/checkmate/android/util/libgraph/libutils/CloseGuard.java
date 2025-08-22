package com.checkmate.android.util.libgraph.libutils;

import android.util.Log;

/**
 * Simple finalizer-based leak detector.  In debug builds it logs a warning
 * if {@link #close()} was never called on the guarded resource.
 */
public final class CloseGuard {
    private static final String TAG = "CloseGuard";

    private final String label;
    private volatile boolean closed;

    public CloseGuard(String label) { this.label = label; }

    /** Mark the resource as closed (call from your release()/close()). */
    public void close() { closed = true; }

    @Override protected void finalize() throws Throwable {
        try {
            if (!closed) {
                Log.w(TAG, "Leaked resource: " + label);
            }
        } finally {
            super.finalize();
        }
    }
}
