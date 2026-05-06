package com.ta.managementproject;

import com.ta.managementproject.dto.request.RegisterRequestDTO;
import com.ta.managementproject.entity.ProjectManager;
import com.ta.managementproject.entity.ProjectMember;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserDb userDb;
    @Mock private RoleDb roleDb;
    @Mock private AESUtil aesUtil;
    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private HttpServletRequest request;
    @Mock private JwtUtils jwtUtils;
    @Mock private UtilService utilService;
    @Mock private AuthService authService;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new RegisterRequestDTO();
        mockRequest.setUsername("newuser");
        mockRequest.setFullName("New User");
        mockRequest.setPassword("password123");
    }

    // Helper: stub utilService.buildResponse
    private void stubBuildResponse(HttpStatus status) {
        when(utilService.buildResponse(eq(status), anyString(), any()))
                .thenReturn(ResponseEntity.status(status).build());
    }

    // ===================== addNewUser =====================

    @Test
    void addNewUser_ShouldReturnCreated_WhenRoleIsProjectManager() throws Exception {
        mockRequest.setRole(1);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectManagerDb.save(any())).thenReturn(new ProjectManager());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = userService.addNewUser(mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void addNewUser_ShouldReturnCreated_WhenRoleIsProjectMember() throws Exception {
        mockRequest.setRole(2);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectMemberDb.save(any())).thenReturn(new ProjectMember());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = userService.addNewUser(mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void addNewUser_ShouldThrowConflictException_WhenUsernameAlreadyExists() {
        mockRequest.setRole(1);

        User existingUser = new ProjectManager();
        when(userDb.findByUsername("newuser")).thenReturn(existingUser);

        ConflictException ex = assertThrows(ConflictException.class, () ->
                userService.addNewUser(mockRequest));

        assertEquals("Username already exist!", ex.getMessage());
    }

    @Test
    void addNewUser_ShouldThrowBadRequestException_WhenRoleIsInvalid() {
        mockRequest.setRole(99);

        when(userDb.findByUsername("newuser")).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.addNewUser(mockRequest));

        assertEquals("Selected role is not valid!", ex.getMessage());
    }

    @Test
    void addNewUser_ShouldThrowBadRequestException_WhenRoleIsZero() {
        mockRequest.setRole(0);

        when(userDb.findByUsername("newuser")).thenReturn(null);

        assertThrows(BadRequestException.class, () ->
                userService.addNewUser(mockRequest));
    }

    @Test
    void addNewUser_ShouldCallProjectManagerDbSave_WhenRoleIsProjectManager() throws Exception {
        mockRequest.setRole(1);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectManagerDb.save(any())).thenReturn(new ProjectManager());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(projectManagerDb, times(1)).save(any(ProjectManager.class));
        verify(projectMemberDb, never()).save(any());
    }

    @Test
    void addNewUser_ShouldCallProjectMemberDbSave_WhenRoleIsProjectMember() throws Exception {
        mockRequest.setRole(2);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectMemberDb.save(any())).thenReturn(new ProjectMember());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(projectMemberDb, times(1)).save(any(ProjectMember.class));
        verify(projectManagerDb, never()).save(any());
    }

    @Test
    void addNewUser_ShouldEncryptPassword_WhenSavingProjectManager() throws Exception {
        mockRequest.setRole(1);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectManagerDb.save(any())).thenReturn(new ProjectManager());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(aesUtil, times(1)).encrypt("password123");
        verify(projectManagerDb).save(argThat(pm -> "encryptedpassword".equals(pm.getPassword())));
    }

    @Test
    void addNewUser_ShouldEncryptPassword_WhenSavingProjectMember() throws Exception {
        mockRequest.setRole(2);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectMemberDb.save(any())).thenReturn(new ProjectMember());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(aesUtil, times(1)).encrypt("password123");
        verify(projectMemberDb).save(argThat(pm -> "encryptedpassword".equals(pm.getPassword())));
    }

    @Test
    void addNewUser_ShouldSetCorrectUsername_WhenSavingProjectManager() throws Exception {
        mockRequest.setRole(1);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MANAGER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectManagerDb.save(any())).thenReturn(new ProjectManager());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(projectManagerDb).save(argThat(pm ->
                "newuser".equals(pm.getUsername()) && "New User".equals(pm.getFullName())));
    }

    @Test
    void addNewUser_ShouldSetCorrectUsername_WhenSavingProjectMember() throws Exception {
        mockRequest.setRole(2);

        when(userDb.findByUsername("newuser")).thenReturn(null);
        when(roleDb.findByName("PROJECT_MEMBER")).thenReturn(new com.ta.managementproject.entity.Role());
        when(aesUtil.encrypt("password123")).thenReturn("encryptedpassword");
        when(projectMemberDb.save(any())).thenReturn(new ProjectMember());
        stubBuildResponse(HttpStatus.CREATED);

        userService.addNewUser(mockRequest);

        verify(projectMemberDb).save(argThat(pm ->
                "newuser".equals(pm.getUsername()) && "New User".equals(pm.getFullName())));
    }

    @Test
    void addNewUser_ShouldNotCheckRoleDb_WhenUsernameAlreadyExists() throws Exception {
        mockRequest.setRole(1);

        when(userDb.findByUsername("newuser")).thenReturn(new ProjectManager());

        assertThrows(ConflictException.class, () -> userService.addNewUser(mockRequest));

        verify(roleDb, never()).findByName(anyString());
    }

    // ===================== getUserRoleByUsername =====================

    @Test
    void getUserRoleByUsername_ShouldReturnProjectManagerRole_WhenUserIsProjectManager() {
        when(userDb.getRoleByUsername("manager1")).thenReturn("PROJECT_MANAGER");

        Role result = userService.getUserRoleByUsername("manager1");

        assertEquals(Role.PROJECT_MANAGER, result);
    }

    @Test
    void getUserRoleByUsername_ShouldReturnProjectMemberRole_WhenUserIsProjectMember() {
        when(userDb.getRoleByUsername("member1")).thenReturn("PROJECT_MEMBER");

        Role result = userService.getUserRoleByUsername("member1");

        assertEquals(Role.PROJECT_MEMBER, result);
    }

    @Test
    void getUserRoleByUsername_ShouldCallUserDbGetRoleByUsername() {
        when(userDb.getRoleByUsername("manager1")).thenReturn("PROJECT_MANAGER");

        userService.getUserRoleByUsername("manager1");

        verify(userDb, times(1)).getRoleByUsername("manager1");
    }

    @Test
    void getUserRoleByUsername_ShouldThrowIllegalArgumentException_WhenRoleIsInvalid() {
        when(userDb.getRoleByUsername("unknown")).thenReturn("INVALID_ROLE");

        assertThrows(IllegalArgumentException.class, () ->
                userService.getUserRoleByUsername("unknown"));
    }
}