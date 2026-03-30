package com.self.simplefilter.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

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
    private float[] VERTEX_DATA;
    FloatBuffer vFloatBuffer;

    int uRatioHandle;
    int uTimeHandle;

    int SEGMENT = 60; // 波浪用更多段
    float ratio;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initCircleFanData(); // ←--- 换成扇形顶点

        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        vFloatBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(VERTEX_DATA);
        vFloatBuffer.position(0);

        // 【波动着色器】真正会动的版本
        String vertexShaderCode = "attribute vec4 vPosition;\n" +
                "uniform float uRatio;\n" +
                "uniform float uTime;\n" +
                "void main(){\n" +
                "    float x = vPosition.x;\n" +
                "    float y = vPosition.y;\n" +

                // 计算角度 → 让每个点波动相位不一样
                "    float angle = atan(y, x);\n" +
                "    float wave = sin(uTime * 5.0 + angle * 6.0) * 0.15;\n" +
                "    float finalR = 0.8 + wave;\n" +

                // 应用波动半径
                "    x = cos(angle) * finalR;\n" +
                "    y = sin(angle) * finalR;\n" +

                "    gl_Position = vec4(x / uRatio, y, 0.0, 1.0);\n" +
                "}";

        String fragmentShaderCode = "precision mediump float;\n"
                + "void main() {\n"
                + "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
                + "}";

        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uRatioHandle = GLES20.glGetUniformLocation(mProgram, "uRatio");
        uTimeHandle = GLES20.glGetUniformLocation(mProgram, "uTime");
    }

    // ==============================================
    // 【关键】画波动圆必须用 圆心+外圈 TRIANGLE_FAN 模式
    // ==============================================
    private void initCircleFanData() {
        List<Float> list = new ArrayList<>();

        // 1. 圆心点
        list.add(0f);
        list.add(0f);

        // 2. 一圈外圈点
        float r = 1f;
        for (int i = 0; i <= SEGMENT; i++) {
            double angle = Math.PI * 2 * i / SEGMENT;
            float x = (float) (Math.cos(angle));
            float y = (float) (Math.sin(angle));
            list.add(x);
            list.add(y);
        }

        VERTEX_DATA = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            VERTEX_DATA[i] = list.get(i);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        ratio = (float) width / height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 时间流动
        float time = (System.currentTimeMillis() % 10000) / 1000f;

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        // 传参数
        GLES20.glUniform1f(uRatioHandle, ratio);
        GLES20.glUniform1f(uTimeHandle, time);

        // 顶点
        GLES20.glVertexAttribPointer(vPositionHandle,
                2, GLES20.GL_FLOAT, false,
                0, vFloatBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);

        // ←--- 必须用 TRIANGLE_FAN
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_DATA.length / 2);
    }
}