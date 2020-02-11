package com.example.groceryassistant;

import android.graphics.PointF;
import android.view.View;

import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.LocationView;
import com.navigine.naviginesdk.SubLocation;

public class Display {

    // UI Parameters
    public LocationView mLocationView;
    private View mBackView;
    private View mZoomInView;
    private View mZoomOutView;


    public Display(LocationView location, View back, View zIn, View zOut) {
        mLocationView = location;
        mBackView = back;
        mZoomInView  = zIn;
        mZoomOutView = zOut;

        mLocationView.setBackgroundColor(0xffebebeb);
    }

    public void setBackVisibility(int visibility) {
        mBackView.setVisibility(visibility);
    }

    public void setZoomVisibility(int visibility) {
        mZoomInView.setVisibility(visibility);
        mZoomOutView.setVisibility(visibility);
    }

    public void setListeners(LocationView.Listener locationListener, View.OnLayoutChangeListener layoutListener) {
        mLocationView.setListener(locationListener);
        mLocationView.addOnLayoutChangeListener(layoutListener);
    }

    public void setZoomParameters(float minZoomFactor, float maxZoomFactor) {
        mLocationView.setZoomRange(minZoomFactor, maxZoomFactor);
        mLocationView.setZoomFactor(minZoomFactor);
    }

    public void redrawLocationView() {
        mLocationView.redraw();
    }

    public void zoomLocationView(float zoom) {
        mLocationView.zoomBy(zoom);
    }

    public PointF getAbsCoordinates(float x, float y) {
        return mLocationView.getAbsCoordinates(x, y);
    }

    public PointF getScreenCoordinates(float x, float y) {
        return mLocationView.getScreenCoordinates(x, y);
    }

    public PointF getScreenCoordinates(LocationPoint p) {
        return mLocationView.getScreenCoordinates(p);
    }

    public float getLocationWidth() {
        return mLocationView.getWidth();
    }

    public float getLocationHeight() {
        return mLocationView.getHeight();
    }

    public float getScreenLengthX(float r) {
        return mLocationView.getScreenLengthX(r);
    }

    public boolean loadSubLocation(SubLocation subLoc) {
        return mLocationView.loadSubLocation(subLoc);
    }
    public void scrollByLocation(float deltaX, float deltaY) {
        mLocationView.scrollBy(deltaX, deltaY);
    }



}
