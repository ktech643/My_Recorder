package com.checkmate.android.util.libgraph.gl;

import android.opengl.GLES20;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class GlResourceManager {
    private static final ReentrantLock lock = new ReentrantLock();
    private static GlThread glThread;
    private static final ConcurrentLinkedQueue<Integer> textures = new ConcurrentLinkedQueue<>();
    private static final int BATCH_SIZE = 5;

    public static void init(GlThread thread) {
        glThread = thread;
    }

    // Add batch processing threshold

    public static void scheduleTextureDeletion(int texture) {
        if (glThread == null) return;

        synchronized (lock) {
            textures.add(texture);
            if (textures.size() >= BATCH_SIZE) {
                glThread.post(GlResourceManager::performCleanup);
            }
        }
    }

    public static void performCleanup() {
        if (glThread == null || textures.isEmpty()) return;
        int[] ids = new int[textures.size()];
        int i = 0;
        while (!textures.isEmpty()) {
            Integer tex = textures.poll();
            if (tex != null) ids[i++] = tex;
        }

        GLES20.glDeleteTextures(ids.length, ids, 0);
    }
}