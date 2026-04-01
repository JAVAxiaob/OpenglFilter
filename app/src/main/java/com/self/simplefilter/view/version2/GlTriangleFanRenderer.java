package com.self.simplefilter.view.version2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.self.simplefilter.utils.OpenGLUtil;

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
    final int SEGMENT = 180;
    final float RADIUS = 0.8f;

    float[] VERTEX_DATA;
    private int uRatioHandle;
    private float screenRatio;
    private int uTimeHandle;
    private float time;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initData();
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4) //申请直接内存空间
                .order(ByteOrder.nativeOrder()) //设置字节序为本地字节序
                .asFloatBuffer();//将直接内存空间转换为浮点数缓冲区
        vertexBuffer.put(VERTEX_DATA);//将顶点数据写入缓冲区
        vertexBuffer.position(0);//将缓冲区位置设置为0
        // 【修复1：顶点着色器确保uTime被使用，不被优化】
        String vertexShaderCode = "attribute vec4 vPosition;\n"
                + "uniform float uRatio;\n"
                + "uniform float uTime;\n"
                + "void main() {\n"
                + "    float scale = 0.5 + 0.3 * sin(uTime * 2.0);\n" // 降低缩放幅度，更明显
                + "    vec2 pos = vPosition.xy * scale;\n"
                + "    gl_Position = vec4(pos.x / uRatio, pos.y, 0.0, 1.0);\n"
                + "}";

        // 【修复2：片元着色器优化动画，避免初始卡死绿色】
        String fragmentShaderCode = "precision mediump float;\n"
                + "uniform float uTime;\n"
                + "void main() {\n"
                + "    float t = uTime * 1.5;\n" // 加快动画速度
                + "    float r = 0.5 + 0.5 * sin(t);\n"
                + "    float g = 0.5 + 0.5 * cos(t * 1.2);\n"
                + "    float b = 0.5 + 0.5 * sin(t * 0.8 + 1.57);\n" // 偏移相位，避免初始同值
                + "    gl_FragColor = vec4(r, g, b, 1.0);\n"
                + "}";
        //        创建程序
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        //        获取顶点着色器属性位置
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        //获取片段元着色器属性位置
        uRatioHandle = GLES20.glGetUniformLocation(mProgram, "uRatio");

        //获取片段元着色器属性位置
        uTimeHandle = GLES20.glGetUniformLocation(mProgram, "uTime");
       Log.d("GL_DEBUG", "uTimeHandle: " + uTimeHandle);
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
        // ✅ 修复：把时间限制在 0~10 之间循环，sin/cos 才会明显变化
        float time = (System.currentTimeMillis() % 10000) / 1000f;
        Log.d("GL_DEBUG", "time: " + time);
        //清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //使用程序
        GLES20.glUseProgram(mProgram);
        //设置宽高比
        GLES20.glUniform1f(uRatioHandle, screenRatio);
        GLES20.glUniform1f(uTimeHandle, time);
        GLES20.glVertexAttribPointer(vPositionHandle, 2,
                GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, VERTEX_DATA.length / 2);
    }
}
