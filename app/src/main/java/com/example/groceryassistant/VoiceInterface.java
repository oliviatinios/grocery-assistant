package com.example.groceryassistant;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static android.content.ContentValues.TAG;


public class VoiceInterface {

//    private TextToSpeech textToSpeech;
//
//    VoiceInterface(Context context) {
//        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
//            @Override
//            public void onInit(int status) {
//                if(status != TextToSpeech.ERROR) {
//                    textToSpeech.setLanguage(Locale.CANADA);
//                }
//            }
//        });
//
//    }
//
//    void speak(String text) {
//        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
//    }

    // Cognito pool ID. Pool needs to be unauthenticated pool with
    // Amazon Polly permissions.
    String COGNITO_POOL_ID = "us-east-1:02e43abc-0cb2-4c63-80c1-11b22e36364b";

    // Region of Amazon Polly.
    Regions MY_REGION = Regions.US_EAST_1;

    private List<Voice> voices;
    private AmazonPollyPresigningClient client;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private Context context;

    VoiceInterface(Context context) {

        this.context = context;
//        // Initialize the Amazon Cognito credentials provider.
//        credentialsProvider = new CognitoCachingCredentialsProvider(
//                context,
//                COGNITO_POOL_ID,
//                MY_REGION
//        );
//
//        // Create a client that supports generation of presigned URLs.
//        client = new AmazonPollyPresigningClient(credentialsProvider);
//
//        // Create describe voices request.
//        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();
//
//        // Synchronously ask Amazon Polly to describe available TTS voices.
//        DescribeVoicesResult describeVoicesResult = client.describeVoices(describeVoicesRequest);
//        voices = describeVoicesResult.getVoices();

    }

    void speak(String text) {

        // Initialize the Amazon Cognito credentials provider.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                COGNITO_POOL_ID,
                MY_REGION
        );

        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(credentialsProvider);

        // Create describe voices request.
        DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        // Synchronously ask Amazon Polly to describe available TTS voices.
        DescribeVoicesResult describeVoicesResult = client.describeVoices(describeVoicesRequest);
        voices = describeVoicesResult.getVoices();






        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set the text to synthesize.
                        .withText(text)
                        // Select voice for synthesis.
                        .withVoiceId(voices.get(0).getId()) // "Joanna"
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);


        // Use MediaPlayer: https://developer.android.com/guide/topics/media/mediaplayer.html

        // Create a media player to play the synthesized audio stream.
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
        }

        // Prepare the MediaPlayer asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();

        // Set the callback to start the MediaPlayer when it's prepared.
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });

        // Set the callback to release the MediaPlayer after playback is completed.
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });

    }

}

