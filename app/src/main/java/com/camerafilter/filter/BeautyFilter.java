package com.camerafilter.filter;

import android.opengl.GLES20;
import android.opengl.GLES11Ext;

import com.camerafilter.utils.ShaderUtil;

public class BeautyFilter extends BaseFilter {

    private int program;
    private int aPosition;
    private int aTextureCoord;
    private int uTexture;

    private int uTextureWidth;
    private int uTextureHeight;

    // 美颜强度
    public float beautyLevel = 0.5f;

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
                    "uniform float uTextureWidth;\n" +
                    "uniform float uTextureHeight;\n" +

                    "void main(){\n" +
                    "    vec2 uv = vTextureCoord;\n" +
                    "    vec4 color = texture2D(uTexture, uv);\n" +

                    "    float offsetX = 1.0 / uTextureWidth;\n" +
                    "    float offsetY = 1.0 / uTextureHeight;\n" +
                    "    vec3 sum = vec3(0.0);\n" +
                    "    float totalWeight = 0.0;\n" +

                    "    for(float i=-2.0;i<=2.0;i++){\n" +
                    "        for(float j=-2.0;j<=2.0;j++){\n" +
                    "            vec4 c = texture2D(uTexture, uv + vec2(i*offsetX, j*offsetY));\n" +
                    "            float w = 1.0 - distance(c.rgb, color.rgb);\n" +
                    "            w *= w;\n" +
                    "            sum += c.rgb * w;\n" +
                    "            totalWeight += w;\n" +
                    "        }\n" +
                    "    }\n" +

                    "    vec3 skin = sum / totalWeight;\n" +
                    "    vec3 res = mix(color.rgb, skin, 0.5);\n" +
                    "    gl_FragColor = vec4(res, color.a);\n" +
                    "}";

    public BeautyFilter() {
        program = ShaderUtil.createProgram(vertexShader, fragmentShader);

        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aTextureCoord = GLES20.glGetAttribLocation(program, "aTextureCoord");
        uTexture = GLES20.glGetUniformLocation(program, "uTexture");
        uTextureWidth = GLES20.glGetUniformLocation(program, "uTextureWidth");
        uTextureHeight = GLES20.glGetUniformLocation(program, "uTextureHeight");
    }

    @Override
    public int onDraw(int textureId) {
        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(uTexture, 0);

        // 传入纹理宽高，保证采样正确
        GLES20.glUniform1f(uTextureWidth, 640f);  // 可改成实际分辨率
        GLES20.glUniform1f(uTextureHeight, 480f);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        return textureId;
    }
}