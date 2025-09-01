package com.example.leafsmart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Displays the saved detection history list (HistoryAdapter).
 * Allows the user to view details of past detection or clear all history.
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList; // List of saved history entries
    private Map<String, Disease> diseaseMap; // Disease details loaded from assets file

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadHistory();// Load saved history from file

        Button btnClearHistory = findViewById(R.id.btnClearHistory);
        TextView emptyText = findViewById(R.id.textEmptyHistory);

        loadDiseaseData(); // Load disease descriptions for detail view

        // Clear History functionality
        btnClearHistory.setOnClickListener(v -> {
            File file = new File(getFilesDir(), "history.json");
            if (file.exists() && file.delete()) {
                File[] files = getFilesDir().listFiles();
                for (File f : files) {
                    if (f.getName().startsWith("leaf_")) {
                        f.delete();
                    }
                }

                historyList.clear(); // Clear list
                adapter.notifyDataSetChanged();
                emptyText.setVisibility(View.VISIBLE);
                Toast.makeText(this, "History cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No history to delete.", Toast.LENGTH_SHORT).show();
            }
        });


        // Back Track Button
        Button btnBackMenu = findViewById(R.id.btnBackMenu);
        btnBackMenu.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
            startActivity(intent);
        });

    }

    // Loads saved history entries from history.json into the list
    private void loadHistory() {
        try {
            File file = new File(getFilesDir(), "history.json");
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file); // Read JSON file
                InputStreamReader reader = new InputStreamReader(fis);
                Gson gson = new Gson();
                Type listType = new TypeToken<List<HistoryItem>>(){}.getType();
                historyList = gson.fromJson(reader, listType);
                reader.close();
            } else {
                historyList = new ArrayList<>();
            }
            // Adapter creation
            adapter = new HistoryAdapter(historyList, item -> {
                Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
                intent.putExtra("disease_name", item.getDiseaseName());
                intent.putExtra("confidence", item.getConfidence());
                intent.putExtra("health_status", item.getHealthStatus());
                intent.putExtra("date", item.getFormattedDate());
                intent.putExtra("image_path", item.getGradCamPath());

                 // Include disease details if available
                if (diseaseMap.containsKey(item.getDiseaseName())) {
                    Disease disease = diseaseMap.get(item.getDiseaseName());
                    intent.putExtra("description", disease.getDescription());
                    intent.putExtra("symptoms", disease.getSymptoms());
                    intent.putExtra("treatment", disease.getTreatment());
                }
                startActivity(intent);

            });
            recyclerView.setAdapter(adapter); // Show or hide "No history" message

            // 👇 Add this block here
            TextView emptyText = findViewById(R.id.textEmptyHistory);
            if (historyList.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
            } else {
                emptyText.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load history.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    // Loads disease descriptions, symptoms, and treatments from the local JSON file
    private void loadDiseaseData() {
        try {
            InputStream inputStream = getAssets().open("diseases.json");
            Type type = new TypeToken<Map<String, Disease>>() {}.getType();
            diseaseMap = new Gson().fromJson(new InputStreamReader(inputStream), type);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            diseaseMap = new HashMap<>();
        }
    }


}
