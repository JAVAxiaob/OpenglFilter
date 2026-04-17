package com.camerafilter.filter;

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

    private final short[] INDICES = {0, 1, 2, 0, 2, 3};

    protected FloatBuffer vertexBuffer;
    protected FloatBuffer textureBuffer;
    protected ShortBuffer indexBuffer;

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

    public int onDraw(int textureId) {
        return textureId;
    }

    public void release() {}
}