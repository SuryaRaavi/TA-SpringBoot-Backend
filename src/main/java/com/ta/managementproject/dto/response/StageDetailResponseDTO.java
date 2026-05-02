package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StageDetailResponseDTO extends StageResponseDTO {
    private Long totalTask;
    private Long finishedTask;
    private Long todoTask;
    private Long inProgressTask;
    private Double progress;
}
