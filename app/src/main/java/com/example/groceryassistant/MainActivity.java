package com.example.groceryassistant;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.navigine.naviginesdk.DeviceInfo;
import com.navigine.naviginesdk.Location;
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
import java.util.Map;

public class MainActivity extends Activity {

    private static final String   TAG                     = "MAIN_ACTIVITY";
    private static final String   NOTIFICATION_CHANNEL    = "GROCERYASSISTANT_NOTIFICATION_CHANNEL";
    private static final int      ADJUST_TIMEOUT          = 5000; // milliseconds
    private static final boolean  ORIENTATION_ENABLED     = true; // Show device orientation?
    private static final boolean  NOTIFICATIONS_ENABLED   = true; // Show zone notifications?

    // NavigationThread instance
    private NavigationThread mNavigation            = null;

    private Display gui;

    private boolean       mAdjustMode               = false;
    private long          mAdjustTime               = 0;

    // Location parameters
    private Location      mLocation                 = null;
    private int           mCurrentSubLocationIndex  = -1;

    // Device parameters
    private DeviceInfo    mDeviceInfo               = null; // Current device
    private LocationPoint mPinPoint                 = null; // Potential device target
    private LocationPoint mTargetPoint              = null; // Current device target
    private RectF         mPinPointRect             = null;

    private Bitmap  mVenueBitmap    = null;
    private Venue   mTargetVenue    = null;
    private Venue   mSelectedVenue  = null;
    private RectF   mSelectedVenueRect = null;
    private Zone    mSelectedZone   = null;

    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    // Declare a DynamoDBMapper object
    private DynamoDBMapper dynamoDBMapper;

    // Text to speech
    private TextToSpeech textToSpeech;
    private Item navItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity started");

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        gui = new Display(
                (LocationView)findViewById(R.id.navigation__location_view),
                findViewById(R.id.navigation__back_view),
                findViewById(R.id.navigation__zoom_in_view),
                findViewById(R.id.navigation__zoom_out_view),
                findViewById(R.id.navigation__adjust_mode_view),
                (TextView)findViewById(R.id.navigation__error_message_label)
        );

        gui.setBackVisibility(View.INVISIBLE);
        gui.setZoomVisibility(View.INVISIBLE);

        gui.setAdjustVisibility(View.INVISIBLE);
        gui.setErrorVisibility(View.GONE);

        mVenueBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.elm_venue);

        LocationView.Listener locationListener = new LocationView.Listener()
        {
            @Override public void onClick     ( float x, float y ) { handleClick(x, y);     }
            @Override public void onLongClick ( float x, float y ) { handleLongClick(x, y); }
            @Override public void onScroll    ( float x, float y, boolean byTouchEvent ) { handleScroll ( x, y,  byTouchEvent ); }
            @Override public void onZoom      ( float ratio,      boolean byTouchEvent ) { handleZoom   ( ratio, byTouchEvent ); }

            @Override public void onDraw(Canvas canvas)
            {
                drawZones(canvas);
                drawPoints(canvas);
                drawVenues(canvas);
                drawDevice(canvas);
            }
        };

        // Loading map only when location view size is known
        OnLayoutChangeListener layoutListener = new OnLayoutChangeListener()
        {
            @Override public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
            {
                int width  = right  - left;
                int height = bottom - top;
                if (width == 0 || height == 0)
                    return;

                Log.d(TAG, "Layout chaged: " + width + "x" + height);

                int oldWidth  = oldRight  - oldLeft;
                int oldHeight = oldBottom - oldTop;
                if (oldWidth != width || oldHeight != height)
                    loadMap();
            }
        };

        gui.setListeners(locationListener, layoutListener);

        gui.mDisplayDensity = getResources().getDisplayMetrics().density;
        mNavigation     = NavigineSDK.getNavigation();

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

        if (NOTIFICATIONS_ENABLED)
        {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26)
                notificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL, "default",
                        NotificationManager.IMPORTANCE_LOW));
        }

        connectToAws();
        requestAudioPermissions();

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.CANADA);
                }
            }
        });
    }

    @Override public void onDestroy()
    {
        if (mNavigation != null)
        {
            NavigineSDK.finish();
            mNavigation = null;
        }

        super.onDestroy();
    }

    @Override public void onBackPressed()
    {
        moveTaskToBack(true);
    }

    public void toggleAdjustMode(View v)
    {
        mAdjustMode = !mAdjustMode;
        mAdjustTime = 0;
        Button adjustModeButton = findViewById(R.id.navigation__adjust_mode_button);
        adjustModeButton.setBackgroundResource(mAdjustMode ?
                R.drawable.btn_adjust_mode_on :
                R.drawable.btn_adjust_mode_off);
        gui.redrawLocationView();
    }

    public void onZoomIn(View v)
    {
        gui.zoomLocationView(1.25f);
    }

    public void onZoomOut(View v)
    {
        gui.zoomLocationView(0.8f);
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
        gui.setBackVisibility(View.GONE);
        gui.redrawLocationView();
    }

    private void handleClick(float x, float y)
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
                gui.setBackVisibility(View.VISIBLE);
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
                gui.setBackVisibility(View.VISIBLE);
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

        gui.redrawLocationView();
    }

    public void onNav(float x, float y) {

        Log.d("Debug:onNav", "x = " + x + ", y = " + y);

        if (mNavigation == null)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        mTargetPoint  = new LocationPoint(mLocation.getId(), subLoc.getId(), x, y);
        mTargetVenue  = null;
        mPinPoint     = null;
        mPinPointRect = null;

        mNavigation.setTarget(mTargetPoint);
        gui.setBackVisibility(View.VISIBLE);
        gui.redrawLocationView();

    }

    private void handleLongClick(float x, float y)
    {
        Log.d(TAG, String.format(Locale.ENGLISH, "Long click at (%.2f, %.2f)", x, y));
        makePin(gui.getAbsCoordinates(x, y));
        cancelVenue();
    }

    private void handleScroll(float x, float y, boolean byTouchEvent)
    {
        if (byTouchEvent)
            mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
    }

    private void handleZoom(float ratio, boolean byTouchEvent)
    {
        if (byTouchEvent)
            mAdjustTime = NavigineSDK.currentTimeMillis() + ADJUST_TIMEOUT;
    }

    private void handleEnterZone(Zone z)
    {
        Log.d(TAG, "Enter zone " + z.getName());
        if (NOTIFICATIONS_ENABLED)
        {
            Intent notificationIntent = new Intent(this, NotificationActivity.class);
            notificationIntent.putExtra("zone_id",    z.getId());
            notificationIntent.putExtra("zone_name",  z.getName());
            notificationIntent.putExtra("zone_color", z.getColor());
            notificationIntent.putExtra("zone_alias", z.getAlias());

            // Setting up a notification
            Notification.Builder notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL);
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

    private void handleLeaveZone(Zone z)
    {
        Log.d(TAG, "Leave zone " + z.getName());
        if (NOTIFICATIONS_ENABLED)
        {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(z.getId());
        }
    }

    private void handleDeviceUpdate(DeviceInfo deviceInfo)
    {
        // TO DO : Use path array to get the next turn
        //float l = deviceInfo.getPaths().get(0).getLength();

        if (!(mTargetPoint == null)) {

            float dx = deviceInfo.getX() - mTargetPoint.getX();
            float dy = deviceInfo.getY() - mTargetPoint.getY();
            double d = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
            Log.d(TAG, String.format("dx: %f, dy: %f, d: %f", dx, dy, d));


            if (d < 1) {

                mTargetPoint  = null;
                mTargetVenue  = null;
                mPinPoint     = null;
                mPinPointRect = null;

                mNavigation.cancelTargets();
                gui.setBackVisibility(View.GONE);
                gui.redrawLocationView();

                if (navItem != null) {
                    textToSpeech.speak("You have arrived at the " + navItem.getName(), TextToSpeech.QUEUE_FLUSH, null);
                } else {
                    textToSpeech.speak("You have arrived", TextToSpeech.QUEUE_FLUSH, null);
                }
                navItem = null;
                Log.d(TAG, "Navigation Ended");
            }
        }

        mDeviceInfo = deviceInfo;
        if (mDeviceInfo == null)
            return;

        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        if (mDeviceInfo.isValid())
        {
            gui.setErrorVisibility(View.GONE);
            gui.setBackVisibility(mTargetPoint != null || mTargetVenue != null ?
                    View.VISIBLE : View.GONE);
            if (mAdjustMode)
                adjustDevice();
        }
        else
        {
            gui.setBackVisibility(View.GONE);
            switch (mDeviceInfo.getErrorCode())
            {
                case 4:
                    gui.setErrorMessage("You are out of navigation zone! Please, check that your bluetooth is enabled!");
                    break;

                case 8:
                case 30:
                    gui.setErrorMessage("Not enough beacons on the location! Please, add more beacons!");
                    break;

                default:
                    gui.setErrorMessage(String.format(Locale.ENGLISH,
                            "Something is wrong with location '%s' (error code %d)! " +
                                    "Please, contact technical support!",
                            mLocation.getName(), mDeviceInfo.getErrorCode()));
                    break;
            }
        }

        // This causes map redrawing
        gui.redrawLocationView();
    }

    private boolean loadMap()
    {
        if (mNavigation == null)
        {
            Log.e(TAG, "Can't load map! Navigine SDK is not available!");
            return false;
        }

        mLocation = mNavigation.getLocation();
        mCurrentSubLocationIndex = -1;

        if (mLocation == null)
        {
            Log.e(TAG, "Loading map failed: no location");
            return false;
        }

        if (mLocation.getSubLocations().size() == 0)
        {
            Log.e(TAG, "Loading map failed: no sublocations");
            mLocation = null;
            return false;
        }

        if (!loadSubLocation(0))
        {
            Log.e(TAG, "Loading map failed: unable to load default sublocation");
            mLocation = null;
            return false;
        }


        gui.setZoomVisibility(View.VISIBLE);
        gui.setZoomVisibility(View.VISIBLE);
        gui.setAdjustVisibility(View.GONE);

        mNavigation.setMode(NavigationThread.MODE_NORMAL);

        if (D.WRITE_LOGS)
            mNavigation.setLogFile(getLogFile("log"));

        gui.redrawLocationView();
        return true;
    }

    private boolean loadSubLocation(int index)
    {
        if (mNavigation == null)
            return false;

        if (mLocation == null || index < 0 || index >= mLocation.getSubLocations().size())
            return false;

        SubLocation subLoc = mLocation.getSubLocations().get(index);
        Log.d(TAG, String.format(Locale.ENGLISH, "Loading sublocation %s (%.2f x %.2f)", subLoc.getName(), subLoc.getWidth(), subLoc.getHeight()));

        if (subLoc.getWidth() < 1.0f || subLoc.getHeight() < 1.0f)
        {
            Log.e(TAG, String.format(Locale.ENGLISH, "Loading sublocation failed: invalid size: %.2f x %.2f", subLoc.getWidth(), subLoc.getHeight()));
            return false;
        }

        if(!gui.loadSubLocation(subLoc))
        {
            Log.e(TAG, "Loading sublocation failed: invalid image");
            return false;
        }

        float viewWidth = gui.getLocationWidth();
        float viewHeight = gui.getLocationHeight();
        float minZoomFactor = Math.min(viewWidth / subLoc.getWidth(), viewHeight / subLoc.getHeight());
        float maxZoomFactor = LocationView.ZOOM_FACTOR_MAX;

        gui.setZoomParameters(minZoomFactor, maxZoomFactor);
        Log.d(TAG, String.format(Locale.ENGLISH, "View size: %.1f x %.1f", viewWidth, viewHeight));

        mAdjustTime = 0;
        mCurrentSubLocationIndex = index;

        cancelVenue();
        gui.redrawLocationView();
        return true;
    }

    private void makePin(PointF P)
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (P.x < 0.0f || P.x > subLoc.getWidth() ||
                P.y < 0.0f || P.y > subLoc.getHeight())
        {
            // Missing the map
            return;
        }

        if (mTargetPoint != null || mTargetVenue != null)
            return;

        if (mDeviceInfo == null || !mDeviceInfo.isValid())
            return;

        mPinPoint = new LocationPoint(mLocation.getId(), subLoc.getId(), P.x, P.y);
        mPinPointRect = new RectF();
        gui.redrawLocationView();
    }

    private void cancelPin()
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return;

        if (mTargetPoint != null || mTargetVenue != null || mPinPoint == null)
            return;

        mPinPoint = null;
        mPinPointRect = null;
        gui.redrawLocationView();
    }

    private void cancelVenue()
    {
        mSelectedVenue = null;
        gui.redrawLocationView();
    }

    private Venue getVenueAt(float x, float y)
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return null;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return null;

        Venue v0 = null;
        float d0 = 1000.0f;

        for(int i = 0; i < subLoc.getVenues().size(); ++i)
        {
            Venue v = subLoc.getVenues().get(i);
            PointF P = gui.getScreenCoordinates(v.getX(), v.getY());

            float d = Math.abs(x - P.x) + Math.abs(y - P.y);
            if (d < 30.0f * gui.mDisplayDensity && d < d0)
            {
                v0 = v;
                d0 = d;
            }
        }

        return v0;
    }

    private Zone getZoneAt(float x, float y)
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return null;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
        if (subLoc == null)
            return null;

        PointF P = gui.getAbsCoordinates(x, y);
        LocationPoint LP = new LocationPoint(mLocation.getId(), subLoc.getId(), P.x, P.y);

        for(int i = 0; i < subLoc.getZones().size(); ++i)
        {
            Zone Z = subLoc.getZones().get(i);
            if (Z.contains(LP))
                return Z;
        }
        return null;
    }

    private void drawPoints(Canvas canvas)
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
        final float dp        = gui.mDisplayDensity;
        final float textSize  = 16 * dp;

        // Preparing paints
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        // Drawing pin point (if it exists and belongs to the current sublocation)
        if (mPinPoint != null && mPinPoint.subLocation == subLoc.getId())
        {
            final PointF T = gui.getScreenCoordinates(mPinPoint);
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
            final PointF T = gui.getScreenCoordinates(mTargetPoint);
            final float tRadius = 10 * dp;

            paint.setARGB(255, 0, 0, 0);
            paint.setStrokeWidth(4 * dp);
            canvas.drawLine(T.x, T.y, T.x, T.y - 3 * tRadius, paint);

            paint.setColor(solidColor);
            canvas.drawCircle(T.x, T.y - 3 * tRadius, tRadius, paint);
        }
    }

    private void drawVenues(Canvas canvas)
    {
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);

        final float dp = gui.mDisplayDensity;
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

            final PointF P = gui.getScreenCoordinates(v.getX(), v.getY());
            final float x0 = P.x - venueSize/2;
            final float y0 = P.y - venueSize/2;
            final float x1 = P.x + venueSize/2;
            final float y1 = P.y + venueSize/2;
            canvas.drawBitmap(mVenueBitmap, null, new RectF(x0, y0, x1, y1), paint);
        }

        if (mSelectedVenue != null)
        {
            final PointF T = gui.getScreenCoordinates(mSelectedVenue.getX(), mSelectedVenue.getY());
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

    private void drawZones(Canvas canvas)
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
            final PointF        Q0 = gui.getScreenCoordinates(P0);
            path.moveTo(Q0.x, Q0.y);

            for(int j = 0; j < Z.getPoints().size(); ++j)
            {
                final LocationPoint P = Z.getPoints().get((j + 1) % Z.getPoints().size());
                final PointF        Q = gui.getScreenCoordinates(P);
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

    private void drawDevice(Canvas canvas)
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
        final float dp = gui.mDisplayDensity;

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
                        PointF P1 = gui.getScreenCoordinates(P);
                        PointF Q1 = gui.getScreenCoordinates(Q);
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
        final float radius  = gui.getScreenLengthX(r);
        final float radius1 = 25 * dp;                            // Internal radius: fixed, solid

        PointF O = gui.getScreenCoordinates(x, y);
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

    private void adjustDevice()
    {
        // Check if location is loaded
        if (mLocation == null || mCurrentSubLocationIndex < 0)
            return;

        // Check if navigation is available
        if (mDeviceInfo == null || !mDeviceInfo.isValid())
            return;

        long timeNow = System.currentTimeMillis();

        // Adjust map, if necessary
        if (timeNow >= mAdjustTime)
        {
            // Firstly, set the correct sublocation
            SubLocation subLoc = mLocation.getSubLocations().get(mCurrentSubLocationIndex);
            if (mDeviceInfo.getSubLocationId() != subLoc.getId())
            {
                for(int i = 0; i < mLocation.getSubLocations().size(); ++i)
                    if (mLocation.getSubLocations().get(i).getId() == mDeviceInfo.getSubLocationId())
                        loadSubLocation(i);
            }

            // Secondly, adjust device to the center of the screen
            PointF center = gui.getScreenCoordinates(mDeviceInfo.getX(), mDeviceInfo.getY());
            float deltaX  = gui.getLocationWidth()  / 2 - center.x;
            float deltaY  = gui.getLocationHeight() / 2 - center.y;
            mAdjustTime   = timeNow;
            gui.scrollByLocation(deltaX, deltaY);
        }
    }

    private String getLogFile(String extension)
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

    private void connectToAws() {
        // Establish connection to AWS
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                Log.d(TAG, "AWSMobileClient is instantiated and you are connected to AWS!");
            }
        }).execute();

        // Instantiate a DynamoDB client
        AmazonDynamoDBClient dynamoDBClient = new AmazonDynamoDBClient(AWSMobileClient.getInstance().getCredentialsProvider());
        this.dynamoDBMapper = DynamoDBMapper.builder()
                .dynamoDBClient(dynamoDBClient)
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .build();
    }

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                // Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        // If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            initInteractiveVoiceView();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    initInteractiveVoiceView();
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permissions denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    // Reads item from DynamoDB
    private void getItem(DynamoDBMapper mapper, String name) throws Exception {
        Item item = mapper.load(Item.class, name);
        Log.d(TAG,item.toString());
        navItem = item;
        textToSpeech.speak("Calculating route to the "
                + navItem.getName(), TextToSpeech.QUEUE_FLUSH, null);
    }

    public void initInteractiveVoiceView(){
        InteractiveVoiceView voiceView =
                findViewById(R.id.voiceInterface);

        voiceView.setInteractiveVoiceListener(
                new InteractiveVoiceView.InteractiveVoiceListener() {

                    @Override
                    public void dialogReadyForFulfillment(Map slots, String intent) {
                        Log.d(TAG, String.format(
                                Locale.US,
                                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                                intent,
                                slots.toString()));
                        final String shoppingListItem = slots.get("ShoppingList_Item").toString().toLowerCase();
                        if (intent.equals("NavigateToItem")) {
                            Log.d(TAG,"Handling NavigateToItem intent.");
                            Runnable runnable = new Runnable() {
                                public void run() {
                                    try {
                                        getItem(dynamoDBMapper, shoppingListItem);
                                        //onNav(navItem.getPositionX(), navItem.getPositionY());

                                    }
                                    catch (Throwable t) {
                                        Log.d(TAG,"Could not retrieve item from DynamoDB: " + t);
                                        t.printStackTrace();
                                    }
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                            // TOTO Fix loop
                            while(navItem == null) {}

                            onNav(navItem.getPositionX(), navItem.getPositionY());
                        }
                    }
                    //onNav(navItem.getPositionX(), navItem.getPositionY());
                    @Override
                    public void onResponse(Response response) {
                        Log.d(TAG, "User input: " + response.getInputTranscript());
                        Log.d(TAG, "Bot response: " + response.getTextResponse());
                    }

                    @Override
                    public void onError(String responseText, Exception e) {
                        Log.e(TAG, "Error: " + responseText, e);
                    }
                });

        voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance().getCredentialsProvider());

        voiceView.getViewAdapter()
                .setInteractionConfig(
                        new InteractionConfig(getApplicationContext().getString(R.string.aws_bot_name),
                                getApplicationContext().getString(R.string.aws_bot_alias)));

        voiceView.getViewAdapter()
                .setAwsRegion(getApplicationContext()
                        .getString(R.string.aws_region));
    }
}