package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class StageResponseDTO {
    private String stageId;
    private String stageName;
    private Integer order;

    private Long totalTask;
    private Long finishedTask;
    private Long todoTask;
    private Long inProgressTask;
    private Double progress;
}
