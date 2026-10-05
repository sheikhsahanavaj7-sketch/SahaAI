package com.saha.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Locale;

public class SahaVoiceService extends Service {

    private SpeechRecognizer recognizer;
    private TextToSpeech tts;

    private boolean ttsReady = false;
    private boolean responding = false;

    private final Handler handler = new Handler();

    private static final String CHANNEL_ID = "saha_voice";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Saha AI")
                        .setContentText("Saha AI voice service is active")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .build();

        startForeground(1, notification);

        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result =
                        tts.setLanguage(new Locale("hi", "IN"));

                ttsReady =
                        result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        // TTS ko ready hone ka thoda time
        handler.postDelayed(
                this::startListening,
                1500
        );
    }

    private void startListening() {

        if (responding) {
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }

        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }

        recognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        recognizer.setRecognitionListener(
                new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null && !matches.isEmpty()) {

                    String command =
                            matches.get(0)
                                    .toLowerCase(Locale.ROOT)
                                    .trim();

                    if (containsWakeWord(command)) {
                        respond();
                    }
                }

                restartListening();
            }

            @Override
            public void onError(int error) {
                restartListening();
            }

            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(
                    int eventType,
                    Bundle params) {
            }
        });

        Intent speechIntent =
                new Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "hi-IN"
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "hi-IN"
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                3
        );

        recognizer.startListening(speechIntent);
    }

    private boolean containsWakeWord(String command) {

        return command.contains("hey saha")
                || command.contains("hey sah")
                || command.contains("he saha")
                || command.contains("saha")
                || command.contains("साहा")
                || command.contains("सहा")
                || command.contains("साह");
    }

    private void respond() {

        if (!ttsReady || tts == null) {
            return;
        }

        responding = true;

        tts.speak(
                "जी, मैं सुन रहा हूँ।",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SAHA_RESPONSE"
        );

        // Response ke baad listening dobara start
        handler.postDelayed(() -> {

            responding = false;
            restartListening();

        }, 2500);
    }

    private void restartListening() {

        if (recognizer != null) {

            recognizer.destroy();
            recognizer = null;
        }

        if (!responding) {

            handler.postDelayed(
                    this::startListening,
                    1000
            );
        }
    }

    private void createNotificationChannel() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Saha AI Voice",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        if (recognizer != null) {

            recognizer.destroy();
            recognizer = null;
        }

        if (tts != null) {

            tts.stop();
            tts.shutdown();
            tts = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
