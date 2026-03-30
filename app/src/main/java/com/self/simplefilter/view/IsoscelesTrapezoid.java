package com.self.simplefilter.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class IsoscelesTrapezoid implements GLSurfaceView.Renderer {
    int mProgram;
    int vPositionHandle;

    FloatBuffer vertexBuffer;

    private float[] mProjectionMatrix = new float[16]; // 正交投影矩阵

    float[] VERTEX_DATA = {
            -0.7f, -0.4f,
            -0.4f, 0.4f,
            0.7f, -0.4f,
            0.4f, 0.4f
    };
    private int uMatrixHandle;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTEX_DATA);
        vertexBuffer.position(0);

        // 【核心：顶点着色器用矩阵相乘，替代手动x/uRatio】
        String vertexShaderCode = "attribute vec4 vPosition;\n"
                + "uniform mat4 uMatrix;\n"
                + "void main() {\n"
                + "    gl_Position = uMatrix * vPosition;\n"
                + "}";

        String fragmentShaderCode = "precision mediump float;\n"
                + "void main() {\n"
                + "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
                + "}";
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // 【核心：生成正交投影矩阵，自动适配宽高】
        float ratio = (float) width / height;
        // 正交矩阵：left, right, bottom, top, near, far
        //orthoM(
        //    输出矩阵,
        //    偏移,
        //    left, right,    // X 轴
        //    bottom, top,    // Y 轴
        //    near, far       // Z 轴！！！！
        //)
        Matrix.orthoM(
                mProjectionMatrix, 0,
                -1.0f, 1.0f,            // X：固定 -1 ~ 1
                -1.0f / ratio, 1.0f / ratio, // Y：自适应
                -1.0f, 1.0f             // Z：固定不动
        );
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mProjectionMatrix, 0);
        GLES20.glVertexAttribPointer(vPositionHandle,
                2, GLES20.GL_FLOAT, false,
                0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_DATA.length / 2);
    }
}
