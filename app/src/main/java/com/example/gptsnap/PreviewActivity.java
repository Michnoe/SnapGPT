package com.example.gptsnap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PreviewActivity extends AppCompatActivity {

    public static String capturedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        TextView previewText = findViewById(R.id.preview_text);
        Button sendToChatGPTButton = findViewById(R.id.send_to_chatgpt_button);

        if (capturedText != null) {
            previewText.setText(capturedText);
        } else {
            previewText.setText("No text captured.");
        }

        sendToChatGPTButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturedText != null) {
                    sendToChatGPT(capturedText);
                }
            }
        });
    }

    private void sendToChatGPT(String extractedText) {
        // Call the existing sendToChatGPT method in MainActivity
        MainActivity mainActivity = new MainActivity();
        mainActivity.sendToChatGPT(extractedText);

        // Close the PreviewActivity and return to MainActivity
        finish();
    }
}
