package com.camerafilter.filter;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BaseFilter {

    protected final float[] VERTEX_COORD = {
            -1.0f,  1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f,  1.0f
    };

    protected final float[] TEXTURE_COORD = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
//?sdsd
    private final short[] INDICES = {0, 1, 2, 0, 2, 3};

    protected FloatBuffer vertexBuffer;
    protected FloatBuffer textureBuffer;
    protected ShortBuffer indexBuffer;

    // ---------------- FBO 相关 ----------------
    protected int[] fboId = new int[1];
    protected int[] fboTextureId = new int[1];
    protected int width, height;

    public BaseFilter() {
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_COORD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_COORD);
        vertexBuffer.position(0);

        textureBuffer = ByteBuffer.allocateDirect(TEXTURE_COORD.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEXTURE_COORD);
        textureBuffer.position(0);

        indexBuffer = ByteBuffer.allocateDirect(INDICES.length * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(INDICES);
        indexBuffer.position(0);
    }

    // 创建 FBO
    public void createFbo(int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glGenFramebuffers(1, fboId, 0);
        GLES20.glGenTextures(1, fboTextureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId[0]);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                fboTextureId[0],
                0);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public int getFboTextureId() {
        return fboTextureId[0];
    }

    public int onDraw(int textureId) {
        return textureId;
    }

    public void release() {}
}