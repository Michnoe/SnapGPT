package com.example.gptsnap;

import static com.example.gptsnap.BuildConfig.CHATGPT_API_KEY;

import java.io.File;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextView extractedText;
    // Other variables
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ProgressDialog progressDialog;

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading Model");
        progressDialog.setMessage("Please wait while the text recognition model is being downloaded...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions();
        }

        Button captureImageButton = findViewById(R.id.captureImageButton);
        captureImageButton.setOnClickListener(this::captureImage);


    }


    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        ActivityCompat.requestPermissions(this, permissions, 100);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview);

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

    }

    public void captureImage(View view) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timeStamp = sdf.format(new Date());
        File outputFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "IMG_" + timeStamp + ".jpg");

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap capturedImageBitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                        extractTextFromImage(capturedImageBitmap);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        // Handle any errors
                        Toast.makeText(MainActivity.this, "Error capturing image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required to use this app.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }



// ...

    private void extractTextFromImage(Bitmap bitmap) {
        showProgressDialog();

        InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    // Dismiss the progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    // Process the text
                    sendToChatGPT(text.getText());
                })
                .addOnFailureListener(e -> {
                    // Dismiss the progress dialog
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    // Handle any errors
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }





    public void sendToChatGPT(String extractedText) {
        // Implement API call to ChatGPT using the extracted text
        // You will need to use the API key and endpoint provided by the ChatGPT service
        // For example, using OkHttp:

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        Log.d("sendToChatGPT", "Extracted text: " + extractedText);

        String jsonBody = "{\"prompt\": \"" + extractedText + "\", \"max_tokens\": 50}"; // Modify max_tokens as needed

        Log.d("sendToChatGPT", "JSON payload: " + jsonBody);

        RequestBody requestBody = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/engines/text-davinci-002/completions") // Replace "davinci-codex" with "text-davinci-002"
                .addHeader("Authorization", "Bearer "+CHATGPT_API_KEY) // Replace YOUR_API_KEY with your actual API key
                .post(requestBody)
                .build();


        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                // Handle any errors
                Log.e("sendToChatGPT", "API call failed", e);
            }

           /*
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                // Handle the response and display the insight from ChatGPT
                if (response.isSuccessful()) {
                    Log.d("sendToChatGPT", "API call successful: " + response.message());
                    String responseBody = Objects.requireNonNull(response.body()).string();

                    // Close the response body
                    response.close();

                    Log.d("sendToChatGPT", "API call successful: " + responseBody);

                    // Extract the relevant data from the response and update the UI
                    Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                    intent.putExtra("response", responseBody);
                    startActivity(intent);
                } else {
                    Log.e("sendToChatGPT", "API call unsuccessful: " + response.message());
                    // Add this line to log more information about the error
                    Log.e("sendToChatGPT", "Error response body: " + response.body().string());
                }
            }
            */
           @Override
           public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
               // Handle the response and display the insight from ChatGPT
               if (response.isSuccessful()) {
                   String responseBody = Objects.requireNonNull(response.body()).string();

                   // Close the response body
                   response.close();

                   Log.d("sendToChatGPT", "API call successful: " + responseBody);

                   // Extract the relevant data from the response and update the UI
                   try {
                       JSONObject jsonResponse = new JSONObject(responseBody);
                       //String generatedText = jsonResponse.getJSONObject("choices").getJSONArray("choices").getJSONObject(0).getString("text");
                       String generatedText = jsonResponse.getJSONArray("choices").getJSONObject(0).getString("text");

                       Intent intent = new Intent(MainActivity.this, ResponseActivity.class);
                       intent.putExtra("response", generatedText);
                       startActivity(intent);
                   } catch (JSONException e) {
                       Log.e("sendToChatGPT", "JSON parsing error: " + e.getMessage());
                   }
               } else {
                   //Log.e("sendToChatGPT", "API call unsuccessful: " + response.message());
                   Log.e("sendToChatGPT", "Error response body: " + response.body().string());

               }
           }

        });



    }
}

       
