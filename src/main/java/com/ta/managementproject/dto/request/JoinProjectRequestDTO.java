package com.ta.managementproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinProjectRequestDTO {

    @NotBlank
    @Size(min = 16, max = 16, message = "Join code must be exactly 16 characters")
    private String joinCode;
}
