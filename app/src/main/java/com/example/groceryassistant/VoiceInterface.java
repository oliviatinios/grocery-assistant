package com.example.groceryassistant;

import android.media.AudioManager;
import android.media.MediaPlayer;
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

class VoiceInterface {

    private List<Voice> voices;
    private AmazonPollyPresigningClient client;
    private MediaPlayer mediaPlayer;

    private int VOICE_NUMBER = 12;

    VoiceInterface() {
        // Create a client that supports generation of presigned URLs.
        client = new AmazonPollyPresigningClient(AWSMobileClient.getInstance().getCredentialsProvider());

        // Create describe voices request.
        final DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

        // HTTP requests must be in separate thread
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

        while (voices == null) {}   // Ugly wait loop will remove later

        // Set up the initial media player
        setupNewMediaPlayer();
    }

    void speak(String text) {
        // Set the chosen voice
        Voice selectedVoice = voices.get(VOICE_NUMBER);

        // Create speech synthesis request.
        SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                new SynthesizeSpeechPresignRequest()
                        // Set text to synthesize.
                        .withText(text)
                        // Set voice selected by the user.
                        .withVoiceId(selectedVoice.getId())
                        // Set format to MP3.
                        .withOutputFormat(OutputFormat.Mp3);

        // Get the presigned URL for synthesized speech audio stream.
        URL presignedSynthesizeSpeechUrl =
                client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

        Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

        // Create a media player to play the synthesized audio stream.
        if (mediaPlayer.isPlaying()) {
            setupNewMediaPlayer();
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            // Set media player's data source to previously obtained URL.
            mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
        } catch (IOException e) {
            Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
        }

        // Start the playback asynchronously (since the data source is a network stream).
        mediaPlayer.prepareAsync();

    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
    }

}
