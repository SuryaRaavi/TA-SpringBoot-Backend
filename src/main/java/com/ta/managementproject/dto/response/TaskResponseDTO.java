package com.ta.managementproject.dto.response;

import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Stage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDTO {
    private String taskId;

    private String taskName;

    private Integer priority;

    private Instant dueDate;

    private String status;

    private String projectMemberName;

    private String label;

    private Instant createdAt;

    private Integer order;
}
