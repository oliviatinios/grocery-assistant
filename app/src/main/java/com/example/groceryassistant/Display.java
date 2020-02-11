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

    float mDisplayDensity = 0.0f;


    Display(LocationView location, View back, View zIn, View zOut, View mode, TextView error) {
        mLocationView = location;
        mBackView = back;
        mZoomInView  = zIn;
        mZoomOutView = zOut;
        mAdjustModeView = mode;
        mErrorMessageLabel = error;

//        mLocationView.setBackgroundColor(0xffebebeb); // The old background colour
        mLocationView.setBackgroundColor(0xAA40E0D0);
    }

    void setBackVisibility(int visibility) {
        mBackView.setVisibility(visibility);
    }


    void setZoomVisibility(int visibility) {
        mZoomInView.setVisibility(visibility);
        mZoomOutView.setVisibility(visibility);
    }

    void setAdjustVisibility(int visibility) {
        mErrorMessageLabel.setVisibility(visibility);
    }

    void setErrorVisibility(int visibility) {
        mErrorMessageLabel.setVisibility(visibility);
    }

    void setListeners(LocationView.Listener locationListener, View.OnLayoutChangeListener layoutListener) {
        mLocationView.setListener(locationListener);
        mLocationView.addOnLayoutChangeListener(layoutListener);
    }

    void setZoomParameters(float minZoomFactor, float maxZoomFactor) {
        mLocationView.setZoomRange(minZoomFactor, maxZoomFactor);
        mLocationView.setZoomFactor(minZoomFactor);
    }

    void setErrorMessage(String message)
    {
        mErrorMessageLabel.setText(message);
        mErrorMessageLabel.setVisibility(View.VISIBLE);
    }

    void redrawLocationView() {
        mLocationView.redraw();
    }

    void zoomLocationView(float zoom) {
        mLocationView.zoomBy(zoom);
    }

    PointF getAbsCoordinates(float x, float y) {
        return mLocationView.getAbsCoordinates(x, y);
    }

    PointF getScreenCoordinates(float x, float y) {
        return mLocationView.getScreenCoordinates(x, y);
    }

    PointF getScreenCoordinates(LocationPoint p) {
        return mLocationView.getScreenCoordinates(p);
    }

    float getLocationWidth() {
        return mLocationView.getWidth();
    }

    float getLocationHeight() {
        return mLocationView.getHeight();
    }

    float getScreenLengthX(float r) {
        return mLocationView.getScreenLengthX(r);
    }

    boolean loadSubLocation(SubLocation subLoc) {
        return mLocationView.loadSubLocation(subLoc);
    }
    void scrollByLocation(float deltaX, float deltaY) {
        mLocationView.scrollBy(deltaX, deltaY);
    }



}
