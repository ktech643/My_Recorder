package com.checkmate.android.util.libgraph.libutils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * One-liner wrapper so GL code can report non-fatal exceptions without a
 * direct dependency on Crashlytics everywhere.
 *
 * If Crashlytics is not present (unit tests), the call is silently ignored.
 */
public final class GlCrashReporter {

    private GlCrashReporter() { /* no-instantiation */ }

    public static void log(Throwable t) {
        try {
            FirebaseCrashlytics.getInstance().recordException(t);
        } catch (Throwable ignore) {
            // Crashlytics not on class-path (e.g. unit-test) – ignore.
        }
    }
}
