package com.serenegiant.glutils.es1;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLES10;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.serenegiant.utils.BuildCheck;
import com.serenegiant.usbcameracommon.BuildConfig;

import javax.microedition.khronos.opengles.GL10;

public final class GLHelper {
	private static final boolean DEBUG = BuildConfig.DEBUG;
	private static final String TAG = "GLHelper";

	private static final ThreadLocal<float[]> SCRATCH_ARRAY = ThreadLocal.withInitial(() -> new float[32]);

	// Private constructor to prevent instantiation
	private GLHelper() {}

	public static void checkGlError(@NonNull String op) {
		if (!DEBUG) return;

		int error;
		while ((error = GLES10.glGetError()) != GLES10.GL_NO_ERROR) {
			String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg, new Throwable());
		}
	}

	public static void checkGlError(@NonNull GL10 gl, @NonNull String op) {
		if (!DEBUG) return;

		int error;
		while ((error = gl.glGetError()) != GL10.GL_NO_ERROR) {
			String msg = op + ": glError 0x" + Integer.toHexString(error);
			Log.e(TAG, msg, new Throwable());
		}
	}

	public static int initTex(int texTarget, int filterParam) {
		return initTex(texTarget, GLES10.GL_TEXTURE0, filterParam, filterParam, GLES10.GL_CLAMP_TO_EDGE);
	}

	public static int initTex(int texTarget, int texUnit, int minFilter, int magFilter, int wrap) {
		int[] tex = new int[1];
		GLES10.glActiveTexture(texUnit);
		GLES10.glGenTextures(1, tex, 0);
		GLES10.glBindTexture(texTarget, tex[0]);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_S, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_WRAP_T, wrap);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MIN_FILTER, minFilter);
		GLES10.glTexParameterx(texTarget, GLES10.GL_TEXTURE_MAG_FILTER, magFilter);
		return tex[0];
	}

	public static void deleteTex(int hTex) {
		int[] tex = {hTex};
		GLES10.glDeleteTextures(1, tex, 0);
	}

	public static int loadTextureFromResource(@NonNull Context context, int resId) {
		Drawable drawable = ContextCompat.getDrawable(context, resId);
		if (drawable == null) {
			Log.e(TAG, "Resource not found: " + resId);
			return 0;
		}

		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		if (width <= 0 || height <= 0) {
			width = 256;
			height = 256;
		}

		Bitmap bitmap = null;
		try {
			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, width, height);
			drawable.draw(canvas);

			int[] textures = new int[1];
			GLES10.glGenTextures(1, textures, 0);
			GLES10.glBindTexture(GLES10.GL_TEXTURE_2D, textures[0]);

			GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MIN_FILTER, GLES10.GL_LINEAR);
			GLES10.glTexParameterx(GLES10.GL_TEXTURE_2D, GLES10.GL_TEXTURE_MAG_FILTER, GLES10.GL_LINEAR);
			GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);

			return textures[0];
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Error creating texture from resource", e);
			return 0;
		} finally {
			if (bitmap != null && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
		}
	}

	public static int createTextureWithTextContent(@NonNull String text) {
		final int width = 256;
		final int height = 256;

		Bitmap bitmap = null;
		try {
			bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawARGB(0, 0, 255, 0);

			Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			textPaint.setTextSize(32);
			textPaint.setARGB(0xff, 0xff, 0xff, 0xff);

			// Measure text for centering
			float textWidth = textPaint.measureText(text);
			canvas.drawText(text, (width - textWidth) / 2, height / 2, textPaint);

			int texture = initTex(GLES10.GL_TEXTURE_2D, GLES10.GL_LINEAR);
			GLUtils.texImage2D(GLES10.GL_TEXTURE_2D, 0, bitmap, 0);
			return texture;
		} catch (Exception e) {
			Log.e(TAG, "Error creating text texture", e);
			return 0;
		} finally {
			if (bitmap != null && !bitmap.isRecycled()) {
				bitmap.recycle();
			}
		}
	}

	public static void checkLocation(int location, @NonNull String label) {
		if (location < 0) {
			String msg = "Shader location not found: " + label;
			Log.e(TAG, msg);
			if (DEBUG) {
				throw new RuntimeException(msg);
			}
		}
	}

	@SuppressLint("InlinedApi")
	public static void logVersionInfo() {
		Log.i(TAG, "Vendor: " + GLES10.glGetString(GLES10.GL_VENDOR));
		Log.i(TAG, "Renderer: " + GLES10.glGetString(GLES10.GL_RENDERER));
		Log.i(TAG, "GL Version: " + GLES10.glGetString(GLES10.GL_VERSION));

		if (BuildCheck.isAndroid4_3()) {
			try {
				int[] values = new int[1];
				GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0);
				int major = values[0];
				GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0);
				int minor = values[0];
				Log.i(TAG, "GL ES Version: " + major + "." + minor);
			} catch (UnsatisfiedLinkError e) {
				Log.w(TAG, "GLES 3.0 not available");
			}
		}
	}

	// Matrix utility methods using thread-local scratch buffers
	public static void gluLookAt(float eyeX, float eyeY, float eyeZ,
								 float centerX, float centerY, float centerZ,
								 float upX, float upY, float upZ) {
		float[] scratch = SCRATCH_ARRAY.get();
		Matrix.setLookAtM(scratch, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
		GLES10.glMultMatrixf(scratch, 0);
	}

	public static void gluOrtho2D(float left, float right, float bottom, float top) {
		GLES10.glOrthof(left, right, bottom, top, -1.0f, 1.0f);
	}

	public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
		float ymax = zNear * (float)Math.tan(fovy * Math.PI / 360.0);
		float ymin = -ymax;
		float xmin = ymin * aspect;
		float xmax = ymax * aspect;
		GLES10.glFrustumf(xmin, xmax, ymin, ymax, zNear, zFar);
	}

	public static int gluProject(float objX, float objY, float objZ,
								 @NonNull float[] model, int modelOffset,
								 @NonNull float[] project, int projectOffset,
								 @NonNull int[] view, int viewOffset,
								 @NonNull float[] win, int winOffset) {

		float[] scratch = SCRATCH_ARRAY.get();
		final int M_OFFSET = 0;
		final int V_OFFSET = 16;
		final int V2_OFFSET = 20;

		Matrix.multiplyMM(scratch, M_OFFSET, project, projectOffset, model, modelOffset);

		scratch[V_OFFSET] = objX;
		scratch[V_OFFSET + 1] = objY;
		scratch[V_OFFSET + 2] = objZ;
		scratch[V_OFFSET + 3] = 1.0f;

		Matrix.multiplyMV(scratch, V2_OFFSET, scratch, M_OFFSET, scratch, V_OFFSET);

		float w = scratch[V2_OFFSET + 3];
		if (w == 0.0f) return GLES10.GL_FALSE;

		float rw = 1.0f / w;
		win[winOffset] = view[viewOffset] + view[viewOffset + 2] * (scratch[V2_OFFSET] * rw + 1.0f) * 0.5f;
		win[winOffset + 1] = view[viewOffset + 1] + view[viewOffset + 3] * (scratch[V2_OFFSET + 1] * rw + 1.0f) * 0.5f;
		win[winOffset + 2] = (scratch[V2_OFFSET + 2] * rw + 1.0f) * 0.5f;

		return GLES10.GL_TRUE;
	}

	public static int gluUnProject(float winX, float winY, float winZ,
								   @NonNull float[] model, int modelOffset,
								   @NonNull float[] project, int projectOffset,
								   @NonNull int[] view, int viewOffset,
								   @NonNull float[] obj, int objOffset) {

		float[] scratch = SCRATCH_ARRAY.get();
		final int PM_OFFSET = 0;
		final int INVPM_OFFSET = 16;
		final int V_OFFSET = 0;

		Matrix.multiplyMM(scratch, PM_OFFSET, project, projectOffset, model, modelOffset);
		if (!Matrix.invertM(scratch, INVPM_OFFSET, scratch, PM_OFFSET)) {
			return GLES10.GL_FALSE;
		}

		scratch[V_OFFSET] = 2.0f * (winX - view[viewOffset]) / view[viewOffset + 2] - 1.0f;
		scratch[V_OFFSET + 1] = 2.0f * (winY - view[viewOffset + 1]) / view[viewOffset + 3] - 1.0f;
		scratch[V_OFFSET + 2] = 2.0f * winZ - 1.0f;
		scratch[V_OFFSET + 3] = 1.0f;

		Matrix.multiplyMV(obj, objOffset, scratch, INVPM_OFFSET, scratch, V_OFFSET);
		return GLES10.GL_TRUE;
	}
}