package com.checkmate.android.util.libgraph.egl;

import com.checkmate.android.util.libgraph.libutils.ThreadGuard; /**
 * Thin, thread-safe wrapper around an EGL14 display + context pair.
 *
 *  • minSdk 28, compileSdk 34
 *  • Thread-checked in debug builds (see {@link ThreadGuard})
 *  • Uses Crashlytics for non-fatal driver errors
 */

public interface ContextListener {
    void onContextLost();
    void onContextRecreated();
}
