package com.example.leafsmart;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * History activity in charge of display full details of a saved history entry.
 * Which includes the disease prediction, confidence score, health status,
 * timestamp, description, symptoms, treatment, and Grad-CAM hotspot image.
 */

public class HistoryDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        TextView diseaseNameText = findViewById(R.id.detailDiseaseName);
        TextView confidenceText = findViewById(R.id.detailConfidence);
        TextView healthStatusText = findViewById(R.id.detailHealthStatus);
        TextView timestampText = findViewById(R.id.detailTimestamp);
        TextView descriptionText = findViewById(R.id.textDescription);
        TextView symptomsText = findViewById(R.id.textSymptoms);
        TextView treatmentText = findViewById(R.id.textTreatment);
        ImageView detailImageView = findViewById(R.id.detailImageView);

        // Retrieve data from the Intent passed by HistoryActivity
        String diseaseName = getIntent().getStringExtra("disease_name");
        float confidence = getIntent().getFloatExtra("confidence", 0);
        String healthStatus = getIntent().getStringExtra("health_status");
        String formattedDate = getIntent().getStringExtra("date");
        String description = getIntent().getStringExtra("description");
        String symptoms = getIntent().getStringExtra("symptoms");
        String treatment = getIntent().getStringExtra("treatment");
        String gradCamPath = getIntent().getStringExtra("image_path");

        // Display data
        diseaseNameText.setText("Disease: " + diseaseName);
        confidenceText.setText(String.format("Confidence: %.2f%%", confidence));
        healthStatusText.setText("Health Status: " + healthStatus);
        timestampText.setText("Scanned On: " + formattedDate);
        descriptionText.setText("Description:\n" + (description != null ? description : "N/A"));
        symptomsText.setText("Symptoms:\n" + (symptoms != null ? symptoms : "N/A"));
        treatmentText.setText("Treatment:\n" + (treatment != null ? treatment : "N/A"));

        // Load the Grad-CAM image (if it exits) to visually show the detected hotspot
        if (gradCamPath != null && new File(gradCamPath).exists()) {
            Bitmap gradCam = BitmapFactory.decodeFile(gradCamPath);
            if (gradCam != null) {
                detailImageView.setImageBitmap(gradCam); // Display Grad-CAM image
            } else {
                Toast.makeText(this, "Failed to decode image file.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Image path not found or file is missing.", Toast.LENGTH_SHORT).show();
        }


        // Back track button to return to the history list
        Button btnBackMenu = findViewById(R.id.btnBackMenu);
        btnBackMenu.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryDetailActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}