package com.self.simplefilter.view.version2.fbo;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.self.simplefilter.utils.OpenGLUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GaussianBlurRender implements GLSurfaceView.Renderer {
    // 基础渲染相关
    private int mBaseProgram;
    private int mBaseMatrixHandle;
    private int mBasePositionHandle;
    private FloatBuffer mBaseVertexBuffer;
    private final float[] mProjectionMatrix = new float[16];

    // 红色矩形顶点（标准TRIANGLE_STRIP顺序：左下、左上、右下、右上）
    private final float[] mRectVertices = {
            -0.6f, -0.6f,  // 左下
            -0.6f,  0.6f,  // 左上
            0.6f, -0.6f,  // 右下
            0.6f,  0.6f   // 右上
    };

    // FBO相关
    private int mFboId;
    private int mFboTextureId;
    private int mFboWidth = 540;
    private int mFboHeight = 960;

    // 高斯模糊渲染相关
    private int mBlurProgram;
    private int mBlurPositionHandle;
    private int mBlurTexCoordHandle;
    private int mBlurTextureHandle;
    private int mBlurTexelSizeHandle;
    private FloatBuffer mBlurVertexBuffer;

    // 全屏顶点+纹理坐标（x,y,s,t），标准TRIANGLE_STRIP顺序
    private final float[] mFullScreenVertices = {
            -1.0f, -1.0f, 0.0f, 0.0f,  // 左下
            1.0f, -1.0f, 1.0f, 0.0f,  // 右下
            -1.0f,  1.0f, 0.0f, 1.0f,  // 左上
            1.0f,  1.0f, 1.0f, 1.0f   // 右上
    };

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        checkGLError("glClearColor");

        // 1. 初始化基础矩形顶点
        mBaseVertexBuffer = ByteBuffer.allocateDirect(mRectVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mRectVertices);
        mBaseVertexBuffer.position(0);

        // 2. 初始化全屏顶点+纹理坐标
        mBlurVertexBuffer = ByteBuffer.allocateDirect(mFullScreenVertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(mFullScreenVertices);
        mBlurVertexBuffer.position(0);

        // 3. 创建基础着色器（画红色矩形）
        String baseVertexShader =
                "attribute vec4 aPosition;\n" +
                        "uniform mat4 uProjectionMatrix;\n" +
                        "void main() {\n" +
                        "    gl_Position = uProjectionMatrix * aPosition;\n" +
                        "}";

        String baseFragmentShader =
                "precision mediump float;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = vec4(1.0, 0.3, 0.3, 1.0);\n" +
                        "}";

        mBaseProgram = OpenGLUtil.createProgram(baseVertexShader, baseFragmentShader);
        if (mBaseProgram == 0) throw new RuntimeException("基础着色器创建失败");
        checkGLError("create base program");

        mBaseMatrixHandle = GLES20.glGetUniformLocation(mBaseProgram, "uProjectionMatrix");
        mBasePositionHandle = GLES20.glGetAttribLocation(mBaseProgram, "aPosition");
        checkGLError("get base shader location");

        // 4. 创建高斯模糊着色器
        String blurVertexShader =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    gl_Position = aPosition;\n" +
                        "    vTexCoord = aTexCoord;\n" +
                        "}";

        String blurFragmentShader =
                "precision mediump float;\n" +
                        "uniform sampler2D uTexture;\n" +
                        "uniform vec2 uTexelSize;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "void main() {\n" +
                        "    vec4 color = vec4(0.0);\n" +
                        "    // 5阶水平高斯模糊核（权重和为1）\n" +
                        "    color += texture2D(uTexture, vTexCoord + vec2(-2.0, 0.0) * uTexelSize) * 0.061;\n" +
                        "    color += texture2D(uTexture, vTexCoord + vec2(-1.0, 0.0) * uTexelSize) * 0.242;\n" +
                        "    color += texture2D(uTexture, vTexCoord + vec2( 0.0, 0.0) * uTexelSize) * 0.383;\n" +
                        "    color += texture2D(uTexture, vTexCoord + vec2( 1.0, 0.0) * uTexelSize) * 0.242;\n" +
                        "    color += texture2D(uTexture, vTexCoord + vec2( 2.0, 0.0) * uTexelSize) * 0.061;\n" +
                        "    gl_FragColor = color;\n" +
                        "}";

        mBlurProgram = OpenGLUtil.createProgram(blurVertexShader, blurFragmentShader);
        if (mBlurProgram == 0) throw new RuntimeException("模糊着色器创建失败");
        checkGLError("create blur program");

        mBlurPositionHandle = GLES20.glGetAttribLocation(mBlurProgram, "aPosition");
        mBlurTexCoordHandle = GLES20.glGetAttribLocation(mBlurProgram, "aTexCoord");
        mBlurTextureHandle = GLES20.glGetUniformLocation(mBlurProgram, "uTexture");
        mBlurTexelSizeHandle = GLES20.glGetUniformLocation(mBlurProgram, "uTexelSize");
        checkGLError("get blur shader location");

        // 5. 创建FBO
        createFBO();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mFboWidth = width;
        mFboHeight = height;
        GLES20.glViewport(0, 0, width, height);
        checkGLError("glViewport");

        // 计算正交投影矩阵，适配屏幕比例，保证矩形不变形
        float ratio = (float) width / height;
        Matrix.orthoM(mProjectionMatrix, 0, -1, 1, -ratio, ratio, -1, 1);
        checkGLError("Matrix.orthoM");

        // 屏幕尺寸变化时，重建FBO（避免纹理尺寸不匹配）
        createFBO();
    }

    /**
     * 创建离屏渲染FBO
     */
    private void createFBO() {
        // 1. 生成FBO
        int[] fboIds = new int[1];
        GLES20.glGenFramebuffers(1, fboIds, 0);
        mFboId = fboIds[0];
        checkGLError("glGenFramebuffers");

        // 2. 生成纹理（和FBO分开数组，避免覆盖）
        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        mFboTextureId = texIds[0];
        checkGLError("glGenTextures");

        // 3. 配置纹理参数
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId);
        //当纹理被缩小显示时，使用线性平滑采样 (让缩小后的图不锯齿、更清晰)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //当纹理被放大显示时，也使用线性平滑采样。（放大不模糊、不锯齿，高斯模糊必须用这个
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //纹理 ** 水平方向（左右）** 坐标超出 0~1 范围时，直接拉伸边缘颜色，不重复、不出现黑边。
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //纹理 ** 垂直方向（上下）** 坐标超出 0~1 范围时，直接拉伸边缘颜色，不重复、不出现黑边。
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGLError("glTexParameteri");

        // 4. 分配纹理存储空间（RGBA_8888）
        GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,  // 1. 这是一个 2D 纹理
                0,                     // 2. 使用基础层级（mipmap 0）
                GLES20.GL_RGBA,        // 3. 纹理在GPU里存成 RGBA 格式（红、绿、蓝、透明）
                mFboWidth,             // 4. 纹理宽度
                mFboHeight,            // 5. 纹理高度
                0,                     // 6. 边框（必须填 0，OpenGL ES 不支持边框）
                GLES20.GL_RGBA,        // 7. 数据格式也是 RGBA
                GLES20.GL_UNSIGNED_BYTE,//8. 每个颜色占 8 位（0~255）
                null                   // 9. 不传图片数据 → 只开辟空显存
        );
        checkGLError("glTexImage2D");

        // 5. 绑定FBO并附加纹理
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
        GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,      // 目标：帧缓冲
                GLES20.GL_COLOR_ATTACHMENT0,// 挂到：颜色附着点0
                GLES20.GL_TEXTURE_2D,       // 纹理类型
                mFboTextureId,              // 要挂上去的纹理ID
                0                            // mipmap层级（固定0）
        );
        checkGLError("glFramebufferTexture2D");

        // 6. 检查FBO完整性
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FBO创建失败，状态码: " + status);
        }

        // 7. 解绑FBO，切回屏幕帧缓冲
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        checkGLError("unbind FBO");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            // ====================== 第一步：离屏渲染红色矩形到FBO ======================
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFboId);
            GLES20.glViewport(0, 0, mFboWidth, mFboHeight);
            GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            checkGLError("bind FBO & clear");

            // 使用基础着色器绘制矩形
            GLES20.glUseProgram(mBaseProgram);
            GLES20.glUniformMatrix4fv(mBaseMatrixHandle, 1, false, mProjectionMatrix, 0);

            mBaseVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(mBasePositionHandle, 2, GLES20.GL_FLOAT, false, 8, mBaseVertexBuffer);
            GLES20.glEnableVertexAttribArray(mBasePositionHandle);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGLError("draw base rect to FBO");

            // ====================== 第二步：解绑FBO，对纹理做高斯模糊并渲染到屏幕 ======================
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glViewport(0, 0, mFboWidth, mFboHeight);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            checkGLError("unbind FBO & clear screen");

            // 使用模糊着色器
            GLES20.glUseProgram(mBlurProgram);
            // 绑定纹理单元0
            GLES20.glUniform1i(mBlurTextureHandle, 0);
            // 传入纹素大小（1/宽，1/高），用于采样偏移
            GLES20.glUniform2f(mBlurTexelSizeHandle, 1f / mFboWidth, 1f / mFboHeight);
            checkGLError("set blur uniforms");

            // 设置顶点和纹理坐标
            mBlurVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(
                    mBlurPositionHandle,  // 传给：位置属性
                    2,                    // 每次取 2个值：x,y
                    GLES20.GL_FLOAT,      // 浮点型
                    false,                // 不用归一化
                    16,                   // 读完一组，跳过 16字节，再取下一组
                    mBlurVertexBuffer     // 数据源
            );
            mBlurVertexBuffer.position(2);
            GLES20.glVertexAttribPointer(
                    mBlurTexCoordHandle,  // 传给：纹理坐标
                    2,                    // 每次取 2个值：s,t
                    GLES20.GL_FLOAT,
                    false,
                    16,                   // 同样跳过 16字节
                    mBlurVertexBuffer
            );

            GLES20.glEnableVertexAttribArray(mBlurPositionHandle);
            GLES20.glEnableVertexAttribArray(mBlurTexCoordHandle);
            checkGLError("enable vertex attribs");

            // 激活纹理单元0，绑定FBO纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboTextureId);
            checkGLError("bind FBO texture");

            // 绘制全屏模糊效果
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGLError("draw blur to screen");

            // 禁用顶点数组（可选，规范写法）
            GLES20.glDisableVertexAttribArray(mBasePositionHandle);
            GLES20.glDisableVertexAttribArray(mBlurPositionHandle);
            GLES20.glDisableVertexAttribArray(mBlurTexCoordHandle);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * OpenGL错误检查工具
     */
    private void checkGLError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            String errorMsg;
            switch (error) {
                case GLES20.GL_INVALID_ENUM: errorMsg = "GL_INVALID_ENUM (无效枚举)"; break;
                case GLES20.GL_INVALID_VALUE: errorMsg = "GL_INVALID_VALUE (无效值)"; break;
                case GLES20.GL_INVALID_OPERATION: errorMsg = "GL_INVALID_OPERATION (无效操作)"; break;
                case GLES20.GL_OUT_OF_MEMORY: errorMsg = "GL_OUT_OF_MEMORY (内存不足)"; break;
                default: errorMsg = "未知错误(" + error + ")";
            }
            throw new RuntimeException("【GL错误】" + op + " → " + errorMsg);
        }
    }
}