package com.example.groceryassistant;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.StrictMode;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
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


    private List<Voice> voices;
    private AmazonPollyPresigningClient client;

    private int VOICE_NUMBER = 12;

    VoiceInterface() {
        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(AWSMobileClient.getInstance().getCredentialsProvider());

        // Create describe voices request.
        final DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    // Synchronously ask the Polly Service to describe available TTS voices.
                    DescribeVoicesResult describeVoicesResult = client.describeVoices(describeVoicesRequest);

                    // Get list of voices from the result.
                    voices = describeVoicesResult.getVoices();

                    // Log a message with a list of available TTS voices.
                    Log.i(TAG, "Available Polly voices: " + voices);

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        thread.start();

    }

    void speak(String text) {

        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set the text to synthesize.
                        .withText(text)
                        // Select voice for synthesis.
                        .withVoiceId(voices.get(VOICE_NUMBER).getId()) // "Joanna"
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
        } catch (
                IOException e) {
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
