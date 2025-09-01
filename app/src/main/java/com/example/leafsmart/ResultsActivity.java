package com.example.leafsmart;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.widget.ProgressBar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.ImageView;

/**
 * ResultsActivity displays the results of the plant disease detection process.
 * It shows the predicted disease, confidence levels, and relevant information
 * such as symptoms and treatment options.
 */

public class ResultsActivity extends AppCompatActivity {

    private TextView diseaseNameText, confidenceText, descriptionText, symptomsText, treatmentText;
    private Map<String, Disease> diseaseMap;
    private float[][][][] featureMap;
    private int topClassIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        // Set UI design
        diseaseNameText = findViewById(R.id.textDiseaseName);
        confidenceText = findViewById(R.id.textConfidence);
        descriptionText = findViewById(R.id.textDescription);
        symptomsText = findViewById(R.id.textSymptoms);
        treatmentText = findViewById(R.id.textTreatment);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        ImageView leafImageView = findViewById(R.id.imageLeaf);
        TextView healthScoreText = findViewById(R.id.textHealthScore);
        TextView preventiveTipsText = findViewById(R.id.textPreventiveTips);
        ProgressBar progressBar = findViewById(R.id.progressHealthScore);

        // Redirect to history activity
        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ResultsActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        loadDiseaseData(); // load disease descriptions, symptoms, and treatments from JSON diseases file

        // Retrieve predictions results
        String[] diseaseNames = getIntent().getStringArrayExtra("disease_names");
        float[] confidences = getIntent().getFloatArrayExtra("confidences");

        // Retrieve Gra-Cam map data and top predicted index
        String featureMapPath = getIntent().getStringExtra("feature_map_path");
        topClassIndex = getIntent().getIntExtra("top_index", -1);
        if (featureMapPath != null) {
            featureMap = loadFeatureMap(featureMapPath);
        }

        // Display the top 3 predictions with confidence score
        StringBuilder resultText = new StringBuilder();
        for (int i = 0; i < diseaseNames.length; i++) {
            resultText.append(String.format("%d. %s (%.2f%% confidence)\n", i + 1, diseaseNames[i], confidences[i] * 100));
        }

        diseaseNameText.setText(resultText.toString());
        confidenceText.setText(String.format("Top Prediction Confidence: %.2f%%", confidences[0] * 100));

        // Display disease description, symptoms, and treatment for the top prediction
        Disease topDisease = diseaseMap.get(diseaseNames[0]);
        if (topDisease != null) {
            descriptionText.setText("Description: " + topDisease.getDescription());
            symptomsText.setText("Symptoms: " + topDisease.getSymptoms());
            treatmentText.setText("Treatment: " + topDisease.getTreatment());
        }
        // Determine leaf health status (Healthy vs Diseased) and set colours based on the result
        String topLabel = diseaseNames[0].toLowerCase();
        String healthStatus;
        int healthColor;
        if (topLabel.contains("healthy")) {
            healthStatus = "Healthy";
            healthColor = Color.parseColor("#2E7D32"); // Green
        } else {
            healthStatus = "Diseased";
            healthColor = Color.parseColor("#C62828"); // Red
        }
        // Show health score
        float topConfidence = confidences[0] * 100;
        healthScoreText.setText(String.format("Leaf Health: %.0f%% (%s)", topConfidence, healthStatus));
        healthScoreText.setTextColor(healthColor);
        progressBar.setProgress((int) topConfidence);
        progressBar.setProgressTintList(ColorStateList.valueOf(healthColor));
        preventiveTipsText.setText("Preventive Tips:\n" + getPreventiveTips()); // Show preventive tips for the detected disease

        // Load and display original image, and overlay with the Grad-Map heatmap functionality
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null && new File(imagePath).exists()) {
            Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);
            if (originalBitmap != null && featureMap != null && topClassIndex != -1) {
                Bitmap overlayBitmap = generateGradCamOverlay(originalBitmap, featureMap[0]); // Generate Grad-Map Overlay
                leafImageView.setImageBitmap(overlayBitmap);

                // Save Grad-CAM image to History
                new Thread(() -> {
                    String overlayFilename = "leaf_" + System.currentTimeMillis() + "_overlay.png";
                    File overlayFile = new File(getFilesDir(), overlayFilename);
                    try (FileOutputStream out = new FileOutputStream(overlayFile)) {
                        overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

                        // Save detection result with Grad-CAM path to history.json
                        HistoryItem entry = new HistoryItem(
                                diseaseNames[0],
                                topConfidence,
                                healthStatus,
                                System.currentTimeMillis(),
                                overlayFile.getAbsolutePath()
                        );
                        saveHistoryEntry(entry);

                        // Notify user when saved
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Grad-CAM saved to history.", Toast.LENGTH_SHORT).show();
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Failed to save Grad-CAM image.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "Missing heatmap data. Showing original.", Toast.LENGTH_SHORT).show(); // If not heatmap data, show original image
                leafImageView.setImageBitmap(originalBitmap);
            }
        } else {
            Toast.makeText(this, "Image path not found or file missing.", Toast.LENGTH_SHORT).show();
        }

    }

    // Reads feature map file form storage into a 4D float array
    private float[][][][] loadFeatureMap(String path) {
        float[][][][] map = new float[1][7][7][1280];
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] buffer = new byte[4];
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 7; j++) {
                    for (int k = 0; k < 1280; k++) {
                        if (fis.read(buffer) != -1) {
                            map[0][i][j][k] = ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder()).getFloat();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
    // Generates a Grad-CAM heatmap and overlays it on the original leaf image
    private Bitmap generateGradCamOverlay(Bitmap bitmap, float[][][] convFeatures) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Step 1: Compute average activation per channel
        float[] weights = new float[1280];
        for (int k = 0; k < 1280; k++) {
            float sum = 0f;
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 7; j++) {
                    sum += convFeatures[i][j][k];
                }
            }
            weights[k] = sum / 49f;
        }

        // Step 2: Create Grad-CAM heatmap using ReLU
        float[][] heatmap = new float[7][7];
        float maxVal = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                float weightedSum = 0f;
                for (int k = 0; k < 1280; k++) {
                    weightedSum += convFeatures[i][j][k] * weights[k];
                }
                heatmap[i][j] = Math.max(weightedSum, 0); // ReLU
                if (heatmap[i][j] > maxVal) maxVal = heatmap[i][j];
            }
        }

        // Step 3: Normalise heatmap values and create bitmap
        Bitmap heatmapBmp = Bitmap.createBitmap(7, 7, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                int intensity = (int) ((heatmap[i][j] / maxVal) * 255);
                heatmapBmp.setPixel(j, i, Color.argb(intensity, 255, 0, 0));
            }
        }

        // Step 4: Scale heatmap to match original image size and overlay with transparency
        Bitmap resizedHeatmap = Bitmap.createScaledBitmap(heatmapBmp, width, height, true);
        Bitmap overlay = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlay);
        canvas.drawBitmap(bitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setAlpha(100); // Transparency for heatmap overlay
        canvas.drawBitmap(resizedHeatmap, 0, 0, paint);

        return overlay;
    }

    // Provides disease-specif preventive tips or generic advice from the JSON disease_names file
    private String getPreventiveTips() {
        String[] diseaseNames = getIntent().getStringArrayExtra("disease_names");
        if (diseaseNames == null || diseaseNames.length == 0) {
            return "Maintain healthy soil, avoid water splash, and inspect leaves regularly.";
        }
        // Switch statement matches the top diseases with its preventive measures
        String topDisease = diseaseNames[0].toLowerCase();
        switch (topDisease) {
            case "apple___apple_scab":
            case "apple___black_rot": return "Prune affected areas, apply fungicide during spring, and avoid wet foliage.";
            case "apple___cedar_apple_rust": return "Remove nearby cedar trees if possible and apply fungicide in early spring.";
            case "blueberry___healthy":
            case "cherry_(including_sour)___healthy":
            case "corn_(maize)___healthy":
            case "grape___healthy":
            case "peach___healthy":
            case "pepper,_bell___healthy":
            case "potato___healthy":
            case "raspberry___healthy":
            case "soybean___healthy":
            case "tomato___healthy":
            case "strawberry___healthy": return "Plant is healthy. Keep monitoring weekly and water appropriately.";
            case "cherry_(including_sour)___powdery_mildew":
            case "squash___powdery_mildew": return "Avoid overhead watering, improve air circulation, and apply sulfur-based fungicide.";
            case "corn_(maize)___cercospora_leaf_spot gray_leaf_spot":
            case "corn_(maize)___northern_leaf_blight": return "Use resistant hybrids, rotate crops, and remove infected debris.";
            case "corn_(maize)___common_rust_": return "Use rust-resistant varieties, remove crop residue, and rotate with non-host crops.";
            case "grape___black_rot":
            case "grape___esca_(black_measles)":
            case "grape___leaf_blight_(isariopsis_leaf_spot)": return "Prune infected leaves, manage vineyard humidity, and avoid fruit injuries.";
            case "orange___haunglongbing_(citrus_greening)": return "Control psyllid population, remove infected trees, and monitor frequently.";
            case "peach___bacterial_spot":
            case "pepper,_bell___bacterial_spot":
            case "tomato___bacterial_spot": return "Avoid wetting foliage, apply copper-based fungicide, and rotate crops annually.";
            case "potato___early_blight":
            case "tomato___early_blight": return "Remove infected leaves, apply chlorothalonil or mancozeb, and avoid overhead watering.";
            case "potato___late_blight":
            case "tomato___late_blight": return "Destroy infected plants, apply fungicides preventively, and space plants for air circulation.";
            case "strawberry___leaf_scorch": return "Water at root level, prune infected leaves, and apply nitrogen sparingly.";
            case "tomato___leaf_mold": return "Ensure proper air flow, avoid overcrowding, and remove affected leaves.";
            case "tomato___septoria_leaf_spot": return "Apply fungicide regularly, remove infected leaves, and avoid splashing water.";
            case "tomato___spider_mites two-spotted_spider_mite": return "Increase humidity, use insecticidal soap, and monitor leaf undersides.";
            case "tomato___target_spot": return "Remove lower leaves, use mulch to prevent soil splash, and apply fungicide.";
            case "tomato___tomato_yellow_leaf_curl_virus":
            case "tomato___tomato_mosaic_virus": return "Control whiteflies, remove infected plants, and use resistant varieties.";
            default: return "Improve air flow, keep foliage dry, use disease-resistant varieties, and inspect weekly.";
        }
    }
    // Load diease data from JSON into a Map
    private void loadDiseaseData() {
        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("diseases.json");
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Disease>>(){}.getType();
            diseaseMap = gson.fromJson(new InputStreamReader(inputStream), type);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load disease data", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    // Saves detection results to history.json, so it can be shown in the History Activity
    private void saveHistoryEntry(HistoryItem entry) {
        try {
            File file = new File(getFilesDir(), "history.json");
            Gson gson = new Gson();
            List<HistoryItem> history = new ArrayList<>();
            if (file.exists()) {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
                Type listType = new TypeToken<List<HistoryItem>>(){}.getType();
                history = gson.fromJson(reader, listType);
                reader.close();
            }
            history.add(0, entry); // Add new entry at top
            FileWriter writer = new FileWriter(file);
            gson.toJson(history, writer);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save history", Toast.LENGTH_SHORT).show();
        }
    }
}
