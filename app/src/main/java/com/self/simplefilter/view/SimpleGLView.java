package com.self.simplefilter.view;
import android.content.Context;
import android.opengl.GLSurfaceView;


public class SimpleGLView extends GLSurfaceView {
    public SimpleGLView(Context context) {
        super(context);
        //设置EGL上下文版本为2.0
        setEGLContextClientVersion(2);
        //设置渲染器
        setRenderer(new IsoscelesTrapezoid());
    }
}

