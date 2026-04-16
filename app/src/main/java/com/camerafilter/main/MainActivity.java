//package com.camerafilter.main;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.opengl.GLSurfaceView;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.camera.core.CameraX;
//import androidx.camera.core.Preview;
//import androidx.camera.core.PreviewConfig;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import com.camerafilter.filter.FilterManager;
//import com.camerafilter.renderer.CameraRenderer;
//import com.maniu.openglfilter.R;
//
//public class MainActivity extends AppCompatActivity {
//    public GLSurfaceView glSurfaceView;
//    private CameraRenderer renderer;
//
//    // 你要的前后摄像头切换
//    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        glSurfaceView = findViewById(R.id.gl_surface_view);
//        glSurfaceView.setEGLContextClientVersion(2);
//        renderer = new CameraRenderer(this, glSurfaceView);
//        glSurfaceView.setRenderer(renderer);
//        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//
//        // 权限
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
//        } else {
//            startCamera();
//        }
//
//        Button btn_origin = findViewById(R.id.btn_origin);
//        Button btn_beauty = findViewById(R.id.btn_beauty);
//        Button btn_switch = findViewById(R.id.btn_switch);
//
//        btn_origin.setOnClickListener(v -> {
//            renderer.setFilter(FilterManager.FilterType.ORIGIN);
//            Toast.makeText(this, "原图", Toast.LENGTH_SHORT).show();
//        });
//
//        btn_beauty.setOnClickListener(v -> {
//            renderer.setFilter(FilterManager.FilterType.BEAUTY);
//            Toast.makeText(this, "美颜", Toast.LENGTH_SHORT).show();
//        });
//
//        // 切换前后摄像头
//        btn_switch.setOnClickListener(v -> {
//            if (currentFacing == CameraX.LensFacing.BACK) {
//                currentFacing = CameraX.LensFacing.FRONT;
//            } else {
//                currentFacing = CameraX.LensFacing.BACK;
//            }
//            startCamera();
//        });
//    }
//
//    /**
//     * 【CameraX alpha05 专用】启动相机（完全适配你的版本）
//     */
//    private void startCamera() {
//        CameraX.unbindAll();
//
//        PreviewConfig config = new PreviewConfig.Builder()
//                .setLensFacing(currentFacing)
//                .build();
//
//        Preview preview = new Preview(config);
//        preview.setPreviewSurfaceProvider(renderer.getPreviewSurfaceProvider());
//
//        CameraX.bindToLifecycle(this, preview);
//    }
//}