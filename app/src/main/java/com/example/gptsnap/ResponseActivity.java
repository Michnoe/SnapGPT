package com.example.gptsnap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ResponseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_response);

        Intent intent = getIntent();
        String response = intent.getStringExtra("response");

        if (response != null) {
            setResponse(response);
        } else {
            setResponse("No response received.");
        }
    }

    public void setResponse(String response) {
        TextView tvResponse = findViewById(R.id.tv_response);
        tvResponse.setText(response);
    }
}
