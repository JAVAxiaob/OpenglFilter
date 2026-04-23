package com.camerafilter.filter;

import java.util.Objects;

import android.util.Log;

public class FilterManager {

    private static final String TAG = "FilterManager";
    private int width;
    private int height;

    public void setCameraSize(int cameraWidth, int cameraHeight, int width, int height) {
        if (cameraFilter != null) {
            cameraFilter.setCameraSize(cameraWidth, cameraHeight, width, height);
        }
    }

    public void setCameraOrientation(int rotation, boolean frontCamera) {
        if (cameraFilter!=null){
            cameraFilter.setCameraOrientation(rotation, frontCamera);
        }
    }

    public enum FilterType {
        ORIGIN,
        BEAUTY
    }

    private BeautyFilter beautyFilter;
    private CameraFilter cameraFilter;
    private FilterType currentFilter = FilterType.ORIGIN;

    public void setCurrentFilter(FilterType type) {
        this.currentFilter = type;
    }

    public void init() {
        if (beautyFilter == null) {
            beautyFilter = new BeautyFilter();
        }
        if (cameraFilter == null) {
            cameraFilter = new CameraFilter();
        }
    }

    // ==========================================
    // ✅ 【正确流程】
    // 相机纹理 → CameraFilter（转FBO） → 美颜/原图 → 输出
    // ==========================================
    public int onDraw(int textureId) {
        // 1. 无论开不开美颜，第一步必须走 CameraFilter
        // 把 OES 纹理 → 转为 FBO 普通纹理
        int fboTextureId = cameraFilter.onDraw(textureId);

        // 2. 判断是否开启美颜
        if (Objects.requireNonNull(currentFilter) == FilterType.BEAUTY) {
            Log.d(TAG, "onDraw: 美颜模式");
            beautyFilter.setWidth(width);
            beautyFilter.setHeight(height);
            return beautyFilter.onDraw(textureId);
        } else {
            Log.d(TAG, "onDraw: 原图模式");
            // 原图模式：直接渲染 FBO 纹理到屏幕
            return fboTextureId;
        }
    }


    public void setWidth(int width) {
        this.width = width;
    }


    public void setHeight(int height) {
        this.height = height;
    }
}