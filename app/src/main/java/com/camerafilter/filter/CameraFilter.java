package com.camerafilter.filter;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.camerafilter.utils.ShaderUtil;

public class CameraFilter extends BaseFilter {

    private int program;
    private int aPosition;
    private int aTextureCoord;
    private int uTexture;

    private int uMatrix; // 矩阵变换统一入口

    // 最终变换矩阵：解决旋转 + 比例 + 镜像
    private final float[] mvpMatrix = new float[16];

    // 相机与显示比例，用于防拉伸
    private int cameraWidth  = 1280;
    private int cameraHeight = 720;
    private int surfaceWidth = 1080;
    private int surfaceHeight = 2340;

    // 相机方向与镜像
    private int cameraRotation = 90;
    private boolean isFrontCamera = false;

    private final String vertexShader =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform mat4 uMatrix;\n" +
                    "void main(){\n" +
                    "    gl_Position = aPosition * uMatrix;\n" +
                    "    vTextureCoord = aTextureCoord;\n"  +
                    "}";

    private final String fragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES uTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(uTexture, vTextureCoord);\n" +
                    "}";

    public CameraFilter() {
        program = ShaderUtil.createProgram(vertexShader, fragmentShader);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureCoord = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uTexture = GLES20.glGetUniformLocation(program, "uTexture");
        uMatrix = GLES20.glGetUniformLocation(program, "uMatrix");
        // 初始化为单位矩阵
        Matrix.setIdentityM(mvpMatrix, 0);
    }

    /**
     * 设置相机分辨率 和 显示分辨率（自动防拉伸）
     */
    public void setCameraSize(int camW, int camH, int surfaceW, int surfaceH) {
        this.cameraWidth = camW;
        this.cameraHeight = camH;
        this.surfaceWidth = surfaceW;
        this.surfaceHeight = surfaceH;
        updateMatrix();
    }

    /**
     * 设置相机旋转（通常90度）、是否前置（自动镜像）
     */
    public void setCameraOrientation(int rotation, boolean frontCamera) {
        this.cameraRotation = rotation;
        this.isFrontCamera = frontCamera;
        updateMatrix();
    }

    /**
     * 核心：统一构建变换矩阵（防拉伸 + 旋转 + 镜像）
     */
    private void updateMatrix() {
        Matrix.setIdentityM(mvpMatrix, 0);

        // 1. 旋转：修正相机方向（通常90度）
        Matrix.rotateM(mvpMatrix, 0, cameraRotation, 0, 0, 1);

        // 2. 前置相机水平镜像
        if (isFrontCamera) {
            Matrix.scaleM(mvpMatrix, 0, -1, 1, 1);
        }

        // 3. 比例校正，彻底防止拉伸
        float camRatio = cameraWidth  / (float) cameraHeight;
        float surfaceRatio = surfaceWidth  / (float) surfaceHeight;

        if (camRatio > surfaceRatio) {
            // 相机更宽 → 上下缩
            float scale = surfaceRatio / camRatio;
            Matrix.scaleM(mvpMatrix, 0, 1, scale, 1);
        } else {
            // 相机更高 → 左右缩
            float scale = camRatio / surfaceRatio;
            Matrix.scaleM(mvpMatrix, 0, scale, 1, 1);
        }
    }
    @Override
    public int onDraw(int textureId) {
        if (width > 0 && height > 0 && fboTextureId[0] <= 0) {
            createFbo(width, height);
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);

        GLES20.glUseProgram(program);
        // ================= 加入这一行，解决方向 + 拉伸 =================
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, mvpMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(uTexture, 0);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return getFboTextureId();
    }
}