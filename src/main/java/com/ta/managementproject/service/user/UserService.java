package com.ta.managementproject.service.user;

import com.ta.managementproject.dto.request.RegisterRequestDTO;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<?> addNewUser(RegisterRequestDTO requestDTO);
}
