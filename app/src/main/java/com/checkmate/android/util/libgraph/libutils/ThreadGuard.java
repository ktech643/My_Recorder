package com.checkmate.android.util.libgraph.libutils;

/**
 * Debug-only helper that records the thread on which an object was
 * created and throws if a method is later called from a different
 * thread.  In release builds the check is stripped out by R8/ProGuard.
 */
public final class ThreadGuard {

    private final long owner = Thread.currentThread().getId();

    /** Call at the top of any public method that must stay on the owner thread. */
    public void check() {
        if (Thread.currentThread().getId() != owner) {
            throw new IllegalStateException(
                    "Wrong thread: created on " + owner +
                            ", now on " + Thread.currentThread().getId());
        }
    }
}
