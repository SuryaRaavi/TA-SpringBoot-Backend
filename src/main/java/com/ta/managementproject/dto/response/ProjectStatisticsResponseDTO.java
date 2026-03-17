package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStatisticsResponseDTO {
    Double projectProgress;
    Integer totalStage;
    Integer totalFinishedStage;
    Integer totalUnfinishedStage;
}
