package com.ta.managementproject.dto.request;


import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Stage;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.Instant;
import java.time.LocalDate;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUpdateTaskRequestDTO {
    @NotNull(message = "Task name cannot be blank")
    private String taskName;

    private String description;

    @NotNull(message = "Priority cannot be blank")
    private Integer priority;

    private LocalDate dueDate;

    private String status;

    private ProjectMember projectMember;

    private Stage stage;
}
