//package com.ta.managementproject.measure_test;
//
//import com.ta.managementproject.dto.request.LoginRequestDTO;
//import com.ta.managementproject.entity.*;
//import com.ta.managementproject.exception.ForbiddenException;
//import com.ta.managementproject.exception.NotFoundException;
//import com.ta.managementproject.repository.*;
//import com.ta.managementproject.security.util.AESUtil;
//import com.ta.managementproject.security.util.JwtUtils;
//import com.ta.managementproject.service.UtilService;
//import com.ta.managementproject.service.auth.AuthServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.Date;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthServiceTest {
//
//    @Mock private AESUtil aesUtil;
//    @Mock private UserDb userDb;
//    @Mock private JwtUtils jwtUtils;
//    @Mock private RoleDb roleDb;
//    @Mock private UtilService utilService;
//    @Mock private ProjectDb projectDb;
//    @Mock private StageDb stageDb;
//    @Mock private TaskDb taskDb;
//    @Mock private SubTaskDb subTaskDb;
//
//    @InjectMocks
//    private AuthServiceImpl authService;
//
//    private Role mockRole;
//    private User mockUser;
//    private Project mockProject;
//    private User mockManager;
//
//    @BeforeEach
//    void setUp() {
//        mockRole = new Role();
//        mockRole.setName("MANAGER");
//
//        mockUser = new User();
//        mockUser.setUsername("john");
//        mockUser.setPassword("encryptedPassword");
//        mockUser.setRole(mockRole);
//
//        mockManager = new ProjectManager();
//        mockManager.setUsername("manager1");
//
//        mockProject = Project.builder()
//                .projectId("project-1")
//                .projectManager((ProjectManager) mockManager)
//                .memberInProjectList(List.of())
//                .isCancelled(false)
//                .build();
//    }
//
//    // ===================== doLogin =====================
//
//    @Test
//    void doLogin_ShouldThrowNotFoundException_WhenUsernameNotFound() {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("unknown");
//        request.setPassword("pass");
//
//        when(userDb.findByUsername("unknown")).thenReturn(null);
//
//        assertThrows(NotFoundException.class, () -> authService.doLogin(request));
//    }
//
//    @Test
//    void doLogin_ShouldThrowForbiddenException_WhenPasswordWrong() throws Exception {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("john");
//        request.setPassword("wrongPass");
//
//        when(userDb.findByUsername("john")).thenReturn(mockUser);
//        when(aesUtil.encrypt("wrongPass")).thenReturn("wrongEncrypted");
//
//        assertThrows(ForbiddenException.class, () -> authService.doLogin(request));
//    }
//
//    @Test
//    void doLogin_ShouldReturnOk_WhenCredentialsValid() throws Exception {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("john");
//        request.setPassword("correctPass");
//
//        when(userDb.findByUsername("john")).thenReturn(mockUser);
//        when(aesUtil.encrypt("correctPass")).thenReturn("encryptedPassword");
//        when(jwtUtils.generateJwtToken("john", "MANAGER")).thenReturn("jwt-token");
//        when(jwtUtils.getExpirationFromToken("jwt-token")).thenReturn(new Date());
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Login Successful"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> response = authService.doLogin(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void doLogin_ShouldCallGenerateJwtToken_WhenCredentialsValid() throws Exception {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("john");
//        request.setPassword("correctPass");
//
//        when(userDb.findByUsername("john")).thenReturn(mockUser);
//        when(aesUtil.encrypt("correctPass")).thenReturn("encryptedPassword");
//        when(jwtUtils.generateJwtToken("john", "MANAGER")).thenReturn("jwt-token");
//        when(jwtUtils.getExpirationFromToken("jwt-token")).thenReturn(new Date());
//        when(utilService.buildResponse(any(), any(), any())).thenReturn(ResponseEntity.ok().build());
//
//        authService.doLogin(request);
//
//        verify(jwtUtils, times(1)).generateJwtToken("john", "MANAGER");
//    }
//
//    @Test
//    void doLogin_ShouldThrowNotFoundException_WithCorrectMessage() {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("ghost");
//        request.setPassword("pass");
//
//        when(userDb.findByUsername("ghost")).thenReturn(null);
//
//        NotFoundException ex = assertThrows(NotFoundException.class, () -> authService.doLogin(request));
//        assertEquals("USERNAME_NOT_FOUND", ex.getMessage());
//    }
//
//    @Test
//    void doLogin_ShouldThrowForbiddenException_WithCorrectMessage() throws Exception {
//        LoginRequestDTO request = new LoginRequestDTO();
//        request.setUsername("john");
//        request.setPassword("wrongPass");
//
//        when(userDb.findByUsername("john")).thenReturn(mockUser);
//        when(aesUtil.encrypt("wrongPass")).thenReturn("wrongEncrypted");
//
//        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.doLogin(request));
//        assertEquals("Username atau password yang dimasukkan salah!", ex.getMessage());
//    }
//
//    // ===================== validateProject =====================
//
//    @Test
//    void validateProject_ShouldReturnProject_WhenFound() {
//        when(projectDb.findByProjectId("project-1")).thenReturn(mockProject);
//
//        Project result = authService.validateProject("project-1");
//
//        assertEquals(mockProject, result);
//    }
//
//    @Test
//    void validateProject_ShouldThrowNotFoundException_WhenNotFound() {
//        when(projectDb.findByProjectId("project-x")).thenReturn(null);
//
//        NotFoundException ex = assertThrows(NotFoundException.class,
//                () -> authService.validateProject("project-x"));
//        assertEquals("PROJECT_NOT_FOUND", ex.getMessage());
//    }
//
//    // ===================== validateManagerAccess =====================
//
//    @Test
//    void validateManagerAccess_ShouldPass_WhenUsernameIsManager() {
//        assertDoesNotThrow(() -> authService.validateManagerAccess(mockProject, "manager1"));
//    }
//
//    @Test
//    void validateManagerAccess_ShouldThrowForbiddenException_WhenNotManager() {
//        ForbiddenException ex = assertThrows(ForbiddenException.class,
//                () -> authService.validateManagerAccess(mockProject, "other_user"));
//        assertEquals("FORBIDDEN", ex.getMessage());
//    }
//
//    // ===================== validateManagerAndMemberAccess =====================
//
//    @Test
//    void validateManagerAndMemberAccess_ShouldPass_WhenUserIsManager() {
//        assertDoesNotThrow(() ->
//                authService.validateManagerAndMemberAccess(mockProject, "manager1"));
//    }
//
//    @Test
//    void validateManagerAndMemberAccess_ShouldPass_WhenUserIsMember() {
//        User member = new ProjectMember();
//        member.setUsername("member1");
//
//        MemberInProject mip = new MemberInProject();
//        mip.setProjectMember((ProjectMember) member);
//
//        Project projectWithMember = mockProject.toBuilder()
//                .memberInProjectList(List.of(mip))
//                .build();
//
//        assertDoesNotThrow(() ->
//                authService.validateManagerAndMemberAccess(projectWithMember, "member1"));
//    }
//
//    @Test
//    void validateManagerAndMemberAccess_ShouldThrowForbiddenException_WhenNotManagerNorMember() {
//        ForbiddenException ex = assertThrows(ForbiddenException.class,
//                () -> authService.validateManagerAndMemberAccess(mockProject, "outsider"));
//        assertEquals("Access Not Allowed!", ex.getMessage());
//    }
//
//    // ===================== validateStage =====================
//
//    @Test
//    void validateStage_ShouldReturnStage_WhenFound() {
//        Stage mockStage = Stage.builder().stageId("stage-1").build();
//        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
//
//        Stage result = authService.validateStage("stage-1");
//
//        assertEquals(mockStage, result);
//    }
//
//    @Test
//    void validateStage_ShouldThrowNotFoundException_WhenNotFound() {
//        when(stageDb.findByStageId("stage-x")).thenReturn(null);
//
//        NotFoundException ex = assertThrows(NotFoundException.class,
//                () -> authService.validateStage("stage-x"));
//        assertEquals("STAGE_NOT_FOUND", ex.getMessage());
//    }
//
//    // ===================== validateTask =====================
//
//    @Test
//    void validateTask_ShouldReturnTask_WhenFound() {
//        Task mockTask = Task.builder().taskId("task-1").build();
//        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
//
//        Task result = authService.validateTask("task-1");
//
//        assertEquals(mockTask, result);
//    }
//
//    @Test
//    void validateTask_ShouldThrowNotFoundException_WhenNotFound() {
//        when(taskDb.findByTaskId("task-x")).thenReturn(null);
//
//        NotFoundException ex = assertThrows(NotFoundException.class,
//                () -> authService.validateTask("task-x"));
//        assertEquals("TASK_NOT_FOUND", ex.getMessage());
//    }
//
//    // ===================== validateSubTask =====================
//
//    @Test
//    void validateSubTask_ShouldReturnSubTask_WhenFound() {
//        SubTask mockSubTask = SubTask.builder().subTaskId("subtask-1").build();
//        when(subTaskDb.findSubTaskBySubTaskId("subtask-1")).thenReturn(mockSubTask);
//
//        SubTask result = authService.validateSubTask("subtask-1");
//
//        assertEquals(mockSubTask, result);
//    }
//
//    @Test
//    void validateSubTask_ShouldThrowNotFoundException_WhenNotFound() {
//        when(subTaskDb.findSubTaskBySubTaskId("subtask-x")).thenReturn(null);
//
//        NotFoundException ex = assertThrows(NotFoundException.class,
//                () -> authService.validateSubTask("subtask-x"));
//        assertEquals("SUB_TASK_NOT_FOUND", ex.getMessage());
//    }
//
//    // ===================== validateProjectCancellation =====================
//
//    @Test
//    void validateProjectCancellation_ShouldPass_WhenProjectNotCancelled() {
//        assertDoesNotThrow(() -> authService.validateProjectCancellation(mockProject));
//    }
//
//    @Test
//    void validateProjectCancellation_ShouldThrowForbiddenException_WhenCancelled() {
//        Project cancelledProject = mockProject.toBuilder()
//                .isCancelled(true)
//                .build();
//
//        ForbiddenException ex = assertThrows(ForbiddenException.class,
//                () -> authService.validateProjectCancellation(cancelledProject));
//        assertEquals("Project has been cancelled!", ex.getMessage());
//    }
//}