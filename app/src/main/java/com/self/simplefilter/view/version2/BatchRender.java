package com.self.simplefilter.view.version2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class BatchRender implements GLSurfaceView.Renderer {

    // ========================== 批渲染核心：一个缓冲区存所有形状 ==========================
    // 顺序：梯形(4点) → 矩形(4点) → 三角形(3点)
    private final float[] ALL_VERTEX = {
            // 1. 梯形 (4点) TRIANGLE_STRIP
            -0.7f, -0.3f,
            -0.4f,  0.3f,
            0.7f, -0.3f,
            0.4f,  0.3f,

            // 2. 矩形 (4点) TRIANGLE_STRIP
            -0.3f, -0.7f,
            -0.3f, -0.4f,
            0.3f, -0.7f,
            0.3f, -0.4f,

            // 3. 三角形 (3点) TRIANGLES
            -0.2f,  0.5f,
            0.2f,  0.5f,
            0.0f,  0.7f
    };

    // 每个图形的【起始索引】和【顶点数量】
    private static final int[] START = { 0, 4, 8 };
    private static final int[] COUNT = { 4, 4, 3 };
    private static final int[] MODE  = {
            GLES20.GL_TRIANGLE_STRIP,
            GLES20.GL_TRIANGLE_STRIP,
            GLES20.GL_TRIANGLES
    };

    // ========================== 通用渲染 ==========================
    private int mProgram;
    private int vPositionHandle;
    private int uMatrixHandle;
    private FloatBuffer vertexBuffer;
    private float[] mProjectionMatrix = new float[16];

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        // 一个缓冲区装所有顶点
        vertexBuffer = ByteBuffer.allocateDirect(ALL_VERTEX.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(ALL_VERTEX);
        vertexBuffer.position(0);

        String vertexShader =
                "attribute vec4 vPosition;\n"
                        + "uniform mat4 uMatrix;\n"
                        + "void main() {\n"
                        + "    gl_Position = uMatrix * vPosition;\n"
                        + "}";

        String fragmentShader =
                "precision mediump float;\n"
                        + "void main() {\n"
                        + "    gl_FragColor = vec4(1.0, 0.2, 0.2, 1.0);\n"
                        + "}";

        mProgram = OpenGLUtil.createProgram(vertexShader, fragmentShader);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uMatrixHandle   = GLES20.glGetUniformLocation(mProgram, "uMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;

        // 安全正交矩阵：左右不超屏
        Matrix.orthoM(mProjectionMatrix, 0,
                -1.0f, 1.0f,
                -1.0f / ratio, 1.0f / ratio,
                -1.0f, 1.0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        // ====================== 只绑定一次 ======================
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mProjectionMatrix, 0);
        GLES20.glVertexAttribPointer(vPositionHandle, 2,
                GLES20.GL_FLOAT, false, 8, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);

        // ====================== 多次绘制：批渲染核心 ======================
        for (int i = 0; i < 3; i++) {
            GLES20.glDrawArrays(MODE[i], START[i], COUNT[i]);
        }
    }
}