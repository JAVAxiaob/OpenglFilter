package com.camerafilter.filter;

import android.opengl.GLES20;
import android.util.Log;
import com.camerafilter.utils.ShaderUtil;

public class BeautyFilterTest extends BaseFilter {

    private static final String TAG = "BeautyFilterTest";

    private int program;
    private int aPosition;
    private int aTextureCoord;
    private int uTexture;

    private final String vertexShader =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTextureCoord = aTextureCoord;\n" +
                    "}";

    // 简单输出纹理颜色，也可替换成 vec4(1.0,0.0,0.0,1.0) 测试屏幕是否正常
    private final String fragmentShader =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(uTexture, vTextureCoord);\n" +
                    // "gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);" // 测试用
                    "}";

    public BeautyFilterTest() {
        program = ShaderUtil.createProgram(vertexShader, fragmentShader);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureCoord = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uTexture = GLES20.glGetUniformLocation(program, "uTexture");
    }

    public int onDraw(int fboTextureId, int fboWidth, int fboHeight) {
        Log.d(TAG, "onDraw called: fboTextureId=" + fboTextureId + ", width=" + fboWidth + ", height=" + fboHeight);

        // 绘制到屏幕
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, fboWidth, fboHeight);
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId);
        GLES20.glUniform1i(uTexture, 0);

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        textureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        indexBuffer.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glDisableVertexAttribArray(aPosition);
        GLES20.glDisableVertexAttribArray(aTextureCoord);

        Log.d(TAG, "onDraw finished");

        return fboTextureId;
    }
}