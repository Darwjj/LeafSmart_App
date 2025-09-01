package com.example.leafsmart;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        // Set the layout for the main menu screen

        // Set activities buttons
        Button btnDetect = findViewById(R.id.btnDetect);
        Button btnHowItWorks = findViewById(R.id.btnHowItWorks);
        Button btnAbout = findViewById(R.id.btnAbout);
        Button btnKnowledgeBase = findViewById(R.id.btnKnowledgeBase);
        Button btnHistory = findViewById(R.id.btnHistory);
        Button btnExit = findViewById(R.id.btnExit);
        Button btnChatbot = findViewById(R.id.btnChatbot);

        //Detection Activity
        btnDetect.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetectionActivity.class);
            startActivity(intent);
        });
        // How Works Activity
        btnHowItWorks.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HowWorksActivity.class);
            startActivity(intent);
        });
        //  About Activity
        btnAbout.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });
        // Disease Activity
        btnKnowledgeBase.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DiseaseListActivity.class);
            startActivity(intent);
        });
        // History Activity
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
        // Chatbot Activity
        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChatbotActivity.class);
            startActivity(intent);
        });
        // Exit Functionality
        btnExit.setOnClickListener(v -> {
            finishAffinity();
        });
    }
}
