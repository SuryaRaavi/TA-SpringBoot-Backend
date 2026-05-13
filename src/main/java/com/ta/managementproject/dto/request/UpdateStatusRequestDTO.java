package com.ta.managementproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatusRequestDTO {
    @NotBlank(message = "Status cannot be blank")
    private String status;
}
