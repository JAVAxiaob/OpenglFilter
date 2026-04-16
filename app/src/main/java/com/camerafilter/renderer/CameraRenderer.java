//package com.camerafilter.renderer;
//
//import android.content.Context;
//
//import android.graphics.SurfaceTexture;
//import android.opengl.GLES20;
//import android.opengl.GLSurfaceView;
//import android.view.Surface;
//
//import androidx.camera.core.Preview;
//
//import com.camerafilter.filter.FilterManager;
//
//import javax.microedition.khronos.egl.EGLConfig;
//import javax.microedition.khronos.opengles.GL10;
//
//public class CameraRenderer implements GLSurfaceView.Renderer {
//    private final FilterManager filterManager;
//    private SurfaceTexture mSurfaceTexture;
//    private Surface mSurface;
//    private final GLSurfaceView glSurfaceView;
//
//    public CameraRenderer(Context context, GLSurfaceView glSurfaceView) {
//        this.glSurfaceView = glSurfaceView;
//        filterManager = new FilterManager();
//    }
//
//    @Override
//    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
//        int[] textures = new int[1];
//        GLES20.glGenTextures(1, textures, 0);
//
//        mSurfaceTexture = new SurfaceTexture(textures[0]);
//        mSurfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
//            glSurfaceView.requestRender();
//        });
//
//        mSurface = new Surface(mSurfaceTexture);
//    }
//
//    @Override
//    public void onSurfaceChanged(GL10 gl, int width, int height) {
//        GLES20.glViewport(0, 0, width, height);
//    }
//
//    @Override
//    public void onDrawFrame(GL10 gl) {
//        if (mSurfaceTexture == null) return;
//        mSurfaceTexture.updateTexImage();
//
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
//        GLES20.glClearColor(0, 0, 0, 1);
//
//        filterManager.onDraw(0);
//    }
//
//    public void setFilter(FilterManager.FilterType type) {
//        filterManager.setCurrentFilter(type);
//    }
//
//    // CameraX alpha05 专用
//    public Preview.PreviewSurfaceProvider getPreviewSurfaceProvider() {
//        return new Preview.PreviewSurfaceProvider() {
//            @Override
//            public Surface getSurface() {
//                return mSurface;
//            }
//
//            @Override
//            public void onPreviewSurfaceRequested(Surface surface, int i, int i1) {}
//        };
//    }
//}