package com.ta.managementproject.example;

// Total CYC: 4, COG: 4
public class IfElseExample {
    public boolean isTaskAssignable(boolean isMemberAvailable, boolean hasCapacity, boolean isApproved) { // CYC: 1
        if ((isMemberAvailable && hasCapacity) || isApproved) { // CYC: 3, COG: 3
            return true;
        } else { // COG: 1
            return false;
        }
    }
}
