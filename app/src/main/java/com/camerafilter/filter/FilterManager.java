package com.camerafilter.filter;

import java.util.Objects;

public class FilterManager {
    public enum FilterType {
        ORIGIN,
        BEAUTY
    }

    private final BeautyFilter beautyFilter = new BeautyFilter();
    private FilterType correntFilter = FilterType.ORIGIN;

    public void setFilter(FilterType type) {
        this.correntFilter = type;
    }

    public int onDraw(int textureId) {
        if (Objects.requireNonNull(correntFilter) == FilterType.BEAUTY) {
            return beautyFilter.onDraw(textureId);
        }
        return textureId;
    }

}
