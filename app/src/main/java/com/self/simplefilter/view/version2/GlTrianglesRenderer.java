package com.self.simplefilter.view.version2;

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

public class GlTrianglesRenderer implements GLSurfaceView.Renderer {
    int mProgram;
    int vPositionHandle;
    int uRatioHandle;
    FloatBuffer vertexBuffer;
    private float screenRatio;

    final float R_OUTER = 0.6f;  // 外半径（尖角）
    final float R_INNER = 0.25f; // 内半径（凹点）
    float[] VERTEX_DATA;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initStarData(); // 生成正确五角星顶点
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTEX_DATA);
        vertexBuffer.position(0);

        // 顶点着色器：比例校正，旋转不变形
        String vertexShaderCode = "attribute vec4 vPosition;\n"
                + "uniform float uRatio;\n"
                + "void main() {\n"
                + "    gl_Position = vec4(vPosition.x / uRatio, vPosition.y, 0.0, 1.0);\n"
                + "}";

        String fragmentShaderCode = "precision mediump float;\n"
                + "void main() {\n"
                + "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
                + "}";

        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uRatioHandle = GLES20.glGetUniformLocation(mProgram, "uRatio");
    }

    // ✅ 核心：生成完美五角星顶点（扇形专用）
    private void initStarData() {
        List<Float> vertexList = new ArrayList<>();
        // 第一个点：中心点（扇形必须以中心为起点）
        vertexList.add(0.0f);
        vertexList.add(0.0f);

        // 5个尖角，每个尖角对应1个外点+1个内点，共10个外围点
        for (int i = 0; i < 5; i++) {
            // 外点（尖角）角度：0°, 72°, 144°, 216°, 288°
            float angleOuter = (float) (Math.PI * 2 * i / 5 - Math.PI / 2);
            float xOuter = (float) (R_OUTER * Math.cos(angleOuter));
            float yOuter = (float) (R_OUTER * Math.sin(angleOuter));
            vertexList.add(xOuter);
            vertexList.add(yOuter);

            // 内点（凹点）角度：36°, 108°, 180°, 252°, 324°
            float angleInner = (float) (Math.PI * 2 * (i + 0.5f) / 5 - Math.PI / 2);
            float xInner = (float) (R_INNER * Math.cos(angleInner));
            float yInner = (float) (R_INNER * Math.sin(angleInner));
            vertexList.add(xInner);
            vertexList.add(yInner);
        }

        // 最后补一个外点，闭合扇形
        float angleLast = (float) (Math.PI * 2 * 0 / 5 - Math.PI / 2);
        float xLast = (float) (R_OUTER * Math.cos(angleLast));
        float yLast = (float) (R_OUTER * Math.sin(angleLast));
        vertexList.add(xLast);
        vertexList.add(yLast);

        VERTEX_DATA = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            VERTEX_DATA[i] = vertexList.get(i);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        screenRatio = (float) width / height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);

        // 传递最新比例，旋转不变形
        GLES20.glUniform1f(uRatioHandle, screenRatio);

        GLES20.glVertexAttribPointer(vPositionHandle, 2,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);

        // ✅ 扇形绘制，完美闭合，无镂空
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_DATA.length / 2);
    }
}