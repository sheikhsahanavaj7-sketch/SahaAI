package com.saha.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
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
    private boolean listening = false;

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

        handler.postDelayed(this::startListening, 1800);
    }

    private void startListening() {

        if (responding || listening) {
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
            public void onReadyForSpeech(Bundle params) {
                listening = true;
            }

            @Override
            public void onBeginningOfSpeech() {
                listening = true;
            }

            @Override
            public void onResults(Bundle results) {

                listening = false;

                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null) {

                    for (String result : matches) {

                        String command =
                                result.toLowerCase(Locale.ROOT).trim();

                        if (containsWakeWord(command)) {

                            if (containsGoogleCommand(command)) {
                                openGoogle();
                            } else {
                                respond(
                                        "जी, मैं सुन रहा हूँ।"
                                );
                            }

                            return;
                        }
                    }
                }

                restartListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

                ArrayList<String> partial =
                        partialResults.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (partial != null) {

                    for (String result : partial) {

                        String command =
                                result.toLowerCase(Locale.ROOT);

                        if (containsWakeWord(command)) {

                            stopRecognition();

                            if (containsGoogleCommand(command)) {
                                openGoogle();
                            } else {
                                respond(
                                        "जी, मैं सुन रहा हूँ।"
                                );
                            }

                            return;
                        }
                    }
                }
            }

            @Override
            public void onError(int error) {

                listening = false;
                restartListening();
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                listening = false;
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
                true
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1500
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                2500
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2000
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

    private boolean containsGoogleCommand(String command) {

        return command.contains("google")
                || command.contains("गूगल");
    }

    private void respond(String message) {

        responding = true;

        if (ttsReady && tts != null) {

            tts.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "SAHA_RESPONSE"
            );
        }

        handler.postDelayed(() -> {

            responding = false;
            restartListening();

        }, 2500);
    }

    private void openGoogle() {

        responding = true;

        if (ttsReady && tts != null) {

            tts.speak(
                    "जी, Google खोल रहा हूँ।",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "GOOGLE_RESPONSE"
            );
        }

        handler.postDelayed(() -> {

            try {

                Intent intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com")
                        );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);

            } catch (Exception e) {
                responding = false;
                restartListening();
            }

        }, 1300);
    }

    private void stopRecognition() {

        listening = false;

        if (recognizer != null) {

            try {
                recognizer.stopListening();
            } catch (Exception ignored) {
            }
        }
    }

    private void restartListening() {

        if (recognizer != null) {

            try {
                recognizer.destroy();
            } catch (Exception ignored) {
            }

            recognizer = null;
        }

        listening = false;

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








