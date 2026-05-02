package com.ta.managementproject.restcontroller;


import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.service.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login") // CYC: 7, LOC: 49
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequestDTO) throws Exception{ // CYC: 1, LOC: 4
        return authService.doLogin(loginRequestDTO);
    }
}
