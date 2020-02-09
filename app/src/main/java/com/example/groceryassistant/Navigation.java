package com.example.groceryassistant;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.navigine.naviginesdk.DeviceInfo;
import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.LocationView;
import com.navigine.naviginesdk.NavigationThread;
import com.navigine.naviginesdk.NavigineSDK;
import com.navigine.naviginesdk.RoutePath;
import com.navigine.naviginesdk.SubLocation;
import com.navigine.naviginesdk.Venue;
import com.navigine.naviginesdk.Zone;

import java.io.File;
import java.util.Calendar;
import java.util.Locale;

public class Navigation extends Activity {

    public void toggleAdjustMode(View v)
    {
        MainActivity.mAdjustMode = !MainActivity.mAdjustMode;
        MainActivity.mAdjustTime = 0;
        Button adjustModeButton = (Button)findViewById(R.id.navigation__adjust_mode_button);
        adjustModeButton.setBackgroundResource(MainActivity.mAdjustMode ?
                R.drawable.btn_adjust_mode_on :
                R.drawable.btn_adjust_mode_off);
        MainActivity.mLocationView.redraw();
    }

    public void onZoomIn(View v)
    {
        MainActivity.mLocationView.zoomBy(1.25f);
    }

    public void onZoomOut(View v)
    {
        MainActivity.mLocationView.zoomBy(0.8f);
    }

    public void onMakeRoute(View v)
    {
        if (MainActivity.mNavigation == null)
            return;

        if (MainActivity.mPinPoint == null)
            return;

        MainActivity.mTargetPoint  = MainActivity.mPinPoint;
        MainActivity.mTargetVenue  = null;
        MainActivity.mPinPoint     = null;
        MainActivity.mPinPointRect = null;

        MainActivity.mNavigation.setTarget(MainActivity.mTargetPoint);
        MainActivity.mBackView.setVisibility(View.VISIBLE);
        MainActivity.mLocationView.redraw();
    }

    public void onCancelRoute(View v)
    {
        if (MainActivity.mNavigation == null)
            return;

        MainActivity.mTargetPoint  = null;
        MainActivity.mTargetVenue  = null;
        MainActivity.mPinPoint     = null;
        MainActivity.mPinPointRect = null;

        MainActivity.mNavigation.cancelTargets();
        MainActivity.mBackView.setVisibility(View.GONE);
        MainActivity.mLocationView.redraw();
    }

    public void handleClick(float x, float y)
    {
        Log.d(MainActivity.TAG, String.format(Locale.ENGLISH, "Click at (%.2f, %.2f)", x, y));

        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (MainActivity.mPinPoint != null)
        {
            if (MainActivity.mPinPointRect != null && MainActivity.mPinPointRect.contains(x, y))
            {
                MainActivity.mTargetPoint  = MainActivity.mPinPoint;
                MainActivity.mTargetVenue  = null;
                MainActivity.mPinPoint     = null;
                MainActivity.mPinPointRect = null;
                MainActivity.mNavigation.setTarget(MainActivity.mTargetPoint);
                MainActivity.mBackView.setVisibility(View.VISIBLE);
                return;
            }
            cancelPin();
            return;
        }

        if (MainActivity.mSelectedVenue != null)
        {
            if (MainActivity.mSelectedVenueRect != null && MainActivity.mSelectedVenueRect.contains(x, y))
            {
                MainActivity.mTargetVenue = MainActivity.mSelectedVenue;
                MainActivity.mTargetPoint = null;
                MainActivity.mNavigation.setTarget(new LocationPoint(MainActivity.mLocation.getId(), subLoc.getId(), MainActivity.mTargetVenue.getX(), MainActivity.mTargetVenue.getY()));
                MainActivity.mBackView.setVisibility(View.VISIBLE);
            }
            cancelVenue();
            return;
        }

        // Check if we touched venue
        MainActivity.mSelectedVenue = getVenueAt(x, y);
        MainActivity.mSelectedVenueRect = new RectF();

        // Check if we touched zone
        if (MainActivity.mSelectedVenue == null)
        {
            Zone Z = getZoneAt(x, y);
            if (Z != null)
                MainActivity.mSelectedZone = (MainActivity.mSelectedZone == Z) ? null : Z;
        }

        MainActivity.mLocationView.redraw();
    }

    public void onNav(float x, float y) {

        if (MainActivity.mNavigation == null)
            return;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);

        MainActivity.mTargetPoint  = new LocationPoint(MainActivity.mLocation.getId(), subLoc.getId(), x, y);
        MainActivity.mTargetVenue  = null;
        MainActivity.mPinPoint     = null;
        MainActivity.mPinPointRect = null;

        MainActivity.mNavigation.setTarget(MainActivity.mTargetPoint);
        MainActivity.mBackView.setVisibility(View.VISIBLE);
        MainActivity.mLocationView.redraw();

    }

    public void handleLongClick(float x, float y)
    {
        Log.d(MainActivity.TAG, String.format(Locale.ENGLISH, "Long click at (%.2f, %.2f)", x, y));
        makePin(MainActivity.mLocationView.getAbsCoordinates(x, y));
        cancelVenue();
    }

    public void handleScroll(float x, float y, boolean byTouchEvent)
    {
        if (byTouchEvent)
            MainActivity.mAdjustTime = NavigineSDK.currentTimeMillis() + MainActivity.ADJUST_TIMEOUT;
    }

    public void handleZoom(float ratio, boolean byTouchEvent)
    {
        if (byTouchEvent)
            MainActivity.mAdjustTime = NavigineSDK.currentTimeMillis() + MainActivity.ADJUST_TIMEOUT;
    }

    public void handleEnterZone(Zone z)
    {
        Log.d(MainActivity.TAG, "Enter zone " + z.getName());
        if (MainActivity.NOTIFICATIONS_ENABLED)
        {
            Intent notificationIntent = new Intent(this, NotificationActivity.class);
            notificationIntent.putExtra("zone_id",    z.getId());
            notificationIntent.putExtra("zone_name",  z.getName());
            notificationIntent.putExtra("zone_color", z.getColor());
            notificationIntent.putExtra("zone_alias", z.getAlias());

            // Setting up a notification
            Notification.Builder notificationBuilder = new Notification.Builder(this, MainActivity.NOTIFICATION_CHANNEL);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(this, z.getId(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
            notificationBuilder.setContentTitle("New zone");
            notificationBuilder.setContentText("You have entered zone '" + z.getName() + "'");
            notificationBuilder.setSmallIcon(R.drawable.elm_logo);
            notificationBuilder.setAutoCancel(true);

            // Posting a notification
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(z.getId(), notificationBuilder.build());
        }
    }

    public void handleLeaveZone(Zone z)
    {
        Log.d(MainActivity.TAG, "Leave zone " + z.getName());
        if (MainActivity.NOTIFICATIONS_ENABLED)
        {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(z.getId());
        }
    }

    public void handleDeviceUpdate(DeviceInfo deviceInfo)
    {
        // TO DO : Use path array to get the next turn
        //float l = deviceInfo.getPaths().get(0).getLength();

        if (!(MainActivity.mTargetPoint == null)) {

            float dx = deviceInfo.getX() - MainActivity.mTargetPoint.getX();
            float dy = deviceInfo.getY() - MainActivity.mTargetPoint.getY();
            double d = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
            Log.d(MainActivity.TAG, String.format("dx: %f, dy: %f, d: %f", dx, dy, d));


            if (d < 1) {

                MainActivity.mTargetPoint  = null;
                MainActivity.mTargetVenue  = null;
                MainActivity.mPinPoint     = null;
                MainActivity.mPinPointRect = null;

                MainActivity.mNavigation.cancelTargets();
                MainActivity.mBackView.setVisibility(View.GONE);
                MainActivity.mLocationView.redraw();

                if (MainActivity.navItem != null) {
                    MainActivity.textToSpeech.speak("You have arrived at the " + MainActivity.navItem.getName(), MainActivity.textToSpeech.QUEUE_FLUSH, null);
                } else {
                    MainActivity.textToSpeech.speak("You have arrived", MainActivity.textToSpeech.QUEUE_FLUSH, null);
                }
                MainActivity.navItem = null;
                Log.d(MainActivity.TAG, "Navigation Ended");
            }
        }

        MainActivity.mDeviceInfo = deviceInfo;
        if (MainActivity.mDeviceInfo == null)
            return;

        // Check if location is loaded
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        if (MainActivity.mDeviceInfo.isValid())
        {
            cancelErrorMessage();
            MainActivity.mBackView.setVisibility(MainActivity.mTargetPoint != null || MainActivity.mTargetVenue != null ?
                    View.VISIBLE : View.GONE);
            if (MainActivity.mAdjustMode)
                adjustDevice();
        }
        else
        {
            MainActivity.mBackView.setVisibility(View.GONE);
            switch (MainActivity.mDeviceInfo.getErrorCode())
            {
                case 4:
                    setErrorMessage("You are out of navigation zone! Please, check that your bluetooth is enabled!");
                    break;

                case 8:
                case 30:
                    setErrorMessage("Not enough beacons on the location! Please, add more beacons!");
                    break;

                default:
                    setErrorMessage(String.format(Locale.ENGLISH,
                            "Something is wrong with location '%s' (error code %d)! " +
                                    "Please, contact technical support!",
                            MainActivity.mLocation.getName(), MainActivity.mDeviceInfo.getErrorCode()));
                    break;
            }
        }

        // This causes map redrawing
        MainActivity.mLocationView.redraw();
    }

    public void setErrorMessage(String message)
    {
        MainActivity.mErrorMessageLabel.setText(message);
        MainActivity.mErrorMessageLabel.setVisibility(View.VISIBLE);
    }

    public void cancelErrorMessage()
    {
        MainActivity.mErrorMessageLabel.setVisibility(View.GONE);
    }

    public boolean loadMap()
    {
        if (MainActivity.mNavigation == null)
        {
            Log.e(MainActivity.TAG, "Can't load map! Navigine SDK is not available!");
            return false;
        }

        MainActivity.mLocation = MainActivity.mNavigation.getLocation();
        MainActivity.mCurrentSubLocationIndex = -1;

        if (MainActivity.mLocation == null)
        {
            Log.e(MainActivity.TAG, "Loading map failed: no location");
            return false;
        }

        if (MainActivity.mLocation.getSubLocations().size() == 0)
        {
            Log.e(MainActivity.TAG, "Loading map failed: no sublocations");
            MainActivity.mLocation = null;
            return false;
        }

        if (!loadSubLocation(0))
        {
            Log.e(MainActivity.TAG, "Loading map failed: unable to load default sublocation");
            MainActivity.mLocation = null;
            return false;
        }


        MainActivity.mZoomInView.setVisibility(View.VISIBLE);
        MainActivity.mZoomOutView.setVisibility(View.VISIBLE);
        MainActivity.mAdjustModeView.setVisibility(View.VISIBLE);

        MainActivity.mNavigation.setMode(NavigationThread.MODE_NORMAL);

        if (D.WRITE_LOGS)
            MainActivity.mNavigation.setLogFile(getLogFile("log"));

        MainActivity.mLocationView.redraw();
        return true;
    }

    public boolean loadSubLocation(int index)
    {
        if (MainActivity.mNavigation == null)
            return false;

        if (MainActivity.mLocation == null || index < 0 || index >= MainActivity.mLocation.getSubLocations().size())
            return false;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(index);
        Log.d(MainActivity.TAG, String.format(Locale.ENGLISH, "Loading sublocation %s (%.2f x %.2f)", subLoc.getName(), subLoc.getWidth(), subLoc.getHeight()));

        if (subLoc.getWidth() < 1.0f || subLoc.getHeight() < 1.0f)
        {
            Log.e(MainActivity.TAG, String.format(Locale.ENGLISH, "Loading sublocation failed: invalid size: %.2f x %.2f", subLoc.getWidth(), subLoc.getHeight()));
            return false;
        }

        if (!MainActivity.mLocationView.loadSubLocation(subLoc))
        {
            Log.e(MainActivity.TAG, "Loading sublocation failed: invalid image");
            return false;
        }

        float viewWidth  = MainActivity.mLocationView.getWidth();
        float viewHeight = MainActivity.mLocationView.getHeight();
        float minZoomFactor = Math.min(viewWidth / subLoc.getWidth(), viewHeight / subLoc.getHeight());
        float maxZoomFactor = LocationView.ZOOM_FACTOR_MAX;
        MainActivity.mLocationView.setZoomRange(minZoomFactor, maxZoomFactor);
        MainActivity.mLocationView.setZoomFactor(minZoomFactor);
        Log.d(MainActivity.TAG, String.format(Locale.ENGLISH, "View size: %.1f x %.1f", viewWidth, viewHeight));

        MainActivity.mAdjustTime = 0;
        MainActivity.mCurrentSubLocationIndex = index;

        cancelVenue();
        MainActivity.mLocationView.redraw();
        return true;
    }

    public boolean loadNextSubLocation()
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return false;
        return loadSubLocation(MainActivity.mCurrentSubLocationIndex + 1);
    }

    public boolean loadPrevSubLocation()
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return false;
        return loadSubLocation(MainActivity.mCurrentSubLocationIndex - 1);
    }

    public void makePin(PointF P)
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (P.x < 0.0f || P.x > subLoc.getWidth() ||
                P.y < 0.0f || P.y > subLoc.getHeight())
        {
            // Missing the map
            return;
        }

        if (MainActivity.mTargetPoint != null || MainActivity.mTargetVenue != null)
            return;

        if (MainActivity.mDeviceInfo == null || !MainActivity.mDeviceInfo.isValid())
            return;

        MainActivity.mPinPoint = new LocationPoint(MainActivity.mLocation.getId(), subLoc.getId(), P.x, P.y);
        MainActivity.mPinPointRect = new RectF();
        MainActivity.mLocationView.redraw();
    }

    public void cancelPin()
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (MainActivity.mTargetPoint != null || MainActivity.mTargetVenue != null || MainActivity.mPinPoint == null)
            return;

        MainActivity.mPinPoint = null;
        MainActivity.mPinPointRect = null;
        MainActivity.mLocationView.redraw();
    }

    public void cancelVenue()
    {
        MainActivity.mSelectedVenue = null;
        MainActivity.mLocationView.redraw();
    }

    public Venue getVenueAt(float x, float y)
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return null;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
        if (subLoc == null)
            return null;

        Venue v0 = null;
        float d0 = 1000.0f;

        for(int i = 0; i < subLoc.getVenues().size(); ++i)
        {
            Venue v = subLoc.getVenues().get(i);
            PointF P = MainActivity.mLocationView.getScreenCoordinates(v.getX(), v.getY());
            float d = Math.abs(x - P.x) + Math.abs(y - P.y);
            if (d < 30.0f * MainActivity.mDisplayDensity && d < d0)
            {
                v0 = v;
                d0 = d;
            }
        }

        return v0;
    }

    public Zone getZoneAt(float x, float y)
    {
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return null;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
        if (subLoc == null)
            return null;

        PointF P = MainActivity.mLocationView.getAbsCoordinates(x, y);
        LocationPoint LP = new LocationPoint(MainActivity.mLocation.getId(), subLoc.getId(), P.x, P.y);

        for(int i = 0; i < subLoc.getZones().size(); ++i)
        {
            Zone Z = subLoc.getZones().get(i);
            if (Z.contains(LP))
                return Z;
        }
        return null;
    }

    public void drawPoints(Canvas canvas)
    {
        // Check if location is loaded
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        // Get current sublocation displayed
        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);

        if (subLoc == null)
            return;

        final int solidColor  = Color.argb(255, 64, 163, 205);  // Light-blue color
        final int circleColor = Color.argb(127, 64, 163, 205);  // Semi-transparent light-blue color
        final int arrowColor  = Color.argb(255, 255, 255, 255); // White color
        final float dp        = MainActivity.mDisplayDensity;
        final float textSize  = 16 * dp;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Drawing pin point (if it exists and belongs to the current sublocation)
        if (MainActivity.mPinPoint != null && MainActivity.mPinPoint.subLocation == subLoc.getId())
        {
            final PointF T = MainActivity.mLocationView.getScreenCoordinates(MainActivity.mPinPoint);
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

            MainActivity.mPinPointRect.set(x0 - w/2, y0 - h/2, x0 + w/2, y0 + h/2);

            paint.setColor(solidColor);
            canvas.drawRoundRect(MainActivity.mPinPointRect, h/2, h/2, paint);

            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(text, x0 - textWidth/2, y0 + textSize/4, paint);
        }

        // Drawing target point (if it exists and belongs to the current sublocation)
        if (MainActivity.mTargetPoint != null && MainActivity.mTargetPoint.subLocation == subLoc.getId())
        {
            final PointF T = MainActivity.mLocationView.getScreenCoordinates(MainActivity.mTargetPoint);
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
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);

        final float dp = MainActivity.mDisplayDensity;
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

            final PointF P = MainActivity.mLocationView.getScreenCoordinates(v.getX(), v.getY());
            final float x0 = P.x - venueSize/2;
            final float y0 = P.y - venueSize/2;
            final float x1 = P.x + venueSize/2;
            final float y1 = P.y + venueSize/2;
            canvas.drawBitmap(MainActivity.mVenueBitmap, null, new RectF(x0, y0, x1, y1), paint);
        }

        if (MainActivity.mSelectedVenue != null)
        {
            final PointF T = MainActivity.mLocationView.getScreenCoordinates(MainActivity.mSelectedVenue.getX(), MainActivity.mSelectedVenue.getY());
            final float textWidth = paint.measureText(MainActivity.mSelectedVenue.getName());

            final float h  = 50 * dp;
            final float w  = Math.max(120 * dp, textWidth + h/2);
            final float x0 = T.x;
            final float y0 = T.y - 50 * dp;
            MainActivity.mSelectedVenueRect.set(x0 - w/2, y0 - h/2, x0 + w/2, y0 + h/2);

            paint.setColor(venueColor);
            canvas.drawRoundRect(MainActivity.mSelectedVenueRect, h/2, h/2, paint);

            paint.setARGB(255, 255, 255, 255);
            canvas.drawText(MainActivity.mSelectedVenue.getName(), x0 - textWidth/2, y0 + textSize/4, paint);
        }
    }

    public void drawZones(Canvas canvas)
    {
        // Check if location is loaded
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        // Get current sublocation displayed
        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
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

            boolean selected = (Z == MainActivity.mSelectedZone);

            Path path = new Path();
            final LocationPoint P0 = Z.getPoints().get(0);
            final PointF        Q0 = MainActivity.mLocationView.getScreenCoordinates(P0);
            path.moveTo(Q0.x, Q0.y);

            for(int j = 0; j < Z.getPoints().size(); ++j)
            {
                final LocationPoint P = Z.getPoints().get((j + 1) % Z.getPoints().size());
                final PointF        Q = MainActivity.mLocationView.getScreenCoordinates(P);
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
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        // Check if navigation is available
        if (MainActivity.mDeviceInfo == null || !MainActivity.mDeviceInfo.isValid())
            return;

        // Get current sublocation displayed
        SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);

        if (subLoc == null)
            return;

        final int solidColor  = Color.argb(255, 64,  163, 205); // Light-blue color
        final int circleColor = Color.argb(127, 64,  163, 205); // Semi-transparent light-blue color
        final int arrowColor  = Color.argb(255, 255, 255, 255); // White color
        final float dp = MainActivity.mDisplayDensity;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);

        /// Drawing device path (if it exists)
        if (MainActivity.mDeviceInfo.getPaths() != null && MainActivity.mDeviceInfo.getPaths().size() > 0)
        {
            RoutePath path = MainActivity.mDeviceInfo.getPaths().get(0);
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
                        PointF P1 = MainActivity.mLocationView.getScreenCoordinates(P);
                        PointF Q1 = MainActivity.mLocationView.getScreenCoordinates(Q);
                        canvas.drawLine(P1.x, P1.y, Q1.x, Q1.y, paint);
                    }
                }
            }
        }

        paint.setStrokeCap(Paint.Cap.BUTT);

        // Check if device belongs to the current sublocation
        if (MainActivity.mDeviceInfo.getSubLocationId() != subLoc.getId())
            return;

        final float x  = MainActivity.mDeviceInfo.getX();
        final float y  = MainActivity.mDeviceInfo.getY();
        final float r  = MainActivity.mDeviceInfo.getR();
        final float angle = MainActivity.mDeviceInfo.getAzimuth();
        final float sinA = (float)Math.sin(angle);
        final float cosA = (float)Math.cos(angle);
        final float radius  = MainActivity.mLocationView.getScreenLengthX(r);  // External radius: navigation-determined, transparent
        final float radius1 = 25 * dp;                            // Internal radius: fixed, solid

        PointF O = MainActivity.mLocationView.getScreenCoordinates(x, y);
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

        if (MainActivity.ORIENTATION_ENABLED)
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

    public void adjustDevice()
    {
        // Check if location is loaded
        if (MainActivity.mLocation == null || MainActivity.mCurrentSubLocationIndex < 0)
            return;

        // Check if navigation is available
        if (MainActivity.mDeviceInfo == null || !MainActivity.mDeviceInfo.isValid())
            return;

        long timeNow = System.currentTimeMillis();

        // Adjust map, if necessary
        if (timeNow >= MainActivity.mAdjustTime)
        {
            // Firstly, set the correct sublocation
            SubLocation subLoc = MainActivity.mLocation.getSubLocations().get(MainActivity.mCurrentSubLocationIndex);
            if (MainActivity.mDeviceInfo.getSubLocationId() != subLoc.getId())
            {
                for(int i = 0; i < MainActivity.mLocation.getSubLocations().size(); ++i)
                    if (MainActivity.mLocation.getSubLocations().get(i).getId() == MainActivity.mDeviceInfo.getSubLocationId())
                        loadSubLocation(i);
            }

            // Secondly, adjust device to the center of the screen
            PointF center = MainActivity.mLocationView.getScreenCoordinates(MainActivity.mDeviceInfo.getX(), MainActivity.mDeviceInfo.getY());
            float deltaX  = MainActivity.mLocationView.getWidth()  / 2 - center.x;
            float deltaY  = MainActivity.mLocationView.getHeight() / 2 - center.y;
            MainActivity.mAdjustTime   = timeNow;
            MainActivity.mLocationView.scrollBy(deltaX, deltaY);
        }
    }

    public static String getLogFile(String extension)
    {
        try
        {
            final String extDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/Navigine.Demo";
            (new File(extDir)).mkdirs();
            if (!(new File(extDir)).exists())
                return null;

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());

            return String.format(Locale.ENGLISH, "%s/%04d%02d%02d_%02d%02d%02d.%s", extDir,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    calendar.get(Calendar.SECOND),
                    extension);
        }
        catch (Throwable e)
        {
            return null;
        }
    }

}
