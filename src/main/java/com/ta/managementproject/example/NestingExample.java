package com.ta.managementproject.example;

import java.util.List;

// Total CYC: 5, COG: 11
public class NestingExample {
    public int countOverdueTasks(List<Task> tasks) { // CYC: 1
        int count = 0;
        for (Task task : tasks) { // CYC: 1, COG: 1
            if (task.isOverdue()) { // CYC: 1, COG: 2 (+1 pinalti dari level nesting 1)
                if (task.getPriority() == 3) { // CYC: 1, COG: 3 (+2 pinalti dari level nesting 2)
                    count += 2;
                } else if (task.getPriority() == 4){ // CYC: 1, COG: 3 (+2 pinalti dari level nesting 2)
                    count += 3;
                } else { // COG: 1
                    count += 1;
                }
            } else {
                count += 0; // COG: 1
            }
        }
        return count;
    }
}
