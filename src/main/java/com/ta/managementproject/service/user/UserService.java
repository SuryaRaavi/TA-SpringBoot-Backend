package com.ta.managementproject.service.user;

import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.enums.Role;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<?> addNewUser(RegisterRequestDTO requestDTO) throws Exception;

    Role getUserRoleByEmail(String email);
}
