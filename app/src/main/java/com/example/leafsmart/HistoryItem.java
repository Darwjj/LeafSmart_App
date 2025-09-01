package com.example.leafsmart;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Saved detection in the app history functionality.
 * Stores the detected disease, confidence score,
 * health status, timestamp information, and Grad-CAM image path.
 */
public class HistoryItem {
    private long timestamp;
    private String diseaseName;
    private float confidence;
    private String healthStatus;
    private String gradCamPath;

    public HistoryItem(String diseaseName, float confidence, String healthStatus, long timestamp, String gradCamPath) {
        this.diseaseName = diseaseName;
        this.confidence = confidence;
        this.healthStatus = healthStatus;
        this.timestamp = timestamp;
        this.gradCamPath = gradCamPath;
    }

    public long getTimestamp() { return timestamp; }
    public String getDiseaseName() { return diseaseName; }
    public float getConfidence() { return confidence; }
    public String getHealthStatus() { return healthStatus; }
    public String getGradCamPath() { return gradCamPath; }

    public String getFormattedDate() {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}

