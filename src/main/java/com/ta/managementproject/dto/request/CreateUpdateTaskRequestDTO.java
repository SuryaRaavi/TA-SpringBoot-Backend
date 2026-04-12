package com.ta.managementproject.dto.request;


import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Stage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.Instant;



@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUpdateTaskRequestDTO {
    private String taskName;

    private String description;

    private Integer priority;

    private Instant dueDate;

    private String status;

    private ProjectMember projectMember;

    private Stage stage;
}
