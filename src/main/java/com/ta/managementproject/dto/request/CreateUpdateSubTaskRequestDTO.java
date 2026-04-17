package com.ta.managementproject.dto.request;

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
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUpdateSubTaskRequestDTO {
    private String subTaskName;
    private String description;
    private LocalDate dueDate;
    private String status;
    private String label;
    private Integer order;
    private ProjectMember projectMember;
    private Task task;
}
