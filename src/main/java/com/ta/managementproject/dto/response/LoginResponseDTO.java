package com.ta.managementproject.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginResponseDTO {
    private String email;
    private RoleResponseDTO role;
    private String token;
    private Date expirationDate;

}
