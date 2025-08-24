package com.checkmate.android.util.libgraph.libutils;

/**
 * One-liner wrapper so GL code can report non-fatal exceptions without a
 * direct dependency on Crashlytics everywhere.
 *
 * If Crashlytics is not present (unit tests), the call is silently ignored.
 */
public final class GlCrashReporter {

    private GlCrashReporter() { /* no-instantiation */ }

    public static void log(Throwable t) {
        // Firebase Crashlytics removed - silently ignore for now
        // TODO: Implement alternative logging mechanism
    }
}
