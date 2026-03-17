package com.ta.managementproject.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReorderRequestDTO {
    private String id;
    private Integer order;
}
