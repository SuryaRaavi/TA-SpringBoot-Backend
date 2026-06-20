package com.ta.managementproject.example;

import java.util.List;


// Total CYC: 5, COG: 1
public class SwitchExample {
    public String getPriorityLabel(int priorityCode) { // CYC: 1
        switch (priorityCode) { // CYC: 4, COG: 1
            case 1:
                return "Low";
            case 2:
                return "Medium";
            case 3:
                return "High";
            default:
                return "Unknown";
        }
    }
}