package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.entity.ProjectManager;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.Role;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.repository.ProjectManagerDb;
import com.ta.managementproject.repository.ProjectMemberDb;
import com.ta.managementproject.repository.RoleDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserDb userDb;
    @Mock private RoleDb roleDb;
    @Mock private AESUtil aesUtil;
    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private ProjectMemberDb projectMemberDb;

    // ─── Shared fixtures ──────────────────────────────────────────────────────────

    private Role pmRole;
    private Role memberRole;

    @BeforeEach
    void setUp() {
        pmRole = new Role();
        pmRole.setName("PROJECT_MANAGER");

        memberRole = new Role();
        memberRole.setName("PROJECT_MEMBER");
    }

    // ─── Helper: build RegisterRequestDTO ────────────────────────────────────────

    private RegisterRequestDTO buildRequest(String username, String fullName, String password, int role) {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername(username);
        dto.setFullName(fullName);
        dto.setPassword(password);
        dto.setRole(role);
        return dto;
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Role 1 (Project Manager)
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_asProjectManager_returnsCreated() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("secret")).thenReturn("encrypted_secret");

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CREATED.value(), body.getStatus());
        assertTrue(body.getMessage().contains("pm_user"));

        // Harus disimpan ke projectManagerDb, bukan projectMemberDb
        verify(projectManagerDb).save(any(ProjectManager.class));
        verify(projectMemberDb, never()).save(any());
    }

    @Test
    void addNewUser_asProjectManager_savesEncryptedPassword() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "rawPassword", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("rawPassword")).thenReturn("encryptedPassword");

        userService.addNewUser(dto);

        verify(projectManagerDb).save(argThat(pm ->
                pm.getPassword().equals("encryptedPassword") &&
                        pm.getUsername().equals("pm_user") &&
                        pm.getFullName().equals("PM User")
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Role 2 (Project Member)
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_asProjectMember_returnsCreated() throws Exception {
        RegisterRequestDTO dto = buildRequest("member_user", "Member User", "pass123", 2);

        when(userDb.findByUsername("member_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(memberRole);
        when(aesUtil.encrypt("pass123")).thenReturn("encrypted_pass123");

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CREATED.value(), body.getStatus());
        assertTrue(body.getMessage().contains("member_user"));

        // Harus disimpan ke projectMemberDb, bukan projectManagerDb
        verify(projectMemberDb).save(any(ProjectMember.class));
        verify(projectManagerDb, never()).save(any());
    }

    @Test
    void addNewUser_asProjectMember_savesEncryptedPassword() throws Exception {
        RegisterRequestDTO dto = buildRequest("member_user", "Member User", "rawPass", 2);

        when(userDb.findByUsername("member_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(memberRole);
        when(aesUtil.encrypt("rawPass")).thenReturn("encryptedPass");

        userService.addNewUser(dto);

        verify(projectMemberDb).save(argThat(pm ->
                pm.getPassword().equals("encryptedPass") &&
                        pm.getUsername().equals("member_user") &&
                        pm.getFullName().equals("Member User")
        ));
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Username sudah ada (CONFLICT)
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_usernameAlreadyExists_returnsConflict() throws Exception {
        RegisterRequestDTO dto = buildRequest("existing_user", "Existing", "pass", 1);

        User existingUser = new User();
        existingUser.setUsername("existing_user");
        when(userDb.findByUsername("existing_user")).thenReturn(existingUser);

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.CONFLICT.value(), body.getStatus());
        assertEquals("Username already exist!", body.getMessage());

        // Tidak boleh menyimpan apapun
        verify(projectManagerDb, never()).save(any());
        verify(projectMemberDb, never()).save(any());
        verify(aesUtil, never()).encrypt(any());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Role tidak valid (BAD REQUEST)
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_withRole0_returnsBadRequest() {
        RegisterRequestDTO dto = buildRequest("new_user", "New User", "pass", 0);
        when(userDb.findByUsername("new_user")).thenReturn(null);

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Selected role is not valid!", body.getMessage());

        verify(projectManagerDb, never()).save(any());
        verify(projectMemberDb, never()).save(any());
    }

    @Test
    void addNewUser_withRole3_returnsBadRequest() {
        RegisterRequestDTO dto = buildRequest("new_user", "New User", "pass", 3);
        when(userDb.findByUsername("new_user")).thenReturn(null);

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Selected role is not valid!", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    @Test
    void addNewUser_withNegativeRole_returnsBadRequest() {
        RegisterRequestDTO dto = buildRequest("new_user", "New User", "pass", -1);
        when(userDb.findByUsername("new_user")).thenReturn(null);

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Selected role is not valid!", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Response body detail
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_success_responseDataContainsSuccessStatus() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("secret")).thenReturn("encrypted");

        ResponseEntity<?> response = userService.addNewUser(dto);

        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertInstanceOf(CrudResponseDTO.class, body.getData());
        CrudResponseDTO data = (CrudResponseDTO) body.getData();
        assertEquals("SUCCESS", data.getMessage());
    }

    @Test
    void addNewUser_success_timestampIsSet() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("secret")).thenReturn("encrypted");

        ResponseEntity<?> response = userService.addNewUser(dto);

        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertNotNull(body.getTimestamp());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewUser — Exception handling
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewUser_whenUserDbThrowsException_returnsInternalServerError() {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);
        when(userDb.findByUsername("pm_user")).thenThrow(new RuntimeException("DB connection failed"));

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("DB connection failed", body.getMessage());

        CrudResponseDTO data = (CrudResponseDTO) body.getData();
        assertEquals("FAILED", data.getMessage());
        assertEquals("DB connection failed", data.getMessageDetail());
    }

    @Test
    void addNewUser_whenAesUtilThrowsException_returnsInternalServerError() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("secret")).thenThrow(new RuntimeException("Encryption failed"));

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals("Encryption failed", body.getMessage());
        verify(projectManagerDb, never()).save(any());
    }

    @Test
    void addNewUser_whenProjectManagerDbThrowsException_returnsInternalServerError() throws Exception {
        RegisterRequestDTO dto = buildRequest("pm_user", "PM User", "secret", 1);

        when(userDb.findByUsername("pm_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(pmRole);
        when(aesUtil.encrypt("secret")).thenReturn("encrypted");
        doThrow(new RuntimeException("Save failed")).when(projectManagerDb).save(any());

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Save failed", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    @Test
    void addNewUser_whenProjectMemberDbThrowsException_returnsInternalServerError() throws Exception {
        RegisterRequestDTO dto = buildRequest("member_user", "Member User", "secret", 2);

        when(userDb.findByUsername("member_user")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(memberRole);
        when(aesUtil.encrypt("secret")).thenReturn("encrypted");
        doThrow(new RuntimeException("Member save failed")).when(projectMemberDb).save(any());

        ResponseEntity<?> response = userService.addNewUser(dto);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Member save failed", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }
}
