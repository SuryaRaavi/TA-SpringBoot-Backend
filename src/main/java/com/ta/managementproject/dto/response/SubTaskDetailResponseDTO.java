package com.ta.managementproject.dto.response;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Task;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SubTaskDetailResponseDTO {
    private String subTaskId;
    private String subTaskName;
    private String description;
    private Instant dueDate;
    private String status;
    private String label;
    private String projectMemberName;
}
