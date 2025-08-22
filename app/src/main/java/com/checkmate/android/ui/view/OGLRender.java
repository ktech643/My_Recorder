package com.checkmate.android.ui.view;

import android.opengl.GLSurfaceView;
import com.checkmate.android.util.MainActivity;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OGLRender implements GLSurfaceView.Renderer {

    /**
     * Initialize the model data.
     */
    public OGLRender()
    {
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        MainActivity.instance.DelGL();
        MainActivity.instance.InitGL();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        MainActivity.instance.ChangeGL(width,height);
    }

    @Override
    public void onDrawFrame(GL10 glUnused)
    {
        if (MainActivity.instance!=null) {
            MainActivity.instance.UpdateGl();
        }
    }
}