package com.camerafilter.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.opengl.GLSurfaceView;

import com.camerafilter.filter.FilterManager;
import com.camerafilter.renderer.CameraRenderer;
import com.maniu.openglfilter.R;

public class MainActivity extends AppCompatActivity {

    private GLSurfaceView glSurfaceView;
    private CameraRenderer renderer;

    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.caramer_activity_main);

        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new CameraRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            startCamera();
        }

        Button btn_origin = findViewById(R.id.btn_origin);
        Button btn_beauty = findViewById(R.id.btn_beauty);

        btn_origin.setOnClickListener(v -> renderer.setFilter(FilterManager.FilterType.ORIGIN));
        btn_beauty.setOnClickListener(v -> renderer.setFilter(FilterManager.FilterType.BEAUTY));
    }

    private void startCamera() {
        CameraX.unbindAll();

        PreviewConfig config = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(currentFacing)
                .build();

        Preview preview = new Preview(config);
        preview.setOnPreviewOutputUpdateListener(renderer);

        CameraX.bindToLifecycle(this, preview);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }
}