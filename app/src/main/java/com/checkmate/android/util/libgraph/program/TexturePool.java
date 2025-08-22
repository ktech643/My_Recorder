package com.checkmate.android.util.libgraph.program;

import android.opengl.GLES20;

import com.checkmate.android.util.libgraph.egl.ContextListener;
import com.checkmate.android.util.libgraph.egl.EglCore;
import com.checkmate.android.util.libgraph.gl.GlThread;
import com.checkmate.android.util.libgraph.gl.GlUtil;

import java.util.ArrayDeque;
import java.util.Queue;

public class TexturePool implements ContextListener {
    private final Queue<Integer> pool = new ArrayDeque<>();
    private final int texTarget;
    private final GlThread glThread;
    private boolean contextValid = true;

    public TexturePool(int texTarget, GlThread glThread, EglCore core) {
        this.texTarget = texTarget;
        this.glThread = glThread;
        core.addContextListener(this);
    }


    @Override
    public void onContextRecreated() {
        contextValid = true;
    }

    public int acquire() {
        if (!contextValid || pool.isEmpty()) return createTexture();
        return pool.poll();
    }

    @Override
    public void onContextLost() {
        contextValid = false;
        clearPool(); // Immediately release all textures
    }

    private void clearPool() {
        glThread.post(() -> {
            int[] ids = new int[pool.size()];
            int i = 0;
            for (Integer tex : pool) ids[i++] = tex;
            GLES20.glDeleteTextures(ids.length, ids, 0);
            pool.clear();
        });
    }

    private int createTexture() {
        final int[] texId = new int[1];
        glThread.post(() -> {
            GLES20.glGenTextures(1, texId, 0);
            GLES20.glBindTexture(texTarget, texId[0]);
            GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(texTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(texTarget, 0);
        });
        return texId[0];
    }

    public void release(int texture) {
        glThread.post(() -> {
            synchronized (pool) {
                pool.offer(texture);
            }
        });
    }

    public void clear() {
        glThread.post(() -> {
            for (int tex : pool) GLES20.glDeleteTextures(1, new int[]{tex}, 0);
            pool.clear();
        });
    }
}