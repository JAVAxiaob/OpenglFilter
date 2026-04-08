package com.self.simplefilter;
import android.content.Context;
import android.opengl.GLSurfaceView;

import com.self.simplefilter.view.version2.fbo.ColorImageRender;


public class SimpleGLView extends GLSurfaceView {
    public SimpleGLView(Context context) {
        super(context);
        //设置EGL上下文版本为2.0
        setEGLContextClientVersion(2);
        //设置渲染器
        setRenderer(new ColorImageRender(this.getContext()));
    }
}

