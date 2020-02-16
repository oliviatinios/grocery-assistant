package com.example.groceryassistant;

import com.navigine.naviginesdk.DeviceInfo;
import com.navigine.naviginesdk.Location;
import com.navigine.naviginesdk.LocationPoint;
import com.navigine.naviginesdk.NavigationThread;
import com.navigine.naviginesdk.NavigineSDK;
import com.navigine.naviginesdk.Zone;

class Navigation {

    // NavigationThread instance
    private NavigationThread mNavigation;

    private boolean mAdjustMode;
    private long mAdjustTime;

    Navigation() {
        this.mNavigation = NavigineSDK.getNavigation();
        mAdjustMode = false;
        mAdjustTime = 0;
    }

    void setListeners(DeviceInfo.Listener deviceListener, Zone.Listener zoneListener) {
        if (mNavigation != null) {
            mNavigation.setDeviceListener(deviceListener);
            mNavigation.setZoneListener(zoneListener);
        }
    }

    boolean isNull() {
        return mNavigation == null;
    }

    void makeNull() {
        mNavigation = null;
    }

    void cancelTargets() {
        mNavigation.cancelTargets();
    }

    void setTarget(LocationPoint target) {
        mNavigation.setTarget(target);
    }

    Location getLocation() {
        return mNavigation.getLocation();
    }

    boolean getMode() { return mAdjustMode; }

    void setMode(int mode) {
        mNavigation.setMode(mode);
    }

    void setLogFile(String file) {
        mNavigation.setLogFile(file);
    }

    void toggleMode() {
        mAdjustMode = !mAdjustMode;
    }

    long getTime() { return mAdjustTime; }

    void setTime(long time) {
        mAdjustTime = time;
    }
}
