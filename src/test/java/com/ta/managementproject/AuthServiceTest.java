package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.dto.response.LoginResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.ForbiddenException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import java.util.ArrayList;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private AESUtil aesUtil;

    @Mock
    private UserDb userDb;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RoleDb roleDb;

    @Mock
    private UtilService utilService;

    @Mock
    private ProjectDb projectDb;

    @Mock
    private StageDb stageDb;

    @Mock
    private TaskDb taskDb;

    @Mock
    private SubTaskDb subTaskDb;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Shared test fixtures ──────────────────────────────────────────────────

    private User mockUser;
    private Role mockRole;
    private Project mockProject;
    private Stage mockStage;
    private Task mockTask;
    private SubTask mockSubTask;
    private MemberInProject mockMember;

    @BeforeEach
    void setUp() {
        mockRole = new Role();
        mockRole.setName("MANAGER");

        mockUser = new User();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encryptedPassword");
        mockUser.setRole(mockRole);

        // Manager user
        User managerUser = new ProjectManager();
        managerUser.setUsername("manager");

        mockProject = new Project();
        mockProject.setProjectId("PROJECT-001");
        mockProject.setProjectManager((ProjectManager) managerUser);
        mockProject.setMemberInProjectList(new ArrayList<>());

        mockStage = new Stage();
        mockStage.setStageId("STAGE-001");

        mockTask = new Task();
        mockTask.setTaskId("TASK-001");

        mockSubTask = new SubTask();
        mockSubTask.setSubTaskId("SUBTASK-001");

        // Member setup
        User memberUser = new ProjectMember();
        memberUser.setUsername("member");

        mockMember = new MemberInProject();
        mockMember.setProjectMember((ProjectMember) memberUser);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // doLogin
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void doLogin_Success() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("testuser");
        request.setPassword("plainPassword");

        when(userDb.findByUsername("testuser")).thenReturn(mockUser);
        when(aesUtil.encrypt("plainPassword")).thenReturn("encryptedPassword");
        when(jwtUtils.generateJwtToken("testuser", "MANAGER")).thenReturn("jwt-token-123");
        when(jwtUtils.getExpirationFromToken("jwt-token-123")).thenReturn(new Date());

        ResponseEntity<BaseResponseDTO<Object>> expectedResponse = ResponseEntity.ok().build();
        when(utilService.buildResponse(any(HttpStatus.class), anyString(), any())).thenReturn(expectedResponse);

        // Act
        ResponseEntity<?> result = authService.doLogin(request);

        // Assert
        assertNotNull(result);
        verify(userDb).findByUsername("testuser");
        verify(aesUtil).encrypt("plainPassword");
        verify(jwtUtils).generateJwtToken("testuser", "MANAGER");
        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("Login Successful"), any(LoginResponseDTO.class));
    }

    @Test
    void doLogin_UsernameNotFound_ThrowsNotFoundException() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("unknown");
        request.setPassword("password");

        when(userDb.findByUsername("unknown")).thenReturn(null);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> authService.doLogin(request));

        assertEquals("USERNAME_NOT_FOUND", exception.getMessage());
        verify(userDb).findByUsername("unknown");
        verifyNoInteractions(aesUtil, jwtUtils, utilService);
    }

    @Test
    void doLogin_WrongPassword_ThrowsForbiddenException() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("testuser");
        request.setPassword("wrongPassword");

        when(userDb.findByUsername("testuser")).thenReturn(mockUser);
        when(aesUtil.encrypt("wrongPassword")).thenReturn("wrongEncrypted");

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> authService.doLogin(request));

        assertEquals("Username atau password yang dimasukkan salah!", exception.getMessage());
        verify(aesUtil).encrypt("wrongPassword");
        verifyNoInteractions(jwtUtils, utilService);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateProject
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateProject_Success() {
        // Arrange
        when(projectDb.findByProjectId("PROJECT-001")).thenReturn(mockProject);

        // Act
        Project result = authService.validateProject("PROJECT-001");

        // Assert
        assertNotNull(result);
        assertEquals("PROJECT-001", result.getProjectId());
        verify(projectDb).findByProjectId("PROJECT-001");
    }

    @Test
    void validateProject_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(projectDb.findByProjectId("INVALID-ID")).thenReturn(null);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> authService.validateProject("INVALID-ID"));

        assertEquals("PROJECT_NOT_FOUND", exception.getMessage());
        verify(projectDb).findByProjectId("INVALID-ID");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateManagerAccess
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateManagerAccess_Success_WhenUsernameMatchesManager() {
        // Act & Assert — should NOT throw
        assertDoesNotThrow(() ->
                authService.validateManagerAccess(mockProject, "manager"));
    }

    @Test
    void validateManagerAccess_Forbidden_WhenUsernameDoesNotMatch() {
        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> authService.validateManagerAccess(mockProject, "otherUser"));

        assertEquals("FORBIDDEN", exception.getMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateManagerAndMemberAccess
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateManagerAndMemberAccess_Success_WhenManager() {
        // Act & Assert — manager username passes
        assertDoesNotThrow(() ->
                authService.validateManagerAndMemberAccess(mockProject, "manager"));
    }

    @Test
    void validateManagerAndMemberAccess_Success_WhenMember() {
        // Arrange — add a member to the project
        mockProject.getMemberInProjectList().add(mockMember);

        // Act & Assert — member username passes
        assertDoesNotThrow(() ->
                authService.validateManagerAndMemberAccess(mockProject, "member"));
    }

    @Test
    void validateManagerAndMemberAccess_Forbidden_WhenNeitherManagerNorMember() {
        // Arrange — no members in project
        mockProject.setMemberInProjectList(new ArrayList<>());

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> authService.validateManagerAndMemberAccess(mockProject, "stranger"));

        assertEquals("Access Not Allowed!", exception.getMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateStage
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateStage_Success() {
        // Arrange
        when(stageDb.findByStageId("STAGE-001")).thenReturn(mockStage);

        // Act
        Stage result = authService.validateStage("STAGE-001");

        // Assert
        assertNotNull(result);
        assertEquals("STAGE-001", result.getStageId());
        verify(stageDb).findByStageId("STAGE-001");
    }

    @Test
    void validateStage_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(stageDb.findByStageId("INVALID-STAGE")).thenReturn(null);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> authService.validateStage("INVALID-STAGE"));

        assertEquals("STAGE_NOT_FOUND", exception.getMessage());
        verify(stageDb).findByStageId("INVALID-STAGE");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateTask
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateTask_Success() {
        // Arrange
        when(taskDb.findByTaskId("TASK-001")).thenReturn(mockTask);

        // Act
        Task result = authService.validateTask("TASK-001");

        // Assert
        assertNotNull(result);
        assertEquals("TASK-001", result.getTaskId());
        verify(taskDb).findByTaskId("TASK-001");
    }

    @Test
    void validateTask_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(taskDb.findByTaskId("INVALID-TASK")).thenReturn(null);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> authService.validateTask("INVALID-TASK"));

        assertEquals("TASK_NOT_FOUND", exception.getMessage());
        verify(taskDb).findByTaskId("INVALID-TASK");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateSubTask
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    void validateSubTask_Success() {
        // Arrange
        when(subTaskDb.findSubTaskBySubTaskId("SUBTASK-001")).thenReturn(mockSubTask);

        // Act
        SubTask result = authService.validateSubTask("SUBTASK-001");

        // Assert
        assertNotNull(result);
        assertEquals("SUBTASK-001", result.getSubTaskId());
        verify(subTaskDb).findSubTaskBySubTaskId("SUBTASK-001");
    }

    @Test
    void validateSubTask_NotFound_ThrowsNotFoundException() {
        // Arrange
        when(subTaskDb.findSubTaskBySubTaskId("INVALID-SUBTASK")).thenReturn(null);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> authService.validateSubTask("INVALID-SUBTASK"));

        assertEquals("SUB_TASK_NOT_FOUND", exception.getMessage());
        verify(subTaskDb).findSubTaskBySubTaskId("INVALID-SUBTASK");
    }

    @Test
    void validateProjectCancellation_Success(){
        assertDoesNotThrow(() ->
                authService.validateProjectCancellation(mockProject));
    }

    @Test
    void validateProjectCancellation_ThrowsConflictException(){
        mockProject.setCancelled(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> authService.validateProjectCancellation(mockProject));

        assertEquals("Project has been cancelled!", exception.getMessage());
    }
}
