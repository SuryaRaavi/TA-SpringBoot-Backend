package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.dto.response.LoginResponseDTO;
import com.ta.managementproject.entity.Role;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.repository.RoleDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.auth.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserDb userDb;
    @Mock private AESUtil aesUtil;
    @Mock private JwtUtils jwtUtils;
    @Mock private RoleDb roleDb;

    // ─── Shared fixtures ──────────────────────────────────────────────────────────

    private Role role;
    private User user;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        role = new Role();
        role.setName("PROJECT_MANAGER");

        user = new User();
        user.setUsername("pm_user");
        user.setPassword("encrypted_password");
        user.setRole(role);

        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("pm_user");
        loginRequest.setPassword("raw_password");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BaseResponseDTO<LoginResponseDTO> extractBody(ResponseEntity<?> response) {
        return (BaseResponseDTO<LoginResponseDTO>) response.getBody();
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // doLogin — Login berhasil
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_withValidCredentials_returnsOk() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("encrypted_password");
        when(jwtUtils.generateJwtToken("pm_user", "PROJECT_MANAGER")).thenReturn("jwt_token");
        when(jwtUtils.getExpirationFromToken("jwt_token")).thenReturn(new Date());

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<LoginResponseDTO> body = extractBody(response);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("Login Successful", body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void doLogin_withValidCredentials_responseDataIsPopulatedCorrectly() throws Exception {
        Date expirationDate = new Date(System.currentTimeMillis() + 86400000);

        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("encrypted_password");
        when(jwtUtils.generateJwtToken("pm_user", "PROJECT_MANAGER")).thenReturn("jwt_token");
        when(jwtUtils.getExpirationFromToken("jwt_token")).thenReturn(expirationDate);

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        LoginResponseDTO data = extractBody(response).getData();
        assertNotNull(data);
        assertEquals("pm_user", data.getUsername());
        assertEquals("jwt_token", data.getToken());
        assertEquals("PROJECT_MANAGER", data.getRole().getRoleName());
        assertEquals(expirationDate, data.getExpirationDate());
    }

    @Test
    void doLogin_withValidCredentials_jwtIsGeneratedWithCorrectParams() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("encrypted_password");
        when(jwtUtils.generateJwtToken("pm_user", "PROJECT_MANAGER")).thenReturn("jwt_token");
        when(jwtUtils.getExpirationFromToken("jwt_token")).thenReturn(new Date());

        authService.doLogin(loginRequest);

        verify(jwtUtils).generateJwtToken("pm_user", "PROJECT_MANAGER");
        verify(jwtUtils).getExpirationFromToken("jwt_token");
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // doLogin — Username tidak ditemukan
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_withUnknownUsername_returnsNotFound() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(null);

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        BaseResponseDTO<LoginResponseDTO> body = extractBody(response);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Username not found!", body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void doLogin_withUnknownUsername_doesNotCallEncryptOrJwt() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(null);

        authService.doLogin(loginRequest);

        verify(aesUtil, never()).encrypt(any());
        verify(jwtUtils, never()).generateJwtToken(any(), any());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // doLogin — Password salah
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_withWrongPassword_returnsUnauthorized() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("wrong_encrypted");
        // "wrong_encrypted" != "encrypted_password" → unauthorized

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        BaseResponseDTO<LoginResponseDTO> body = extractBody(response);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), body.getStatus());
        assertEquals("Username atau password yang dimasukkan salah!", body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void doLogin_withWrongPassword_doesNotGenerateJwt() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("wrong_encrypted");

        authService.doLogin(loginRequest);

        verify(jwtUtils, never()).generateJwtToken(any(), any());
    }

    @Test
    void doLogin_withWrongPassword_responseDataIsNull() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("wrong_encrypted");

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertNull(extractBody(response).getData());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // doLogin — Role PROJECT_MEMBER
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_asProjectMember_returnsCorrectRole() throws Exception {
        Role memberRole = new Role();
        memberRole.setName("PROJECT_MEMBER");

        User memberUser = new User();
        memberUser.setUsername("member_user");
        memberUser.setPassword("encrypted_pass");
        memberUser.setRole(memberRole);

        LoginRequestDTO memberRequest = new LoginRequestDTO();
        memberRequest.setUsername("member_user");
        memberRequest.setPassword("raw_pass");

        when(userDb.findByUsername("member_user")).thenReturn(memberUser);
        when(aesUtil.encrypt("raw_pass")).thenReturn("encrypted_pass");
        when(jwtUtils.generateJwtToken("member_user", "PROJECT_MEMBER")).thenReturn("member_jwt");
        when(jwtUtils.getExpirationFromToken("member_jwt")).thenReturn(new Date());

        ResponseEntity<?> response = authService.doLogin(memberRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        LoginResponseDTO data = extractBody(response).getData();
        assertEquals("member_user", data.getUsername());
        assertEquals("PROJECT_MEMBER", data.getRole().getRoleName());
        assertEquals("member_jwt", data.getToken());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // doLogin — Exception handling
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_whenUserDbThrowsException_returnsInternalServerError() throws Exception {
        when(userDb.findByUsername("pm_user")).thenThrow(new RuntimeException("DB connection failed"));

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        BaseResponseDTO<LoginResponseDTO> body = extractBody(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("DB connection failed", body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void doLogin_whenAesUtilThrowsException_returnsInternalServerError() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenThrow(new RuntimeException("Encryption error"));

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Encryption error", extractBody(response).getMessage());
        verify(jwtUtils, never()).generateJwtToken(any(), any());
    }

    @Test
    void doLogin_whenJwtUtilsThrowsException_returnsInternalServerError() throws Exception {
        when(userDb.findByUsername("pm_user")).thenReturn(user);
        when(aesUtil.encrypt("raw_password")).thenReturn("encrypted_password");
        when(jwtUtils.generateJwtToken("pm_user", "PROJECT_MANAGER"))
                .thenThrow(new RuntimeException("JWT generation failed"));

        ResponseEntity<?> response = authService.doLogin(loginRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("JWT generation failed", extractBody(response).getMessage());
    }
}
