package com.ta.managementproject.example;

// Total CYC: 3, COG: 3
public class MultipleElseIfExample {

    public String evaluateProjectStatus(int progressPercentage, boolean isOverdue) { // CYC: 1
        if (progressPercentage == 100) { // CYC: 1, COG: 1
            return "Completed";
        } else if (progressPercentage == 50) { // CYC: 1, COG: 1
            return "In Progress";
        } else { // COG: 1
            return "Not Started";
        }
    }
}
