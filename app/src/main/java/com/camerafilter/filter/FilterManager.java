package com.camerafilter.filter;

import java.util.Objects;
import android.util.Log;

public class FilterManager {

    private static final String TAG = "FilterManager";

    public enum FilterType {
        ORIGIN,
        BEAUTY
    }

    private  BeautyFilter beautyFilter ;
    private  CameraFilter cameraFilter ;

    private FilterType currentFilter = FilterType.ORIGIN;

    public void setCurrentFilter(FilterType type) {
        this.currentFilter = type;
    }
    /**
     * 延迟初始化 Shader，必须在 GL 上下文可用时调用
     */
    public void init() {
        if (beautyFilter == null) {
            beautyFilter = new BeautyFilter();
        }
        if (cameraFilter == null) {
            cameraFilter = new CameraFilter();
        }
    }
    public int onDraw(int textureId) {
        if (Objects.requireNonNull(currentFilter) == FilterType.BEAUTY) {
            Log.d(TAG, "onDraw: beautyFilter " + textureId);
            return beautyFilter.onDraw(textureId);
        } else {
            Log.d(TAG, "onDraw: cameraFilter " + textureId);
            return cameraFilter.onDraw(textureId);
        }
    }
}