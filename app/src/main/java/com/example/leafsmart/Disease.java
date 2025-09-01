package com.example.leafsmart;

/**
 * Model Class in charge of represent plant disease.
 * Stores key details, such as disease description, common symptoms and recommended treatment
 * Data load from the local diseases.json file
 */
public class Disease {
    private String description;
    private String symptoms;
    private String treatment;

    public String getDescription() { return description; }
    public String getSymptoms() { return symptoms; }
    public String getTreatment() { return treatment; }
}

