package com.example.groceryassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.util.*;
import java.io.*;
import java.lang.*;
import java.util.*;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.navigine.naviginesdk.*;

public class MainActivity extends Activity {

    public static final String   TAG                     = "MAIN_ACTIVITY";;
    public static final String   NOTIFICATION_CHANNEL    = "GROCERYASSISTANT_NOTIFICATION_CHANNEL";
    public static final int      ADJUST_TIMEOUT          = 5000; // milliseconds
    public static final boolean  ORIENTATION_ENABLED     = true; // Show device orientation?
    public static final boolean  NOTIFICATIONS_ENABLED   = true; // Show zone notifications?

    // NavigationThread instance
    public static NavigationThread mNavigation            = null;

    // UI Parameters
    public static LocationView  mLocationView             = null;
    public static View          mBackView                 = null;
    public static View          mZoomInView               = null;
    public static View          mZoomOutView              = null;
    public static View          mAdjustModeView           = null;
    public static TextView      mErrorMessageLabel        = null;
    public static Handler       mHandler                  = new Handler();
    public static float         mDisplayDensity           = 0.0f;

    public static boolean       mAdjustMode               = false;
    public static long          mAdjustTime               = 0;

    // Location parameters
    public static Location      mLocation                 = null;
    public static int           mCurrentSubLocationIndex  = -1;

    // Device parameters
    public static DeviceInfo    mDeviceInfo               = null; // Current device
    public static LocationPoint mPinPoint                 = null; // Potential device target
    public static LocationPoint mTargetPoint              = null; // Current device target
    public static RectF         mPinPointRect             = null;

    public static Bitmap  mVenueBitmap    = null;
    public static Venue   mTargetVenue    = null;
    public static Venue   mSelectedVenue  = null;
    public static RectF   mSelectedVenueRect = null;
    public static Zone    mSelectedZone   = null;



    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    // Declare a DynamoDBMapper object
    private DynamoDBMapper dynamoDBMapper;

    // Text to speech
    public static TextToSpeech textToSpeech;
    public static Item navItem;

    private Navigation navModule = new Navigation();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity started");

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Setting up GUI parameters
        mBackView = (View)findViewById(R.id.navigation__back_view);
        mZoomInView  = (View)findViewById(R.id.navigation__zoom_in_view);
        mZoomOutView = (View)findViewById(R.id.navigation__zoom_out_view);
        mAdjustModeView = (View)findViewById(R.id.navigation__adjust_mode_view);
        mErrorMessageLabel = (TextView)findViewById(R.id.navigation__error_message_label);

        mBackView.setVisibility(View.INVISIBLE);
        mZoomInView.setVisibility(View.INVISIBLE);
        mZoomOutView.setVisibility(View.INVISIBLE);
        mAdjustModeView.setVisibility(View.INVISIBLE);
        mErrorMessageLabel.setVisibility(View.GONE);

        mVenueBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.elm_venue);

        // Initializing location view
        mLocationView = (LocationView)findViewById(R.id.navigation__location_view);
        mLocationView.setBackgroundColor(0xffebebeb);
        mLocationView.setListener
                (
                        new LocationView.Listener()
                        {
                            @Override public void onClick     ( float x, float y ) { navModule.handleClick(x, y);     }
                            @Override public void onLongClick ( float x, float y ) { navModule.handleLongClick(x, y); }
                            @Override public void onScroll    ( float x, float y, boolean byTouchEvent ) { navModule.handleScroll ( x, y,  byTouchEvent ); }
                            @Override public void onZoom      ( float ratio,      boolean byTouchEvent ) { navModule.handleZoom   ( ratio, byTouchEvent ); }

                            @Override public void onDraw(Canvas canvas)
                            {
                                navModule.drawZones(canvas);
                                navModule.drawPoints(canvas);
                                navModule.drawVenues(canvas);
                                navModule.drawDevice(canvas);
                            }
                        }
                );

        // Loading map only when location view size is known
        mLocationView.addOnLayoutChangeListener
                (
                        new OnLayoutChangeListener()
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
                                    navModule.loadMap();
                            }
                        }
                );

        mDisplayDensity = getResources().getDisplayMetrics().density;
        mNavigation     = NavigineSDK.getNavigation();

        // Setting up device listener
        if (mNavigation != null)
        {
            mNavigation.setDeviceListener
                    (
                            new DeviceInfo.Listener()
                            {
                                @Override public void onUpdate(DeviceInfo info) { navModule.handleDeviceUpdate(info); }
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
                                @Override public void onEnterZone(Zone z) { navModule.handleEnterZone(z); }
                                @Override public void onLeaveZone(Zone z) { navModule.handleLeaveZone(z); }
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
        //setContentView(R.layout.activity_main);

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
                                           String permissions[], int[] grantResults) {
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
                (InteractiveVoiceView) findViewById(R.id.voiceInterface);

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
                                        navModule.onNav(navItem.getPositionX(), navItem.getPositionY());

                                    }
                                    catch (Throwable t) {
                                        Log.d(TAG,"Could not retrieve item from DynamoDB: " + t);
                                        t.printStackTrace();
                                    }
                                }
                            };
                            Thread mythread = new Thread(runnable);
                            mythread.start();
                        }
                    }

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
