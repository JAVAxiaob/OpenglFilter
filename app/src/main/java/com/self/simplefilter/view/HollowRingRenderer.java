package com.self.simplefilter.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Size;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class HollowRingRenderer implements GLSurfaceView.Renderer {
    int mProgram;
    int vPositionHandle;

    FloatBuffer vertexBuffer;

    float[] VERTEX_DATA ;

    float[] mProjectionMatrix = new float[16]; // 正交投影矩阵
    private int uMatrixHandle;

    int SEGMENT = 32;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initData();
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTEX_DATA);
        vertexBuffer.position(0);

        String vertexShaderCode = "attribute vec4 vPosition;\n"
                + "uniform mat4 uMatrix;\n"
                + "void main() {\n"
                + "    gl_Position = uMatrix * vPosition;\n"
                +"}";
        String fragmentShaderCode = "precision mediump float;\n"
                + "void main() {\n"
                + "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n"
                + "}";
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMatrix");
    }

    private void initData() {
        List<Float> list = new ArrayList<>();
        float innerR = 0.35f;
        float outerR = 0.8f;
        for(int i=0;i<=SEGMENT;i++){
            float angle = (float)(Math.PI * 2 * i / SEGMENT);
            float cx = (float)Math.cos(angle);
            float cy = (float)Math.sin(angle);
            //外点、内点交替
            list.add(cx * outerR); list.add(cy * outerR);
            list.add(cx * innerR); list.add(cy * innerR);
        }
        VERTEX_DATA = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            VERTEX_DATA[i] = list.get(i);
        }
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

        // 核心修复：计算宽高比，固定短边为 1，拉伸长边以保持正圆
        float aspectRatio = (float) width / height;

        // 逻辑：
        // 如果屏幕宽 > 高 (横屏)：X 轴范围扩大，Y 轴固定 [-1, 1]
        // 如果屏幕宽 < 高 (竖屏)：Y 轴范围扩大，X 轴固定 [-1, 1]
        if (aspectRatio >= 1.0f) {
            // 横屏：左右拉伸
            Matrix.orthoM(mProjectionMatrix, 0,
                    -aspectRatio, aspectRatio,  // X 轴范围随宽度变化
                    -1.0f, 1.0f,                 // Y 轴固定
                    -1.0f, 1.0f);
        } else {
            // 竖屏：上下拉伸 (你的代码原本就正常，这里保留逻辑)
            Matrix.orthoM(mProjectionMatrix, 0,
                    -1.0f, 1.0f,
                    -1 / aspectRatio, 1 / aspectRatio,
                    -1.0f, 1.0f);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, mProjectionMatrix, 0);
        GLES20.glVertexAttribPointer(
                vPositionHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexBuffer
        );
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        //
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_DATA.length / 2);
    }
}
