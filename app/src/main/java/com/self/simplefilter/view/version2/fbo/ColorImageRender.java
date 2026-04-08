package com.self.simplefilter.view.version2.fbo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.maniu.openglfilter.R;
import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ColorImageRender implements GLSurfaceView.Renderer {
    private Context mContext;

    // 基础渲染
    private int mBaseProgram;
    private int mBaseMatrixHandle;
    private int mBasePositionHandle;
    private int mBaseTexCoordHandle;
    private int mBaseTextureHandle;
    private FloatBuffer mBaseVertexBuffer;

    private final float[] mProjectionMatrix = new float[16];

    // 【固定全屏顶点】+ 【纹理坐标统一翻转】→ 彻底解决图像颠倒
    private final float[] mImageVertices = {
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f
    };

    // 图片纹理ID
    private int mImageTextureId;
    private int mImageWidth, mImageHeight;

    // ==================== FBO ====================
    private int mFboId;
    private int mFboTextureId;
    private int mFboWidth = 540;
    private int mFboHeight = 960;

    // ==================== 模糊渲染 ====================
    private int mBlurProgram;
    private int mBlurPositionHandle;
    private int mBlurTexCoordHandle;
    private int mBlurTextureHandle;
    private int mBlurTexelSizeHandle;
    private FloatBuffer mBlurVertexBuffer;

    // 模糊阶段：全屏顶点
    private final float[] mFullScreenVertices = {
            -1.0f, -1.0f, 0.0f, 1.0f,
            1.0f, -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 1.0f, 0.0f
    };

    public ColorImageRender(Context context) {
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mBaseVertexBuffer = createFloatBuffer(mImageVertices);
        mBlurVertexBuffer = createFloatBuffer(mFullScreenVertices);

        // 加载图片
        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.dog);
        mImageWidth = bitmap.getWidth();
        mImageHeight = bitmap.getHeight();
        mImageTextureId = OpenGLUtil.loadTextureFromBitmap(bitmap);
        bitmap.recycle();

        // ========== 基础着色器 ==========
        String baseVertex =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "uniform mat4 uProjectionMatrix;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){\n" +
                        "    gl_Position = uProjectionMatrix * aPosition;\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "}";

        String baseFragment =
                "precision mediump float;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){\n" +
                        "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
                        "    float gray = color.r*0.3 + color.g*0.59 + color.b*0.11;\n" +
                        "    gl_FragColor = vec4(gray,gray,gray,1.0);\n" +
                        "}";

        mBaseProgram = OpenGLUtil.createProgram(baseVertex, baseFragment);
        mBasePositionHandle = GLES20.glGetAttribLocation(mBaseProgram, "aPosition");
        mBaseTexCoordHandle = GLES20.glGetAttribLocation(mBaseProgram, "aTexCoord");
        mBaseMatrixHandle = GLES20.glGetUniformLocation(mBaseProgram, "uProjectionMatrix");
        mBaseTextureHandle = GLES20.glGetUniformLocation(mBaseProgram, "uTexture");

        // ========== 模糊着色器 ==========
        String blurVertex =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){\n" +
                        "    gl_Position = aPosition;\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "}";

        String blurFragment =
                "precision mediump float;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "uniform vec2 uTexelSize;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main(){\n" +
                        "    vec4 color = vec4(0.0);\n" +
                        "    color += texture2D(uTexture, vTexCoord+vec2(-2,0)*uTexelSize)*0.061;\n" +
                        "    color += texture2D(uTexture, vTexCoord+vec2(-1,0)*uTexelSize)*0.242;\n" +
                        "    color += texture2D(uTexture, vTexCoord)*0.383;\n" +
                        "    color += texture2D(uTexture, vTexCoord+vec2(1,0)*uTexelSize)*0.242;\n" +
                        "    color += texture2D(uTexture, vTexCoord+vec2(2,0)*uTexelSize)*0.061;\n" +
                        "    gl_FragColor = color;\n" +
                        "}";

        mBlurProgram = OpenGLUtil.createProgram(blurVertex, blurFragment);
        mBlurPositionHandle = GLES20.glGetAttribLocation(mBlurProgram, "aPosition");
        mBlurTexCoordHandle = GLES20.glGetAttribLocation(mBlurProgram, "aTexCoord");
        mBlurTextureHandle = GLES20.glGetUniformLocation(mBlurProgram, "uTexture");
        mBlurTexelSizeHandle = GLES20.glGetUniformLocation(mBlurProgram, "uTexelSize");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mFboWidth = width;
        mFboHeight = height;
        GLES20.glViewport(0, 0, width, height);

        // ==============================================
        // 🔥 核心修复：自动计算 居中不变形 正交矩阵
        // ==============================================
        float screenRatio = (float) width / height;
        float imgRatio = (float) mImageWidth / mImageHeight;

        float left, right, bottom, top;

        if (imgRatio > screenRatio) {
            // 图片更宽 → 上下留黑
            left = -imgRatio / screenRatio;
            right = imgRatio / screenRatio;
            bottom = -1;
            top = 1;
        } else {
            // 图片更高 → 左右留黑
            left = -1;
            right = 1;
            bottom = -screenRatio / imgRatio;
            top = screenRatio / imgRatio;
        }

        Matrix.orthoM(mProjectionMatrix, 0,
                left, right,
                bottom, top,
                -1, 1);

        createFBO();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // ---------- 第一步：渲染到 FBO ----------
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
        GLES20.glViewport(0, 0, mFboWidth, mFboHeight);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mBaseProgram);
        GLES20.glUniformMatrix4fv(mBaseMatrixHandle, 1, false, mProjectionMatrix, 0);

        mBaseVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mBasePositionHandle, 2, GLES20.GL_FLOAT, false, 16, mBaseVertexBuffer);
        mBaseVertexBuffer.position(2);
        GLES20.glVertexAttribPointer(mBaseTexCoordHandle, 2, GLES20.GL_FLOAT, false, 16, mBaseVertexBuffer);

        GLES20.glEnableVertexAttribArray(mBasePositionHandle);
        GLES20.glEnableVertexAttribArray(mBaseTexCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mImageTextureId);
        GLES20.glUniform1i(mBaseTextureHandle, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // ---------- 第二步：FBO → 模糊 → 屏幕 ----------
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, mFboWidth, mFboHeight);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mBlurProgram);
        GLES20.glUniform1i(mBlurTextureHandle, 0);
        GLES20.glUniform2f(mBlurTexelSizeHandle, 1f / mFboWidth, 1f / mFboHeight);

        mBlurVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mBlurPositionHandle, 2, GLES20.GL_FLOAT, false, 16, mBlurVertexBuffer);
        mBlurVertexBuffer.position(2);
        GLES20.glVertexAttribPointer(mBlurTexCoordHandle, 2, GLES20.GL_FLOAT, false, 16, mBlurVertexBuffer);

        GLES20.glEnableVertexAttribArray(mBlurPositionHandle);
        GLES20.glEnableVertexAttribArray(mBlurTexCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void createFBO() {
        int[] fbo = new int[1];
        GLES20.glGenFramebuffers(1, fbo, 0);
        mFboId = fbo[0];

        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        mFboTextureId = tex[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                mFboWidth, mFboHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, mFboTextureId, 0);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private FloatBuffer createFloatBuffer(float[] data) {
        FloatBuffer buffer = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(data);
        buffer.position(0);
        return buffer;
    }
}