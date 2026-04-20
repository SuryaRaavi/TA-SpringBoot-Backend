package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProjectDetailResponseDTO {
    private String projectId;
    private String projectName;
    private String description;
    private String fullNamePm;
    private Instant startDate;
    private Instant endDate;
    private String status;
}
