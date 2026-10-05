package com.saha.ai;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);

        text.setText(
                "Saha AI\n\n" +
                "🔊 Text-to-Speech Test\n\n" +
                "Saha ab bolega."
        );

        text.setTextSize(24);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER);
        text.setBackgroundColor(Color.BLACK);

        setContentView(text);

        tts = new TextToSpeech(this, status -> {

            if (status == TextToSpeech.SUCCESS) {

                tts.setLanguage(new Locale("hi", "IN"));

                tts.speak(
                        "नमस्ते, मैं साहा हूँ।",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "SAHA_TEST"
                );
            }
        });
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
}
