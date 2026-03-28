package com.self.simplefilter.utils;

import android.opengl.GLES20;

public class OpenGLUtil {
    public static int compileShader(int type, String source) {
        //创建着色器
        int shader = GLES20.glCreateShader(type);
        //加载着色器代码
        GLES20.glShaderSource(shader, source);
        //编译（配置）
        GLES20.glCompileShader(shader);
        return shader;
    }

    public static int createProgram(String vertexStr, String fragStr) {
        //创建程序器
        int program = GLES20.glCreateProgram();
        //创建顶点着色器
        int vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexStr);
        //创建片元着色器
        int fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragStr);
        //绑定着色器到程序器
        GLES20.glAttachShader(program, vShader);
       //绑定片元着色器到程序器
        GLES20.glAttachShader(program, fShader);
        //链接程序器
        GLES20.glLinkProgram(program);
        return program;
    }
}
