package com.armhansa.app.blindnavigator.tool;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class MyTTS extends UtteranceProgressListener
    implements TextToSpeech.OnInitListener {
    private static MyTTS myTTS;

    public static MyTTS getInstance(Context context) {
        if (myTTS == null) {
            myTTS = new MyTTS(context);
        }
        return myTTS;
    }

    public static MyTTS getInstance() {
        return myTTS;
    }

    private TextToSpeech tts;
    private boolean isSpeaking = false;
    private Locale locale = Locale.getDefault();

    public MyTTS(Context context) {
        tts = new TextToSpeech(context, this);
        tts.setLanguage(new Locale("th"));
    }

    public void addSpeak(String message) {
        tts.speak(message, TextToSpeech.QUEUE_ADD, null);
    }

    public void speakNow(String message) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
    }

    public boolean isSpeaking() {
        return tts.isSpeaking();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            addSpeak("โปรดถือมือถือในแนวตั้งและทำมุมก้ม 60 องศาจากพื้น");
        }
    }

    @Override
    public void onStart(String s) {
        isSpeaking = true;
    }

    @Override
    public void onDone(String s) {
        isSpeaking = false;
    }

    @Override
    public void onError(String s) {
        isSpeaking = false;
    }

    @Override
    public void onStop(String utteranceId, boolean interrupted) {
        speakNow("");
        super.onStop(utteranceId, interrupted);
        if (tts != null) {
            tts.shutdown();
        }
    }

}
