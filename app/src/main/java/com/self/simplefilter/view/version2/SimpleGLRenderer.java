package com.self.simplefilter.view.version2;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//渲染器
public class SimpleGLRenderer implements GLSurfaceView.Renderer {
    int mProgram;
    int vPositionHandle;
    final float[] VERTEX_DATA = {
            -1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,};

    FloatBuffer vertexBuffer;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //创建顶点缓冲区
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        //创建顶点缓冲区
        vertexBuffer = ByteBuffer
                .allocateDirect(VERTEX_DATA.length * 4) //申请直接内存空间
                .order(ByteOrder.nativeOrder()) //设置字节序为本地字节序
                .asFloatBuffer();//将直接内存空间转换为浮点数缓冲区
        vertexBuffer.put(VERTEX_DATA);//将顶点数据写入缓冲区
        vertexBuffer.position(0);//将缓冲区位置设置为0

        String vertexShaderCode = "attribute vec4 vPosition;\n" +
                "void main() {\n" +
                "gl_Position = vPosition;\n" +
                "}";//顶点着色器代码
        String fragmentShaderCode = "precision mediump float;\n" +"void main() {\n" +
                "gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
                "}";//片段元着色器代码
//        创建程序
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
//        获取顶点着色器属性位置
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
    }


    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        //设置视口
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //使用程序
        GLES20.glUseProgram(mProgram);
        //设置顶点属性
        GLES20.glVertexAttribPointer(
                vPositionHandle,//顶点着色器属性位置
                2,//每个顶点的属性数量
                GLES20.GL_FLOAT,//属性类型
                false,//是否需要归一化
                0,//属性偏移量
                vertexBuffer//顶点Buffer
        );
        //启用顶点属性
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        //绘制三角形条带 后面的三角形，自动复用前面的最后 2 个点！
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }
}
