package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProjectDetailResponseDTO {
    private String projectId;
    private String projectName;
    private String description;
    private String fullNamePm;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
