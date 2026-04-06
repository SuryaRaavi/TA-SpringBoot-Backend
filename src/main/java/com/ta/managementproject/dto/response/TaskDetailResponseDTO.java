package com.ta.managementproject.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskDetailResponseDTO {
    private String taskId;
    private String taskName;
    private String description;
    private Integer priority;
    private String label;
    private Instant dueDate;
    private String status;
    private String projectMemberName;
    private Instant createdAt;
}
