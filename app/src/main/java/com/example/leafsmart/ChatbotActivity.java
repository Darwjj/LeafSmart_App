package com.example.leafsmart;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interactive plant care chatbot.
 * Users can either type a question or use quick-access button to ask about diseases and care tips.
 * The chatbot responds based on a local Q&A knowledge base store in the JSON file, plant_qa.json
 */
public class ChatbotActivity extends AppCompatActivity {

    private LinearLayout chatLayout; // Container for chat design
    private EditText chatInput; // User input text
    private Button sendButton; // Button to fired up the message
    private JSONObject qaData; // Q&A Knowledge base

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        chatLayout = findViewById(R.id.chatLayout);
        chatInput = findViewById(R.id.chatInput);
        sendButton = findViewById(R.id.sendButton);

        // Back button to return to main menu
        Button btnBackMenu = findViewById(R.id.btnBackMenu);
        btnBackMenu.setOnClickListener(v -> {
            Intent intent = new Intent(ChatbotActivity.this, MainActivity.class);
            startActivity(intent);
        });
        // Load Q&A Knowledge based from the JSON file
        qaData = loadJSONFromAsset("plant_qa.json");

        LinearLayout quickButtonsLayout = findViewById(R.id.quickButtonsLayout);  // Layout for quick-access category

        if (qaData != null) { // Set quick-access category buttons with an emoji (more straight forward than an layout import)
            Map<String, String> groups = new LinkedHashMap<>();
            groups.put("🍅 Tomato", "tomato");
            groups.put("🍎 Apple", "apple");
            groups.put("🍇 Grape", "grape");
            groups.put("🌽 Corn", "corn");
            groups.put("🌶️ Pepper", "pepper");
            groups.put("🥔 Potato", "potato");
            groups.put("🍓 Strawberry", "strawberry");
            groups.put("🪴 General Care", "care");
            groups.put("🧪 Tips & Healthy", "tips,healthy");
            groups.put("🧬 Show Commands", "show");

            // For each category, add a label and related quick buttons
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                String groupTitle = entry.getKey();
                String[] keywords = entry.getValue().split(",");

                // Add category title
                TextView groupHeader = new TextView(this);
                groupHeader.setText(groupTitle);
                groupHeader.setTextSize(16f);
                groupHeader.setTextColor(Color.BLACK);
                groupHeader.setPadding(10, 20, 10, 5);
                groupHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                quickButtonsLayout.addView(groupHeader);

                // Loop through Q&A entries to find matches for the group
                Iterator<String> keys = qaData.keys();
                boolean anyMatch = false;

                while (keys.hasNext()) {
                    String key = keys.next();
                    boolean matched = false;

                    for (String keyword : keywords) {
                        if (key.toLowerCase().contains(keyword)) {
                            matched = true;
                            break;
                        }
                    }

                    if (matched) {
                        Button quickBtn = new Button(this);
                        quickBtn.setText(capitalizeFirstLetter(key));
                        quickBtn.setAllCaps(false);
                        quickBtn.setTextSize(14f);
                        quickBtn.setPadding(30, 20, 30, 20);
                        quickBtn.setBackgroundResource(R.drawable.bubble_user);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        params.setMargins(10, 5, 10, 5);
                        quickBtn.setLayoutParams(params);

                        quickBtn.setOnClickListener(v -> { // Button for pre-set question
                            chatInput.setText(key);
                            sendButton.performClick();
                        });

                        quickButtonsLayout.addView(quickBtn);
                        anyMatch = true;
                    }
                }
                // Remove the category header if no matches found
                if (!anyMatch) {
                    quickButtonsLayout.removeView(groupHeader);
                }
            }
        }
        // Sending message functionality
        sendButton.setOnClickListener(v -> {
            String question = chatInput.getText().toString().trim();
            if (!question.isEmpty()) {
                addChatBubble(question, true); // Add user message
                String response = getAnswer(question.toLowerCase()); // Get bot reply
                addChatBubble(response, false);
                chatInput.setText("");
            }
        });
    }

    // Set styled chat bubble to the chat layout
    private void addChatBubble(String message, boolean isUser) {
        TextView chatBubble = new TextView(this);
        chatBubble.setText(message);
        chatBubble.setTextColor(Color.WHITE);
        chatBubble.setPadding(25, 20, 25, 20);
        chatBubble.setBackgroundResource(isUser ? R.drawable.bubble_user : R.drawable.bubble_bot);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = isUser ? Gravity.END : Gravity.START;
        params.setMargins(10, 10, 10, 10);
        chatBubble.setLayoutParams(params);
        chatLayout.addView(chatBubble);

        // Automatic Scroll to the latest message
        chatLayout.post(() -> ((ScrollView) chatLayout.getParent()).fullScroll(ScrollView.FOCUS_DOWN));
    }
    // Capitalises the first letter of a string
    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    // Finds the most relevant answer from the Q&A dataset
    // Using two-phase search: Keyword fragment match and fallback to Levenshtein similarity if not strong match found
    private String getAnswer(String question) {
        try {
            question = question.toLowerCase();
            String[] inputWords = question.split("\\s+");
            Iterator<String> keys = qaData.keys();

            // First step: direct Keyword/Fragment match
            while (keys.hasNext()) {
                String key = keys.next();
                String[] keyWords = key.toLowerCase().split("\\s+");

                int matchCount = 0;
                for (String word : keyWords) {
                    for (String inputWord : inputWords) {
                        if (inputWord.contains(word) || word.contains(inputWord)) {
                            matchCount++;
                        }
                    }
                }

                if (matchCount >= 2 || question.contains(key.toLowerCase())) {
                    return qaData.getString(key);
                }
            }

            // Second step: similarity match using Levenshtein distance
            keys = qaData.keys();  // reset iterator
            String bestMatchKey = null;
            double highestSimilarity = 0.0;

            while (keys.hasNext()) {
                String key = keys.next();
                String keyLower = key.toLowerCase();
                int distance = levenshteinDistance(question, keyLower);
                int maxLen = Math.max(question.length(), keyLower.length());

                if (maxLen == 0) continue;

                double similarity = 1.0 - ((double) distance / maxLen);

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                    bestMatchKey = key;
                }
            }

            // Only return if similarity is high enough (e.g. 0.6+)
            if (highestSimilarity >= 0.6 && bestMatchKey != null) {
                return qaData.getString(bestMatchKey);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "Sorry, I couldn't understand that.\n" +
                "Try including keywords like 'blight', 'mildew', 'bacterial spot', or crop names like 'tomato', 'apple', etc.";
    }
    // Calculates Levenshtein distance between two strings
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    // Loads a JSON file from the assets folder into JSONObject
    private JSONObject loadJSONFromAsset(String filename) {
        String json;
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
