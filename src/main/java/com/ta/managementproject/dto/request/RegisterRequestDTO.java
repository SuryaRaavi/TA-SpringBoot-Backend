package com.ta.managementproject.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterRequestDTO {

    private String username;

    private String password;

    private String fullName;

    private String confirmationPass;

    private String email;

    private int role;
}
