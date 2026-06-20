package com.ta.managementproject.example;

// Total CYC: 2, COG: 1
public class ShorthandExample {
    public String getCompletionLabel(boolean isCompleted) { // CYC: 1
        return isCompleted ? "Done" : "Pending"; // CYC: 1, COG: 1
    }
}
