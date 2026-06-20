package com.ta.managementproject.example;

import java.util.List;


// Total CYC: 4, COG: 1
public class SwitchExample {
    public String getPriorityLabel(int priorityCode) { // CYC: 1
        switch (priorityCode) { // CYC: 3, COG: 1
            case 1:
                return "Low";
            case 2:
                return "Medium";
            case 3:
                return "High";
        }
        return null;
    }
}