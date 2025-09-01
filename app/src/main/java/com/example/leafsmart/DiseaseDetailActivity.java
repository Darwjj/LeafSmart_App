package com.example.leafsmart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Displays detailed information by plant disease.
 * This information includes disease name, description, symptoms, and treatment.
 * Data is loaded from the local JSON file so it works fully offline.
 */
public class DiseaseDetailActivity extends AppCompatActivity {

    private TextView nameText, descriptionText, symptomsText, treatmentText;
    private Map<String, Disease> diseaseMap; // Holds all disease details from JSON

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_detail);

        nameText = findViewById(R.id.detailName);
        descriptionText = findViewById(R.id.detailDescription);
        symptomsText = findViewById(R.id.detailSymptoms);
        treatmentText = findViewById(R.id.detailTreatment);

        // Back button to return to the disease list
        Button btnBackMenu = findViewById(R.id.btnBackMenu);
        btnBackMenu.setOnClickListener(v -> {
            Intent intent = new Intent(DiseaseDetailActivity.this, DiseaseListActivity.class);
            startActivity(intent);
        });

        // Load the full disease dataset from local storage file (completely  offline)
        try {
            InputStreamReader reader = new InputStreamReader(getAssets().open("diseases.json"));
            Type type = new TypeToken<Map<String, Disease>>() {}.getType();
            diseaseMap = new Gson().fromJson(reader, type);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load disease data", Toast.LENGTH_SHORT).show();
            return; // Exist early if we cannot load data
        }

        // Get selected disease
        String diseaseName = getIntent().getStringExtra("disease_name");
        nameText.setText(diseaseName);
        // Look up for disease in our loaded map and display its details
        Disease disease = diseaseMap.get(diseaseName);
        if (disease != null) {
            descriptionText.setText("Description: " + disease.getDescription());
            symptomsText.setText("Symptoms: " + disease.getSymptoms());
            treatmentText.setText("Treatment: " + disease.getTreatment());
        } else {
            // If this fails for some reason, it will display n/a
            descriptionText.setText("Description: N/A");
            symptomsText.setText("Symptoms: N/A");
            treatmentText.setText("Treatment: N/A");
        }
    }
}
