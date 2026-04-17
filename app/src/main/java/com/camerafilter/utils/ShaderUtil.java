package com.camerafilter.utils;

import android.opengl.GLES20;
import android.util.Log;

public class ShaderUtil {
    public static final String TAG = "ShaderUtil";

    public static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            int error = GLES20.glGetError();
            Log.e(TAG, "glCreateShader 失败！type=" + type + " glError=" + error);
            return 0;
        }
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "着色器编译错误：" + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "glCreateProgram 失败！");
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "createProgram 链接错误：" + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            return 0;
        }

        return program;
    }
}