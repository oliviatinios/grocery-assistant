package com.example.groceryassistant;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.LocationView;
import com.navigine.naviginesdk.NavigineSDK;
import com.navigine.naviginesdk.RoutePath;
import com.navigine.naviginesdk.SubLocation;
import com.navigine.naviginesdk.Venue;
import com.navigine.naviginesdk.Zone;

public class Display {

    private static final String TAG = "DISPLAY";
    private static final int ADJUST_TIMEOUT = 5000; // milliseconds
    private static final boolean  ORIENTATION_ENABLED     = true; // Show device orientation?



    // UI Parameters
    //private LocationView mLocationView;
    private View mBackView;
    private View mZoomInView;
    private View mZoomOutView;
    private View mAdjustModeView;
    private TextView mErrorMessageLabel;
    private Handler mHandler;
    private float mDisplayDensity;

    private boolean mAdjustMode;
    private long mAdjustTime;

    public Display() {
        // UI Parameters
        //LocationView mLocationView = null;
        View mBackView = null;
        View mZoomInView = null;
        View mZoomOutView = null;
        View mAdjustModeView = null;
        TextView mErrorMessageLabel = null;
        Handler mHandler = new Handler();
        mDisplayDensity = 0.0f;

        mAdjustMode = false;
        mAdjustTime = 0;


    }

    public boolean getAdjustMode() {
        return mAdjustMode;
    }

    public void setBackView(View v) {
        mBackView = v;
        mBackView.setVisibility(View.INVISIBLE);
    }
    public void setZoomInView(View v) {
        mBackView = v;
        mZoomInView.setVisibility(View.INVISIBLE);
    }
    public void setZoomOutView(View v) {
        mBackView = v;
        mZoomOutView.setVisibility(View.INVISIBLE);
    }
    public void setAdjustModeView(View v) {
        mBackView = v;
        mAdjustModeView.setVisibility(View.INVISIBLE);
    }
    public void setErrorMessageLabel(View v) {
        mBackView = v;
        mErrorMessageLabel.setVisibility(View.GONE);
    }

    public void setDisplayDensity(float density) {
        mDisplayDensity = density;
    }

    public void toggleAdjustMode() {
        mAdjustMode = !mAdjustMode;
        mAdjustTime = 0;
    }

    public void handleScroll(float x, float y, boolean byTouchEvent)
    {
        if (byTouchEvent)
            mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
    }

    public void handleZoom(float ratio, boolean byTouchEvent)
    {
        if (byTouchEvent)
            mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
    }

    public void drawPoints(Canvas canvas)
    {
        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        // Get current sublocation displayed
        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        if (subLoc == null)
            return;

        final int solidColor  = Color.argb(255, 64, 163, 205);  // Light-blue color
        final int circleColor = Color.argb(127, 64, 163, 205);  // Semi-transparent light-blue color
        final int arrowColor  = Color.argb(255, 255, 255, 255); // White color
        final float dp        = mDisplayDensity;
        final float textSize  = 16 * dp;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Drawing pin point (if it exists and belongs to the current sublocation)
        if (mPinPoint != null && mPinPoint.subLocation == subLoc.getId())
        {
            final PointF T = mLocationView.getScreenCoordinates(mPinPoint);
            final float tRadius = 10 * dp;

            paint.setARGB(255, 0, 0, 0);
            paint.setStrokeWidth(4 * dp);
            canvas.drawLine(T.x, T.y, T.x, T.y - 3 * tRadius, paint);

            paint.setColor(solidColor);
            paint.setStrokeWidth(0);
            canvas.drawCircle(T.x, T.y - 3 * tRadius, tRadius, paint);

            final String text = "Make route";
            final float textWidth = paint.measureText(text);
            final float h  = 50 * dp;
            final float w  = Math.max(120 * dp, textWidth + h/2);
            final float x0 = T.x;
            final float y0 = T.y - 75 * dp;

            mPinPointRect.set(x0 - w/2, y0 - h/2, x0 + w/2, y0 + h/2);

            paint.setColor(solidColor);
            canvas.drawRoundRect(mPinPointRect, h/2, h/2, paint);

            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(text, x0 - textWidth/2, y0 + textSize/4, paint);
        }

        // Drawing target point (if it exists and belongs to the current sublocation)
        if (mTargetPoint != null && mTargetPoint.subLocation == subLoc.getId())
        {
            final PointF T = mLocationView.getScreenCoordinates(mTargetPoint);
            final float tRadius = 10 * dp;

            paint.setARGB(255, 0, 0, 0);
            paint.setStrokeWidth(4 * dp);
            canvas.drawLine(T.x, T.y, T.x, T.y - 3 * tRadius, paint);

            paint.setColor(solidColor);
            canvas.drawCircle(T.x, T.y - 3 * tRadius, tRadius, paint);
        }
    }

    public void drawVenues(Canvas canvas)
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        final float dp = mDisplayDensity;
        final float textSize  = 16 * dp;
        final float venueSize = 30 * dp;
        final int   venueColor = Color.argb(255, 0xCD, 0x88, 0x50); // Venue color

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(0);
        paint.setColor(venueColor);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        for(int i = 0; i < subLoc.getVenues().size(); ++i)
        {
            Venue v = subLoc.getVenues().get(i);
            if (v.getSubLocationId() != subLoc.getId())
                continue;

            final PointF P = mLocationView.getScreenCoordinates(v.getX(), v.getY());
            final float x0 = P.x - venueSize/2;
            final float y0 = P.y - venueSize/2;
            final float x1 = P.x + venueSize/2;
            final float y1 = P.y + venueSize/2;
            canvas.drawBitmap(mVenueBitmap, null, new RectF(x0, y0, x1, y1), paint);
        }

        if (mSelectedVenue != null)
        {
            final PointF T = mLocationView.getScreenCoordinates(mSelectedVenue.getX(), mSelectedVenue.getY());
            final float textWidth = paint.measureText(mSelectedVenue.getName());

            final float h  = 50 * dp;
            final float w  = Math.max(120 * dp, textWidth + h/2);
            final float x0 = T.x;
            final float y0 = T.y - 50 * dp;
            mSelectedVenueRect.set(x0 - w/2, y0 - h/2, x0 + w/2, y0 + h/2);

            paint.setColor(venueColor);
            canvas.drawRoundRect(mSelectedVenueRect, h/2, h/2, paint);

            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(mSelectedVenue.getName(), x0 - textWidth/2, y0 + textSize/4, paint);
        }
    }

    public void drawZones(Canvas canvas)
    {
        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        // Get current sublocation displayed
        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        for(int i = 0; i < subLoc.getZones().size(); ++i)
        {
            Zone Z = subLoc.getZones().get(i);
            if (Z.getPoints().size() < 3)
                continue;

            boolean selected = (Z == mSelectedZone);

            Path path = new Path();
            final LocationPoint P0 = Z.getPoints().get(0);
            final PointF        Q0 = mLocationView.getScreenCoordinates(P0);
            path.moveTo(Q0.x, Q0.y);

            for(int j = 0; j < Z.getPoints().size(); ++j)
            {
                final LocationPoint P = Z.getPoints().get((j + 1) % Z.getPoints().size());
                final PointF        Q = mLocationView.getScreenCoordinates(P);
                path.lineTo(Q.x, Q.y);
            }

            int zoneColor = Color.parseColor(Z.getColor());
            int red       = (zoneColor >> 16) & 0xff;
            int green     = (zoneColor >> 8 ) & 0xff;
            int blue      = (zoneColor >> 0 ) & 0xff;
            paint.setColor(Color.argb(selected ? 200 : 100, red, green, blue));
            canvas.drawPath(path, paint);
        }
    }

    public void drawDevice(Canvas canvas)
    {
        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        // Check if navigation is available
        if (mDeviceInfo == null || !mDeviceInfo.isValid())
            return;

        // Get current sublocation displayed
        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        if (subLoc == null)
            return;

        final int solidColor  = Color.argb(255, 64,  163, 205); // Light-blue color
        final int circleColor = Color.argb(127, 64,  163, 205); // Semi-transparent light-blue color
        final int arrowColor  = Color.argb(255, 255, 255, 255); // White color
        final float dp = mDisplayDensity;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        /// Drawing device path (if it exists)
        if (mDeviceInfo.getPaths() != null && mDeviceInfo.getPaths().size() > 0)
        {
            RoutePath path = mDeviceInfo.getPaths().get(0);
            if (path.getPoints().size() >= 2)
            {
                paint.setColor(solidColor);

                for(int j = 1; j < path.getPoints().size(); ++j)
                {
                    LocationPoint P = path.getPoints().get(j-1);
                    LocationPoint Q = path.getPoints().get(j);
                    if (P.subLocation == subLoc.getId() && Q.subLocation == subLoc.getId())
                    {
                        paint.setStrokeWidth(3 * dp);
                        PointF P1 = mLocationView.getScreenCoordinates(P);
                        PointF Q1 = mLocationView.getScreenCoordinates(Q);
                        canvas.drawLine(P1.x, P1.y, Q1.x, Q1.y, paint);
                    }
                }
            }
        }

        paint.setStrokeCap(Paint.Cap.BUTT);

        // Check if device belongs to the current sublocation
        if (mDeviceInfo.getSubLocationId() != subLoc.getId())
            return;

        final float x  = mDeviceInfo.getX();
        final float y  = mDeviceInfo.getY();
        final float r  = mDeviceInfo.getR();
        final float angle = mDeviceInfo.getAzimuth();
        final float sinA = (float)Math.sin(angle);
        final float cosA = (float)Math.cos(angle);
        final float radius  = mLocationView.getScreenLengthX(r);  // External radius: navigation-determined, transparent
        final float radius1 = 25 * dp;                            // Internal radius: fixed, solid

        PointF O = mLocationView.getScreenCoordinates(x, y);
        PointF P = new PointF(O.x - radius1 * sinA * 0.22f, O.y + radius1 * cosA * 0.22f);
        PointF Q = new PointF(O.x + radius1 * sinA * 0.55f, O.y - radius1 * cosA * 0.55f);
        PointF R = new PointF(O.x + radius1 * cosA * 0.44f - radius1 * sinA * 0.55f, O.y + radius1 * sinA * 0.44f + radius1 * cosA * 0.55f);
        PointF S = new PointF(O.x - radius1 * cosA * 0.44f - radius1 * sinA * 0.55f, O.y - radius1 * sinA * 0.44f + radius1 * cosA * 0.55f);

        // Drawing transparent circle
        paint.setStrokeWidth(0);
        paint.setColor(circleColor);
        canvas.drawCircle(O.x, O.y, radius, paint);

        // Drawing solid circle
        paint.setColor(solidColor);
        canvas.drawCircle(O.x, O.y, radius1, paint);

        if (ORIENTATION_ENABLED)
        {
            // Drawing arrow
            paint.setColor(arrowColor);
            Path path = new Path();
            path.moveTo(Q.x, Q.y);
            path.lineTo(R.x, R.y);
            path.lineTo(P.x, P.y);
            path.lineTo(S.x, S.y);
            path.lineTo(Q.x, Q.y);
            canvas.drawPath(path, paint);
        }
    }


}
