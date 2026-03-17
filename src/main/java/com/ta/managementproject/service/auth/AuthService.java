package com.ta.managementproject.service.auth;

import com.ta.managementproject.dto.request.LoginRequestDTO;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> doLogin(LoginRequestDTO request) throws Exception;
}
