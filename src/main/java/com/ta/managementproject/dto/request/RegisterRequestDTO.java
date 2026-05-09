package com.ta.managementproject.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class RegisterRequestDTO {
    @NotBlank(message = "Email name cannot be blank")
    @Email
    private String email;
    @NotBlank(message = "Password cannot be blank")
    private String password;

    @NotBlank(message = "Full name cannot be blank")
    private String fullName;

    @NotNull(message = "Role cannot be blank")
    private int role;
}
