package com.ta.managementproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReorderRequestDTO {
    @NotBlank(message = "Id cannot be blank")
    private String id;

    @NotNull(message = "Order cannot be blank")
    private Integer order;
}
