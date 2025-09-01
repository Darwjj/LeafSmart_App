package com.example.leafsmart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.widget.SearchView;

/**
 * Displays search plant diseases list from the disease JSON file.
 * It helps users to tap on a disease to view detailed information.
 */
public class DiseaseListActivity extends AppCompatActivity {

    private RecyclerView recyclerView; // List for disease names
    private DiseaseAdapter adapter; // Adapter for handling diseases list
    private Map<String, Disease> diseaseMap; // Maps disease names to details

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disease_list);

        recyclerView = findViewById(R.id.recyclerViewDiseases);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SearchView searchView = findViewById(R.id.searchView);

        // Back Track Button
        Button btnBackMenu = findViewById(R.id.btnBackMenu);
        btnBackMenu.setOnClickListener(v -> {
            Intent intent = new Intent(DiseaseListActivity.this, MainActivity.class);
            startActivity(intent);
        });

        loadDiseases(); // Load diseases from JSON file
        // Live search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }
    // Reads the JSON file from assets directory and loads all diseases as a list
    private void loadDiseases() {
        try {
            InputStreamReader reader = new InputStreamReader(getAssets().open("diseases.json"));
            Type type = new TypeToken<Map<String, Disease>>(){}.getType();
            diseaseMap = new Gson().fromJson(reader, type);

            List<String> diseaseNames = new ArrayList<>(diseaseMap.keySet()); // Extract disease names for the list
            // Set up adapter for click propose
            adapter = new DiseaseAdapter(diseaseNames, diseaseName -> {
                Intent intent = new Intent(this, DiseaseDetailActivity.class);
                intent.putExtra("disease_name", diseaseName);
                startActivity(intent);
            });

            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this, "Error loading diseases.", Toast.LENGTH_LONG).show();
        }
    }
}