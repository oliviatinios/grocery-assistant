package com.example.groceryassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Establish connection to AWS
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                Log.d("MAIN_ACTIVITY", "AWSMobileClient is instantiated and you are connected to AWS!");
            }
        }).execute();

        setContentView(R.layout.activity_main);

        requestAudioPermissions();
    }

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
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
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            init();
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
                    // permission was granted, yay!
                    init();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void init(){
        InteractiveVoiceView voiceView =
                (InteractiveVoiceView) findViewById(R.id.voiceInterface);

        voiceView.setInteractiveVoiceListener(
                new InteractiveVoiceView.InteractiveVoiceListener() {

                    @Override
                    public void dialogReadyForFulfillment(Map slots, String intent) {
                        Log.d("MAIN_ACTIVITY", String.format(
                                Locale.US,
                                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                                intent,
                                slots.toString()));
                    }

                    @Override
                    public void onResponse(Response response) {
                        Log.d("MAIN_ACTIVITY", "Bot response: " + response.getTextResponse());
                    }

                    @Override
                    public void onError(String responseText, Exception e) {
                        Log.e("MAIN_ACTIVITY", "Error: " + responseText, e);
                    }
                });

        voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance().getCredentialsProvider());

        voiceView.getViewAdapter()
                .setInteractionConfig(
                        new InteractionConfig("GroceryAssistant","GroceryAssistant"));

        voiceView.getViewAdapter()
                .setAwsRegion(getApplicationContext()
                        .getString(R.string.aws_region));
    }
}
