package com.ta.managementproject.dto.response;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProjectDetailResponseDTO extends ProjectResponseDTO{
    private String status;
    private Long totalTask;
    private Long finishedTask;
    private Long todoTask;
    private Long inProgressTask;
    private Double progress;
}
