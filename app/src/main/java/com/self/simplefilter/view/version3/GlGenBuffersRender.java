package com.self.simplefilter.view.version3;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
/**
 * 你现在用的 FloatBuffer 是客户端内存，每次绘制都要从 CPU 拷贝到 GPU，性能瓶颈明显。
 * VBO（顶点缓冲对象）是把顶点数据直接存到 GPU 显存，VAO（顶点数组对象）缓存顶点属性配置，
 * 是工业级渲染的标准。
 */


public class GlGenBuffersRender implements GLSurfaceView.Renderer{

    int mProgram;
    int vPositionHandle;

//   1. 定义顶点 + 索引（4个顶点，6个索引，复用顶点）
    private float[] vertices = {
            -0.5f, -0.5f,
            0.5f, -0.5f,
            -0.5f,  0.5f,
            0.5f,  0.5f
    };
    private short[] indices = {0, 1, 2, 2, 1, 3}; // 两个三角形，复用4个顶点
    // 2. 生成 VBO、VAO、EBO
    private  int[] vbo=new int[1];
    private  int[] vao=new int[1];
    private int[] ebo=new int[1];//索引缓冲区

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
        // 3. 生成 VBO、VAO、EBO
        // 创建 VAO

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
