package com.self.simplefilter.view;

import android.media.DrmInitData;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//x = cos(角度) * 半径
//y = sin(角度) * 半径
public class GlTriangleFanRenderer implements GLSurfaceView.Renderer {
    int mProgram;
    int vPositionHandle;
    FloatBuffer vertexBuffer;
    final int SEGMENT = 360;
    final float RADIUS = 0.8f;

    float[] VERTEX_DATA;
    private int uRatioHandle;
    private float screenRatio;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initData();
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4) //申请直接内存空间
                .order(ByteOrder.nativeOrder()) //设置字节序为本地字节序
                .asFloatBuffer();//将直接内存空间转换为浮点数缓冲区
        vertexBuffer.put(VERTEX_DATA);//将顶点数据写入缓冲区
        vertexBuffer.position(0);//将缓冲区位置设置为0
        String vertexShaderCode = "attribute vec4 vPosition;\n" +
                "uniform float uRatio;\n" +
                "void main() {\n" +
                "gl_Position = vec4(vPosition.x, vPosition.y * uRatio, 0.0, 1.0);\n" +
                "}";//顶点着色器代码
        String fragmentShaderCode ="precision mediump float;\n" + "void main() {\n" +
                "gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
                "}";//片段元着色器代码
        //        创建程序
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        //        获取顶点着色器属性位置
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        //获取片段元着色器属性位置
        uRatioHandle = GLES20.glGetUniformLocation(mProgram, "uRatio");

    }

    private void initData() {
        List<Float> vertexList = new ArrayList<>();
        vertexList.add(0.0f);
        vertexList.add(0.0f);
        for (int i = 0; i <= SEGMENT; i++) {
            float angle = (float) (Math.PI * 2.0f / SEGMENT * i);
            vertexList.add((float) (RADIUS * Math.cos(angle)));
            vertexList.add((float) (RADIUS * Math.sin(angle)));
        }
        VERTEX_DATA = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            VERTEX_DATA[i] = vertexList.get(i);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视口
        GLES20.glViewport(0, 0, width, height);
        // 计算宽高比
        screenRatio = (float) width / height;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //使用程序
        GLES20.glUseProgram(mProgram);
        //设置宽高比
        GLES20.glUniform1f(uRatioHandle, screenRatio);
        GLES20.glVertexAttribPointer(vPositionHandle, 2,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_DATA.length / 2);
    }
}
