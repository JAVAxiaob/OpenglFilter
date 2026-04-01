package com.self.simplefilter.view.version2.fbo;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MiniFBORenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MiniFBORenderer";

    // ================= 全局变量定义 =================
    private int mProgram;                // 第一个着色器程序（用于渲染到FBO）
    private int vPositionHandle;         // 顶点坐标句柄
    private int uMatrix;                 // 变换矩阵句柄
    private float[] mMatrix = new float[16]; // 正交投影矩阵
    private FloatBuffer vertexBuffer;     // 矩形顶点数据缓冲
    private int fboId;                    // FBO 帧缓冲对象ID
    private int fboTextureId;             // FBO 绑定的纹理ID（用于存储渲染结果）
    private final int FBO_W = 500;        // FBO 宽度
    private final int FBO_H = 800;        // FBO 高度
    private int screenProgram;            // 第二个着色器程序（用于把FBO画到屏幕）
    private int screenPositionHandle;     // 屏幕顶点句柄
    private int screenTexCoordHandle;     // 纹理坐标句柄
    private FloatBuffer screenBuffer;     // 屏幕顶点+纹理坐标数据缓冲

    // 矩形顶点数据（归一化设备坐标 -1.0 到 1.0）
    // 顺序：左下 -> 右下 -> 右上 -> 左上 (顺时针)
    private final float[] rect = {
            -0.5f, -0.5f,  // 索引0：左下角
            0.5f, -0.5f,   // 索引1：右下角
            0.5f, 0.5f,    // 索引2：右上角
            -0.5f, 0.5f    // 索引3：左上角
    };

    // 全屏四边形数据 (x, y, texX, texY)
    // 覆盖整个屏幕，纹理坐标对应0-1
    private final float[] screen = {
            -1f, -1f, 0f, 0f,    // 左下
            1f, -1f,  1f, 0f,    // 右下
            -1f, 1f,  0f, 1f,     // 左上
            1f, 1f,   1f, 1f      // 右上
    };

    // ================= 生命周期方法 =================

    /**
     * Surface 创建时调用（APP 启动、页面切回前台时都会重新创建）
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // 1. 设置清屏颜色（灰色）
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);

        // 2. 初始化矩形顶点缓冲
        // allocateDirect: 分配本地内存（不受GC管理，适合GL频繁访问）
        // order: 设置字节顺序为CPU原生顺序
        // asFloatBuffer: 转为浮点缓冲
        // put: 把数据写入缓冲
        vertexBuffer = ByteBuffer.allocateDirect(rect.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(rect);
        vertexBuffer.position(0); // 重置指针到开头

        // 3. 初始化屏幕渲染用的顶点缓冲（逻辑同上）
        screenBuffer = ByteBuffer.allocateDirect(screen.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(screen);
        screenBuffer.position(0);

        // 4. 编译链接第一个程序（画矩形到FBO）
        // 顶点着色器：接收顶点坐标和变换矩阵
        // 片段着色器：输出固定橙色
        String vertexShaderCode = "attribute vec4 vPosition;\n" +
                "uniform mat4 umMatrix;\n" + // 声明 uniform 矩阵
                "void main() {\n" +
                "    gl_Position = umMatrix * vPosition;\n" + // 计算最终坐标
                "}";
        String fragmentShaderCode = "precision mediump float;\n" +
                "void main() {\n" +
                "    gl_FragColor = vec4(1.0, 0.5, 0.2, 1.0);\n" + // 橙色
                "}";

        // 创建程序（工具类方法，内部调用 glCreateShader/ glShaderSource/ glCompileShader/ glLinkProgram）
        mProgram = OpenGLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        // 获取属性和统一变量的地址（句柄）
        vPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        uMatrix = GLES20.glGetUniformLocation(mProgram, "umMatrix");

        // 5. 编译链接第二个程序（把FBO纹理画到屏幕）
        // 顶点着色器：直接输出顶点坐标，接收纹理坐标并传递给片段着色器
        // 片段着色器：采样FBO纹理并输出
        String vsScreen = "attribute vec4 vPosition;\n"
                + "attribute vec2 vTexCoord;\n"
                + "varying vec2 texCoord;\n" //  varying 变量用于传递数据给片段着色器
                + "void main(){\n"
                + "    gl_Position = vPosition;\n"
                + "    texCoord = vTexCoord;\n"
                + "}";

        String fsScreen = "precision mediump float;\n"
                + "uniform sampler2D uTexture;\n" // 采样器变量
                + "varying vec2 texCoord;\n"
                + "void main(){\n"
                + "    gl_FragColor = texture2D(uTexture, texCoord);\n" // 采样纹理
                + "}";

        screenProgram = OpenGLUtil.createProgram(vsScreen, fsScreen);
        // 获取句柄
        screenPositionHandle = GLES20.glGetAttribLocation(screenProgram, "vPosition");
        screenTexCoordHandle = GLES20.glGetAttribLocation(screenProgram, "vTexCoord");

        // 6. 创建并初始化 FBO 对象
        createFBO();
    }

    /**
     * Surface 尺寸变化时调用（屏幕旋转、屏幕分屏等）
     * @param width 屏幕宽
     * @param height 屏幕高
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // 1. 设置视口（渲染区域）为整个屏幕
        GLES20.glViewport(0, 0, width, height);

        // 2. 计算宽高比
        float ratio = (float) width / height;

        // 3. 设置正交投影矩阵
        // 参数：[输出矩阵], [偏移], [左], [右], [底], [顶], [近], [远]
        // -1, 1: X轴范围 -1 到 1
        // -ratio, ratio: Y轴范围根据比例调整，保持矩形不变形
        // -1, 1: 近远平面
        Matrix.orthoM(mMatrix, 0, -1, 1, -ratio, ratio, -1, 1);
    }

    /**
     * 每一帧绘制时调用
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // -------- 第一步：渲染内容到 FBO 帧缓冲区 --------
        // 1. 绑定 FBO，之后所有渲染都画到这个 FBO 里
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        // 2. 设置 FBO 的视口（渲染区域为 FBO 的大小）
        GLES20.glViewport(0, 0, FBO_W, FBO_H);
        // 3. 清除颜色缓冲（用灰色填充）
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 4. 使用第一个着色器程序
        GLES20.glUseProgram(mProgram);
        // 5. 传入变换矩阵（正交投影矩阵）
        GLES20.glUniformMatrix4fv(uMatrix, 1, false, mMatrix, 0);

        // 6. 启用顶点属性数组（必须启用才能使用）
        GLES20.glEnableVertexAttribArray(vPositionHandle);
        // 7. 设置顶点数据指针
        // 参数：[句柄], [维度数], [类型], [是否归一化], [步长], [缓冲指针]
        // 这里每个顶点2个float(x,y)，步长8字节(2*4)，从vertexBuffer开头开始
        GLES20.glVertexAttribPointer(vPositionHandle, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer);

        // 8. 执行绘制！
        // GL_TRIANGLE_FAN: 扇形绘制方式，以第0个点为中心，依次连接0-1-2, 0-2-3，形成一个完整矩形
        // 0: 起始索引, 4: 顶点数量
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        // 9. 禁用顶点属性（释放状态，防止后续渲染干扰）
        GLES20.glDisableVertexAttribArray(vPositionHandle);


        // -------- 第二步：把 FBO 的纹理渲染到手机屏幕 --------
        // 1. 解绑 FBO，恢复默认渲染（渲染到屏幕）
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        // 2. 设置屏幕视口（使用屏幕真实宽高，铺满全屏）
        // 注意：这里如果要动态获取width/height，最好从onSurfaceChanged存起来
        // 临时写法：GLES20.glViewport(0, 0, width, height);
        // 这里为了简单演示，直接写固定全屏，实际项目建议传入宽高
        GLES20.glViewport(0, 0, 500, 800);
        // 3. 清除屏幕缓冲
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 4. 使用第二个着色器程序
        GLES20.glUseProgram(screenProgram);

        // 5. 激活并绑定 FBO 纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); // 激活纹理单元0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId); // 绑定FBO纹理
        // 6. 获取采样器句柄并赋值为0（表示使用纹理单元0）
        int uTexture = GLES20.glGetUniformLocation(screenProgram, "uTexture");
        GLES20.glUniform1i(uTexture, 0);

        // 7. 设置顶点数据（位置 + 纹理坐标）
        screenBuffer.position(0); // 指针移到x,y坐标开头
        // 设置位置属性，每个顶点2个float，步长16字节(4*4)
        GLES20.glVertexAttribPointer(screenPositionHandle, 2, GLES20.GL_FLOAT, false, 16, screenBuffer);

        screenBuffer.position(2); // 指针移到texX, texY坐标开头
        // 设置纹理坐标属性，每个顶点2个float，步长16字节
        GLES20.glVertexAttribPointer(screenTexCoordHandle, 2, GLES20.GL_FLOAT, false, 16, screenBuffer);

        // 8. 启用两个顶点属性数组
        GLES20.glEnableVertexAttribArray(screenPositionHandle);
        GLES20.glEnableVertexAttribArray(screenTexCoordHandle);

        // 9. 执行绘制，把纹理贴到屏幕
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 10. 禁用数组，清理状态
        GLES20.glDisableVertexAttribArray(screenPositionHandle);
        GLES20.glDisableVertexAttribArray(screenTexCoordHandle);
    }

    /**
     * 专门负责创建 FBO 和 纹理
     */
    private void createFBO() {
        int[] fboIds = new int[1];

        // 1. 生成 FBO ID
        GLES20.glGenFramebuffers(1, fboIds, 0);
        fboId = fboIds[0];

        // 2. 生成纹理 ID（用于存储FBO渲染结果）
        GLES20.glGenTextures(1, fboIds, 0);
        fboTextureId = fboIds[0];

        // 3. 绑定纹理目标
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId);

        // 4. 定义纹理图像（空数据，只申请内存）
        // 参数：[目标], [多级渐远级别], [内部格式], [宽], [高], [边界], [格式], [类型], [像素数据]
        // null表示暂时不传入像素数据，后续渲染会自动填充
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,
                GLES20.GL_RGBA, FBO_W, FBO_H, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

        // 5. 设置纹理参数（缩小/放大过滤器为线性插值）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // 6. 绑定 FBO，并将纹理附加到 FBO 的颜色附着点
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fboTextureId, 0);

        // 7. 检查FBO完整性（必须检查，否则可能渲染失败）
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.d(TAG, "createFBO: FBO 创建失败，不完整");
        }

        // 8. 解绑 FBO 和 纹理（防止意外修改）
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
}