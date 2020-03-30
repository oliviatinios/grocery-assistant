package com.example.groceryassistant;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class VoiceInterface {

    private TextToSpeech textToSpeech;

    VoiceInterface(Context context) {
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.CANADA);
                }
            }
        });

    }

    void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

}
