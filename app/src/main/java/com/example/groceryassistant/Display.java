package com.example.groceryassistant;

import android.graphics.PointF;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.LocationView;
import com.navigine.naviginesdk.SubLocation;

public class Display {

    // UI Parameters
    private LocationView mLocationView;
    private View mBackView;
    private View mZoomInView;
    private View mZoomOutView;

    private View mAdjustModeView;
    private TextView mErrorMessageLabel;

    public float mDisplayDensity = 0.0f;


    public Display(LocationView location, View back, View zIn, View zOut, View mode, TextView error) {
        mLocationView = location;
        mBackView = back;
        mZoomInView  = zIn;
        mZoomOutView = zOut;
        mAdjustModeView = mode;
        mErrorMessageLabel = error;

//        mLocationView.setBackgroundColor(0xffebebeb); // The old background colour
        mLocationView.setBackgroundColor(0xAA40E0D0);
    }

    public void setBackVisibility(int visibility) {
        mBackView.setVisibility(visibility);
    }

    public void setZoomVisibility(int visibility) {
        mZoomInView.setVisibility(visibility);
        mZoomOutView.setVisibility(visibility);
    }

    public void setAdjustVisibility(int visibility) {
        mErrorMessageLabel.setVisibility(visibility);
    }

    public void setErrorVisibility(int visibility) {
        mErrorMessageLabel.setVisibility(visibility);
    }

    public void setListeners(LocationView.Listener locationListener, View.OnLayoutChangeListener layoutListener) {
        mLocationView.setListener(locationListener);
        mLocationView.addOnLayoutChangeListener(layoutListener);
    }

    public void setZoomParameters(float minZoomFactor, float maxZoomFactor) {
        mLocationView.setZoomRange(minZoomFactor, maxZoomFactor);
        mLocationView.setZoomFactor(minZoomFactor);
    }

    public void setErrorMessage(String message)
    {
        mErrorMessageLabel.setText(message);
        mErrorMessageLabel.setVisibility(View.VISIBLE);
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
