package com.self.simplefilter.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class WaveTwistedCircle implements GLSurfaceView.Renderer {
    int mProgram;

    int vPositionHandle;
    int uMatrixHandle;   // 正交矩阵
    int uTimeHandle;     // 时间

    private float[] VERTEX_DATA;
    FloatBuffer vFloatBuffer;

    float[] mProjectionMatrix = new float[16]; // 正交矩阵
    int SEGMENT = 64;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initRingData(); // 空心圆环顶点

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        vFloatBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_DATA);
        vFloatBuffer.position(0);

        // ========================
        // 着色器：波动 + 正交矩阵
        // ========================
        String vertexShaderCode =
                "attribute vec4 vPosition;\n"
                        + "uniform mat4 uMatrix;\n"
                        + "uniform float uTime;\n"
                        + "void main(){\n"

                        // 取原始坐标
                        + "    float x = vPosition.x;\n"
                        + "    float y = vPosition.y;\n"

                        // 计算角度 → 每个点波浪不一样
                        + "    float angle = atan(y, x);\n"

                        // 波动公式：时间+角度 → 产生跳动
                        + "    float wave = sin(uTime * 4.0 + angle * 8.0) * 0.1;\n"

                        // 让半径随波浪变化
                        + "    float scale = 1.0 + wave;\n"
                        + "    x *= scale;\n"
                        + "y *= scale;\n"

                        // 应用正交矩阵
                        + "    gl_Position = uMatrix * vec4(x, y, 0.0, 1.0);\n"
                        + "}";

        String fragmentShaderCode =
                "precision mediump float;\n"
                        + "void main() {\n"
                        + "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
                        + "}";

        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMatrix");
        uTimeHandle = GLES20.glGetUniformLocation(mProgram, "uTime");
    }

    // ==============================
    // 空心圆环顶点（TRIANGLE_STRIP）
    // ==============================
    private void initRingData() {
        List<Float> list = new ArrayList<>();
        float outerR = 0.8f;
        float innerR = 0.4f;

        for (int i = 0; i <= SEGMENT; i++) {
            float angle = (float) (Math.PI * 2 * i / SEGMENT);
            float cx = (float) Math.cos(angle);
            float cy = (float) Math.sin(angle);

            // 外点
            list.add(cx * outerR);
            list.add(cy * outerR);
            // 内点
            list.add(cx * innerR);
            list.add(cy * innerR);
        }

        VERTEX_DATA = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            VERTEX_DATA[i] = list.get(i);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // ========================
        // 正交投影矩阵（你要的）
        // ========================
        if (width > height) {
            // 横屏
            Matrix.orthoM(mProjectionMatrix, 0,
                    -ratio, ratio,
                    -1, 1,
                    -1, 1);
        } else {
            // 竖屏
            Matrix.orthoM(mProjectionMatrix, 0,
                    -1, 1,
                    -1 / ratio, 1 / ratio,
                    -1, 1);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        float time = (System.currentTimeMillis() % 10000) / 1000f;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        // 传正交矩阵
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mProjectionMatrix, 0);
        // 传时间
        GLES20.glUniform1f(uTimeHandle, time);

        // 顶点
        GLES20.glVertexAttribPointer(vPositionHandle,
                2, GLES20.GL_FLOAT, false,
                0, vFloatBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);

        // 空心圆环必须用 STRIP
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_DATA.length / 2);
    }
}