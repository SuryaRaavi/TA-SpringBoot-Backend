package com.ta.managementproject.dto.response;

import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Stage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponseDTO {
    private String taskId;

    private String taskName;

    private Integer priority;

    private LocalDate dueDate;

    private String status;

    private String projectMemberName;

    private String label;
}
