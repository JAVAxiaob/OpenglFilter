package com.camerafilter.renderer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import androidx.camera.core.Preview;

import com.camerafilter.filter.FilterManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRenderer implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener {

    private static final String TAG = "CameraRenderer";

    private final FilterManager filterManager;
    private SurfaceTexture mSurfaceTexture;
    private GLSurfaceView glSurfaceView;
    private int mCameraTextureId;
    private boolean surfaceTextureAttached = false;

    public CameraRenderer(Context context, GLSurfaceView glSurfaceView) {
        this.glSurfaceView = glSurfaceView;
        filterManager = new FilterManager();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        filterManager.init(); // 延迟初始化 Shader
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mCameraTextureId = textures[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mCameraTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        if (mSurfaceTexture != null && !surfaceTextureAttached) {
            attachSurfaceTexture();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture == null) return;

        mSurfaceTexture.updateTexImage();

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        filterManager.onDraw(mCameraTextureId);
    }

    public void setFilter(FilterManager.FilterType type) {
        filterManager.setCurrentFilter(type);
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        mSurfaceTexture = output.getSurfaceTexture();

        if (glSurfaceView != null && glSurfaceView.getHolder().getSurface().isValid() && !surfaceTextureAttached) {
            glSurfaceView.queueEvent(this::attachSurfaceTexture);
        }
    }

    private void attachSurfaceTexture() {
        try {
            mSurfaceTexture.attachToGLContext(mCameraTextureId);
            mSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> glSurfaceView.requestRender());
            surfaceTextureAttached = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }
}