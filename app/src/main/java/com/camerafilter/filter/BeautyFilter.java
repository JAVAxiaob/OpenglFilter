package com.camerafilter.filter;

import android.opengl.GLES20;

import com.camerafilter.utils.ShaderUtil;

import javax.microedition.khronos.opengles.GL;

public class BeautyFilter extends BaseFilter {
    private int program;

    private int aPosition;
    private int aTextureCoord;
    private int uTexture;
    private int uWidth;
    private int uHeight;

    public float beautyLeverl = 0.5f;

    private final String vertexShader =
            "attribute vec4 aPosition;\n" +
                    "attribute vec2 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main(){" +
                    "gl_Position = aPosition;\n" +
                    "vTextureCoord = aTextureCoord;\n" +
                    "}";
    private final String fragmentShader =
            "precision mediump float;\n"          +
                    "varying vec2 vTextureCoord;\n"       +
                    "uniform sampler2D uTexture;\n"       +
                    "uniform float uWidth;\n"             +
                    "uniform float uHeight;\n"            +

                    "void main() {\n"                    +
                    "    vec2 uv = vTextureCoord;\n"     +
                    "    vec4 color = texture2D(uTexture, uv);\n"+

                    // 采样偏移
                    "    float offset = 1.0 / max(uWidth, uHeight);\n"+
                    "    vec3 sum = vec3(0.0);\n"       +
                    "    float totalWeight = 0.0;\n"    +

                    // 5x5 双边滤波磨皮
                    "    for(float i=-2.0; i<=2.0; i++){\n"+
                    "        for(float j=-2.0; j<=2.0; j++){\n"+
                    "            vec4 c = texture2D(uTexture, uv + vec2(i,j)*offset);\n"+
                    "            float w = 1.0 - distance(c.rgb, color.rgb);\n"+
                    "            w *= w;\n"+
                    "            sum += c.rgb * w;\n"+
                    "            totalWeight += w;\n"+
                    "        }\n"+
                    "    }\n"+

                    "    vec3 skin = sum / totalWeight;\n"+
                    "    vec3 res = mix(color.rgb, skin, 0.5);\n"+
                    "    res = mix(res, vec3(1.0), 0.1);\n"+ // 美白
                    "    gl_FragColor = vec4(res, color.a);\n"+
                    "}";

    public BeautyFilter() {
        program = ShaderUtil.createProgram(vertexShader,fragmentShader);
        aPosition=GLES20.glGetAttribLocation(program,"aPosition");
        aTextureCoord=GLES20.glGetAttribLocation(program,"aTextureCoord");
        uWidth = GLES20.glGetAttribLocation(program,"uWidth");
        uHeight = GLES20.glGetAttribLocation(program,"uHeight");
    }

    @Override
    public int onDraw(int textureId) {
        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId);
        GLES20.glUniform1i(uTexture,0);

        GLES20.glUniform1f(uWidth,1080f);
        GLES20.glUniform1f(uHeight,2340f);

        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition,2,GLES20.GL_FLOAT,false,0,vertexBuffer);

        GLES20.glEnableVertexAttribArray(aTextureCoord);
        GLES20.glVertexAttribPointer(aTextureCoord,2,GLES20.GL_FLOAT,false,0,textureBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES,6,GLES20.GL_UNSIGNED_SHORT,indexBuffer);

        return textureId;
    }

    @Override
    public void release() {
        super.release();
    }
}
