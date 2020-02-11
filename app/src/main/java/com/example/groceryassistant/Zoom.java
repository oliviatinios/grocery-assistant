package com.example.groceryassistant;

import android.view.View;

public class Zoom {

    private View mZoomInView               = null;
    private View mZoomOutView              = null;

    public Zoom(View zIn, View zOut) {
        mZoomInView  = zIn;
        mZoomOutView = zOut;
    }
    public void setVisibility(int visibility) {
        mZoomInView.setVisibility(visibility);
        mZoomOutView.setVisibility(visibility);
    }
}
