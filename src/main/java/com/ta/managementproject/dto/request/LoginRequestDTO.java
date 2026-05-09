package com.ta.managementproject.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class LoginRequestDTO {
    @NotBlank(message = "Email cannot be blank")
    @Email
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}
