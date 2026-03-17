package com.ta.managementproject.dto.response;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.SubTask;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TaskDetailResponseDTO {
    private String taskId;
    private String taskName;
    private String description;
    private Integer priority;
    private String label;
    private LocalDate dueDate;
    private String status;
    private String projectMemberName;
    private LocalDateTime createdAt;
}
