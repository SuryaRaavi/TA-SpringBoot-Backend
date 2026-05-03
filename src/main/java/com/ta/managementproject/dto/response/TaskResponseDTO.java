package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TaskResponseDTO {
    private String taskId;
    private String taskName;
    private Integer priority;
    private Instant dueDate;
    private String status;
    private String assigneeId;
    private String label;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer order;
    private String stageId;
    private boolean isDeleted;
}
