package com.example.groceryassistant;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import com.navigine.naviginesdk.DeviceInfo;
import com.navigine.naviginesdk.Location;
import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.NavigationThread;
import com.navigine.naviginesdk.NavigineSDK;
import com.navigine.naviginesdk.SubLocation;
import com.navigine.naviginesdk.Venue;
import com.navigine.naviginesdk.Zone;

import java.util.Locale;


public class Navigation {

    private static final String TAG = "NAVIGATION";

    // NavigationThread instance
    private NavigationThread mNavigation;

    // Location parameters
    private Location mLocation;
    private int      mCurrentSubLocationIndex;

    // Device parameters
    private DeviceInfo  mDeviceInfo; // Current device
    private LocationPoint mPinPoint; // Potential device target
    private LocationPoint mTargetPoint; // Current device target
    private RectF         mPinPointRect;

    private Bitmap  mVenueBitmap;
    private Venue   mTargetVenue;
    private Venue   mSelectedVenue;
    private RectF   mSelectedVenueRect;
    private Zone    mSelectedZone;

    public Navigation() {

        // NavigationThread instance
        this.mNavigation = null;

        // Location parameters
        this.mLocation = null;
        this.mCurrentSubLocationIndex = -1;

        // Device parameters
        this.mDeviceInfo    = null; // Current device
        this.mPinPoint      = null; // Potential device target
        this.mTargetPoint   = null; // Current device target
        this.mPinPointRect  = null;

        this.mVenueBitmap       = null;
        this.mTargetVenue       = null;
        this.mSelectedVenue     = null;
        this.mSelectedVenueRect = null;
        this.mSelectedZone      = null;

        mNavigation = NavigineSDK.getNavigation();

        // Setting up device listener
        if (mNavigation != null)
        {
            mNavigation.setDeviceListener
                    (
                            new DeviceInfo.Listener()
                            {
                                @Override public void onUpdate(DeviceInfo info) { handleDeviceUpdate(info); }
                            }
                    );
        }

        // Setting up zone listener
        if (mNavigation != null)
        {
            mNavigation.setZoneListener
                    (
                            new Zone.Listener()
                            {
                                @Override public void onEnterZone(Zone z) { handleEnterZone(z); }
                                @Override public void onLeaveZone(Zone z) { handleLeaveZone(z); }
                            }
                    );
        }


    }

    public void setVenueBitmap(Resources r) {
        mVenueBitmap = BitmapFactory.decodeResource(r, R.drawable.elm_venue);
    }

    public void kill() {
        if (mNavigation != null)
        {
            NavigineSDK.finish();
            mNavigation = null;
        }
    }

    public void onMakeRoute(View v)
    {
        if (mNavigation == null)
            return;

        if (mPinPoint == null)
            return;

        mTargetPoint  = mPinPoint;
        mTargetVenue  = null;
        mPinPoint     = null;
        mPinPointRect = null;

        mNavigation.setTarget(mTargetPoint);

        MainActivity.setBackView(View.VISIBLE);
    }

    public void onCancelRoute(View v)
    {
        if (mNavigation == null)
            return;

        mTargetPoint  = null;
        mTargetVenue  = null;
        mPinPoint     = null;
        mPinPointRect = null;

        mNavigation.cancelTargets();
        mBackView.setVisibility(View.GONE);
        mLocationView.redraw();
    }

    public void handleClick(float x, float y)
    {
        Log.d(TAG, String.format(Locale.ENGLISH, "Click at (%.2f, %.2f)", x, y));

        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (mPinPoint != null)
        {
            if (mPinPointRect != null && mPinPointRect.contains(x, y))
            {
                mTargetPoint  = mPinPoint;
                mTargetVenue  = null;
                mPinPoint     = null;
                mPinPointRect = null;
                mNavigation.setTarget(mTargetPoint);
                mBackView.setVisibility(View.VISIBLE);
                return;
            }
            cancelPin();
            return;
        }

        if (mSelectedVenue != null)
        {
            if (mSelectedVenueRect != null && mSelectedVenueRect.contains(x, y))
            {
                mTargetVenue = mSelectedVenue;
                mTargetPoint = null;
                mNavigation.setTarget(new LocationPoint(mLocation.getId(), subLoc.getId(), mTargetVenue.getX(), mTargetVenue.getY()));
                mBackView.setVisibility(View.VISIBLE);
            }
            cancelVenue();
            return;
        }

        // Check if we touched venue
        mSelectedVenue = getVenueAt(x, y);
        mSelectedVenueRect = new RectF();

        // Check if we touched zone
        if (mSelectedVenue == null)
        {
            Zone Z = getZoneAt(x, y);
            if (Z != null)
                mSelectedZone = (mSelectedZone == Z) ? null : Z;
        }

        mLocationView.redraw();
    }

    public void handleLongClick(float x, float y)
    {
        Log.d(TAG, String.format(Locale.ENGLISH, "Long click at (%.2f, %.2f)", x, y));
        makePin(mLocationView.getAbsCoordinates(x, y));
        cancelVenue();
    }

    public void onNav(float x, float y) {

        if (mNavigation == null)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        mTargetPoint  = new LocationPoint(mLocation.getId(), subLoc.getId(), x, y);
        mTargetVenue  = null;
        mPinPoint     = null;
        mPinPointRect = null;

        mNavigation.setTarget(mTargetPoint);
        mBackView.setVisibility(View.VISIBLE);
        mLocationView.redraw();

    }

}
