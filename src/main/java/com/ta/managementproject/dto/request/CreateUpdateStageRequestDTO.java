package com.ta.managementproject.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateUpdateStageRequestDTO {
    @NotBlank(message = "Stage name cannot be blank")
    private String stageName;
    private String description;

}
