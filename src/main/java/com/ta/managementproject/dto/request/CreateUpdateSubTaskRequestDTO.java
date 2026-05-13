package com.ta.managementproject.dto.request;


import com.ta.managementproject.entity.Task;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUpdateSubTaskRequestDTO {
    @NotBlank(message = "Sub task name cannot be blank")
    private String subTaskName;
    private String description;
    private LocalDate dueDate;
    private String status;
    private String label;
    private Integer order;
    private String projectMember;
}
