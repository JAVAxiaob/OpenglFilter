package com.camerafilter.filter;

import android.opengl.GLES20;
import com.camerafilter.utils.ShaderUtil;

public class BeautyFilter extends BaseFilter {

    private int program;
    private int aPosition;
    private int aTextureCoord;
    private int uTexture;
    private int uTextureWidth;
    private int uTextureHeight;

    private final String vertexShader =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(){\n" +
                    "    gl_Position = aPosition;\n" +
                    "    vTextureCoord = aTextureCoord;\n" +
                    "}";

    // 完全正确的美颜片元着色器（无OES，不报错）
    private final String fragmentShader =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D uTexture;\n" +
                    "uniform float uTextureWidth;\n" +
                    "uniform float uTextureHeight;\n" +

                    "void main(){\n" +
                    "    vec2 uv = vTextureCoord;\n" +
                    "    vec4 color = texture2D(uTexture, uv);\n" +

                    "    float dx = 1.0 / uTextureWidth;\n" +
                    "    float dy = 1.0 / uTextureHeight;\n" +

                    "    vec3 sum = vec3(0.0);\n" +
                    "    float totalWeight = 0.0;\n" +

                    "    for(float i=-1.0; i<=1.0; i++){\n" +
                    "        for(float j=-1.0; j<=1.0; j++){\n" +
                    "            vec4 c = texture2D(uTexture, uv + vec2(i*dx, j*dy));\n" +
                    "            sum += c.rgb;\n" +
                    "            totalWeight += 1.0;\n" +
                    "        }\n" +
                    "    }\n" +

                    "    vec3 res = sum / totalWeight;\n" +
                    "    gl_FragColor = vec4(res, 1.0);\n" +
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
        // ✅【关键】必须解绑 FBO，直接画到屏幕！！！
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glViewport(0, 0, width, height);
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTexture, 0);

        GLES20.glUniform1f(uTextureWidth, 640f);
        GLES20.glUniform1f(uTextureHeight, 480f);

        // 坐标用 8 位对齐，你之前是 0 → 错误！
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureId;
    }
    public void setWidth(int width) {
        this.width = width;
    }


    public void setHeight(int height) {
        this.height = height;
    }
}