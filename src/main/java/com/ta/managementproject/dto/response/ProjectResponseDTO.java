package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class ProjectResponseDTO {
    private String projectId;
    private String projectName;
    private String description;
    private String fullNamePm;
    private Instant startDate;
    private Instant endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private String status;
    private Long totalTask;
    private Long finishedTask;
    private Long todoTask;
    private Long inProgressTask;
    private Double progress;
    private boolean isCancelled;
}
