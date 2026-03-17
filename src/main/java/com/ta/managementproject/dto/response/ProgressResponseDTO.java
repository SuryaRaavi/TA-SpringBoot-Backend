package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProgressResponseDTO {
    private Double progress;
    private Integer totalTask;
    private Integer finishedTask;
    private Integer todoTask;
    private Integer inProgressTask;
}
