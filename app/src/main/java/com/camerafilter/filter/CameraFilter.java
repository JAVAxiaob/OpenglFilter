package com.camerafilter.filter;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import android.opengl.GLES20;

import com.camerafilter.utils.ShaderUtil;

public class CameraFilter extends BaseFilter {

    private int program;
    private int aPosition;
    private int aTextureCoord;
    private int uTexture;

    private final String vertexShader =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(){\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTextureCoord = aTextureCoord;\n" +
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
    }

    @Override
    public int onDraw(int textureId) {
        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(uTexture, 0);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);

        return textureId;
    }
}