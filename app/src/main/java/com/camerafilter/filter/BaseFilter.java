package com.camerafilter.filter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class BaseFilter {
    protected final float[] VERTEX_C00RD = {
            -1.0f, 1.0f, // 左上
            -1.0f, -1.0f, // 左下
            1.0f, -1.0f, // 右下
            1.0f, 1.0f // 右上
    };

    protected final float[] TEXTURE_C00RD = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };

    private final short[] INDICES={0,1,2,0,2,3};

    protected FloatBuffer vertexBuffer;
    protected FloatBuffer textureBuffer;
    protected ShortBuffer indexBuffer;

    public BaseFilter() {
        vertexBuffer=ByteBuffer.allocateDirect(VERTEX_C00RD.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_C00RD);
        vertexBuffer.position(0);

        textureBuffer=ByteBuffer.allocateDirect(TEXTURE_C00RD.length*4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(TEXTURE_C00RD);
        textureBuffer.position(0);

        indexBuffer=ByteBuffer.allocateDirect(INDICES.length*4)
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
