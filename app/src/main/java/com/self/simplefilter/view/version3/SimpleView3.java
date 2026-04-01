package com.self.simplefilter.view.version3;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class SimpleView3 extends GLSurfaceView {

    public SimpleView3(Context context) {
        super(context);
        //设置EGL上下文版本为2.0
        setEGLContextClientVersion(3);
        //设置渲染器
        setRenderer(new GlGenBuffersRender());
    }
}
