package com.example.leafsmart;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.DataType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.FileOutputStream;
import androidx.core.content.FileProvider;

/**
 * Main plant disease detection activity
 * It will allows the user to select or capture an image
 * runs the TensorFlow Lite model, and sends results to ResultsActivtiy
 */
public class DetectionActivity extends AppCompatActivity {
    private static final int SELECT_IMAGE_REQUEST = 1; // Gallery permission
    private ImageView selectedImage; // Preview of selected/capture image
    private Bitmap inputBitmap; // Processed image for model input
    private Interpreter interpreter; // TensorFlow Lite model interpreter
    private ExecutorService executorService; // Runs inference in background thread
    private List<String> labels; // Class labels from labels files
    private static final int CAMERA_REQUEST = 2; // Camera permission
    private Uri photoUri; // Set URI for captured photo



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);

        selectedImage = findViewById(R.id.imagePreview);
        Button btnSelect = findViewById(R.id.btnSelectImage);
        Button btnPredict = findViewById(R.id.btnPredict);
        Button btnBackTrack = findViewById(R.id.btnBackTrack);

        // Image selection: gallery or camera
        btnSelect.setOnClickListener(v -> {
            String[] options = {"Choose from Gallery", "Take a Photo"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Select Image")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            openGallery();
                        } else {
                            openCamera();
                        }
                    })
                    .show();
        });

        // Initialise background thread and load TLite model + labels
        executorService = Executors.newSingleThreadExecutor();
        loadModel();
        loadLabels();
        // Button to fired up the model on the selected image
        btnPredict.setOnClickListener(v -> {
            if (inputBitmap != null) {
                performPrediction(inputBitmap);
            } else {
                Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button to return to main menu
        btnBackTrack.setOnClickListener(v -> {
            Intent intent = new Intent(DetectionActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    // Save the input bitmaps as JPEG file
    private File saveBitmapToFile(Bitmap bitmap) {
        String fileName = "leaf_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(getFilesDir(), fileName);
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return imageFile;
    }

    // Open gallery to chose an image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_IMAGE_REQUEST);
    }

    // Open camera to capture a new image
    private void openCamera() {
        File photoFile = new File(getExternalCacheDir(), "camera_image.jpg");
        photoUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                photoFile
        );

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    // Handles result from gallery or camera
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == SELECT_IMAGE_REQUEST && data != null) {
                    Uri imageUri = data.getData();
                    inputBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } else if (requestCode == CAMERA_REQUEST && photoUri != null) {
                    inputBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                }

                if (inputBitmap != null) {
                    selectedImage.setImageBitmap(inputBitmap);
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Image load failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // TFLite model loaded
    private void loadModel() {
        try (AssetFileDescriptor afd = getAssets().openFd("plant_disease_model_dual_output.tflite");
             FileInputStream fis = afd.createInputStream()) {
            FileChannel fc = fis.getChannel();
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
            interpreter = new Interpreter(buffer);
        } catch (IOException e) {
            Toast.makeText(this, "Error loading model.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Runs inference and send results to ResultsActivity
    private void performPrediction(Bitmap bitmap) {
        executorService.execute(() -> {
            try {
                // Preprocess input image
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3 * 4);
                inputBuffer.order(ByteOrder.nativeOrder());

                int[] intValues = new int[224 * 224];
                resizedBitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224);
                int pixelIndex = 0;
                for (int i = 0; i < 224; ++i) {
                    for (int j = 0; j < 224; ++j) {
                        final int val = intValues[pixelIndex++];
                        inputBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f); // R
                        inputBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);  // G
                        inputBuffer.putFloat((val & 0xFF) / 255.0f);         // B
                    }
                }

                // Output arrays: feature map + predictions
                float[][] predictionOutput = new float[1][38]; // softmax probabilities
                float[][][][] convFeatureOutput = new float[1][7][7][1280]; // feature maps

                Map<Integer, Object> outputMap = new HashMap<>();
                outputMap.put(0, convFeatureOutput);
                outputMap.put(1, predictionOutput);

                Object[] inputArray = {inputBuffer};

                // Run inference
                interpreter.runForMultipleInputsOutputs(inputArray, outputMap);

                // Process predictions
                float[] confidences = predictionOutput[0];

                // Get top-3 predictions
                int[] topIndices = getTopKIndices(confidences, 3);
                String[] topDiseaseNames = new String[3];
                float[] topConfidences = new float[3];

                for (int i = 0; i < 3; i++) {
                    topDiseaseNames[i] = labels.get(topIndices[i]);
                    topConfidences[i] = confidences[topIndices[i]];
                }

                // Save feature map for Grad-CAM
                saveFeatureMap(convFeatureOutput, getFilesDir().getAbsolutePath() + "/featuremap.raw");

                // Launch it into the ResultsActivity
                Intent intent = new Intent(DetectionActivity.this, ResultsActivity.class);
                intent.putExtra("disease_names", topDiseaseNames);
                intent.putExtra("confidences", topConfidences);
                // Save image to file
                File imageFile = saveBitmapToFile(bitmap);
                if (imageFile != null) {
                    intent.putExtra("image_path", imageFile.getAbsolutePath());
                    intent.putExtra("feature_map_path", getFilesDir().getAbsolutePath() + "/featuremap.raw");
                    intent.putExtra("top_index", topIndices[0]);

                    startActivity(intent);
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to save image for Grad-CAM.", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Prediction failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    // Returns indices of top-k highest probability predictions
    private int[] getTopKIndices(float[] probs, int k) {
        PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> Float.compare(probs[b], probs[a]));
        for (int i = 0; i < probs.length; i++) pq.add(i);

        int[] topK = new int[k];
        for (int i = 0; i < k; i++) topK[i] = pq.poll();
        return topK;
    }
    // Save CNN feature map to binary file for later Grad-CAM generation
    private void saveFeatureMap(float[][][][] features, String outputPath) {
        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            for (int i = 0; i < 7; i++) {
                for (int j = 0; j < 7; j++) {
                    for (int c = 0; c < 1280; c++) {
                        float value = features[0][i][j][c];
                        int intBits = Float.floatToIntBits(value);
                        fos.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(intBits).array());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Loads class labels from labels.txt
    private void loadLabels() {
        labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("labels.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load labels", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown(); // Clean up thread pool
        }
    }


}
