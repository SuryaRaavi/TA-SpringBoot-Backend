package com.ta.managementproject.measure_test;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.ProjectManager;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.stage.StageServiceImpl;
import com.ta.managementproject.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageServiceTest {

    @Mock private StageDb stageDb;
    @Mock private UserDb userDb;
    @Mock private ProjectDb projectDb;
    @Mock private TaskDb taskDb;
    @Mock private SubTaskDb subTaskDb;
    @Mock private UserService userService;
    @Mock private UtilService utilService;
    @Mock private AuthService authService;
    @Mock private StageDbWithDsl stageDbWithDsl;

    @InjectMocks
    private StageServiceImpl stageService;

    private MockedStatic<JwtUtils> jwtUtilsMock;

    private User mockUser;
    private ProjectManager mockPm;
    private Project mockProject;
    private Stage mockStage;
    private CreateUpdateStageRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        jwtUtilsMock = mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getCurrentUsername).thenReturn("manager1");

        mockPm = new ProjectManager();
        mockPm.setUsername("manager1");
        mockPm.setFullName("Manager One");

        mockUser = new User();
        mockUser.setUsername("manager1");

        mockProject = Project.builder()
                .projectId("project-1")
                .projectName("Project Alpha")
                .description("Desc")
                .projectManager(mockPm)
                .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                .endDate(Instant.parse("2024-12-31T00:00:00Z"))
                .createdAt(Instant.now())
                .stageList(new ArrayList<>())
                .memberInProjectList(List.of())
                .isCancelled(false)
                .build();

        mockStage = Stage.builder()
                .stageId("stage-1")
                .stageName("Stage One")
                .description("Stage desc")
                .order(1)
                .project(mockProject)
                .createdAt(Instant.now())
                .isDeleted(false)
                .finishedTask(0L)
                .todoTask(0L)
                .inProgressTask(0L)
                .totalTask(0L)
                .progress(0.00)
                .build();

        mockRequest = new CreateUpdateStageRequestDTO();
        mockRequest.setStageName("Stage One");
        mockRequest.setDescription("Stage desc");
    }

    @AfterEach
    void tearDown() {
        jwtUtilsMock.close();
    }

    // Helper: stub utilService.buildResponse
    private void stubBuildResponse(HttpStatus status) {
        when(utilService.buildResponse(eq(status), anyString(), any()))
                .thenReturn(ResponseEntity.status(status).build());
    }

    // ===================== getAllStage =====================

    @Test
    void getAllStage_ShouldReturnOk_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StageResponseDTO> page = new PageImpl<>(List.of());

        when(stageDbWithDsl.findAll(any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.getAllStage(
                "project-1", pageable, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllStage_ShouldCallStageDbWithDsl_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StageResponseDTO> page = new PageImpl<>(List.of());

        when(stageDbWithDsl.findAll(any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        stageService.getAllStage("project-1", pageable, null, null, null);

        verify(stageDbWithDsl, times(1)).findAll(any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void getAllStage_ShouldPassUsernameFromJwt_WhenCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StageResponseDTO> page = new PageImpl<>(List.of());

        when(stageDbWithDsl.findAll(any(), any(), any(), any(), eq("manager1"), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        stageService.getAllStage("project-1", pageable, null, null, null);

        verify(stageDbWithDsl).findAll(any(), any(), any(), any(), eq("manager1"), any());
    }

    // ===================== getStage =====================

    @Test
    void getStage_ShouldReturnOk_WhenValidAccess() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.getStage("stage-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getStage_ShouldThrowNotFoundException_WhenStageNotFound() {
        when(authService.validateStage("stage-x"))
                .thenThrow(new NotFoundException("STAGE_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                stageService.getStage("stage-x"));
    }

    @Test
    void getStage_ShouldCallValidateManagerAndMemberAccess() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        stageService.getStage("stage-1");

        verify(authService, times(1)).validateManagerAndMemberAccess(eq(mockProject), eq("manager1"));
    }

    // ===================== addNewStage =====================

    @Test
    void addNewStage_ShouldReturnOk_WhenValidRequest() {
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.getTotalStage("project-1")).thenReturn(2);
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.addNewStage("project-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void addNewStage_ShouldCallStageDbSave_WhenValidRequest() {
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.getTotalStage("project-1")).thenReturn(2);
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.addNewStage("project-1", mockRequest);

        verify(stageDb, times(1)).save(any(Stage.class));
    }

    @Test
    void addNewStage_ShouldSetOrderToTotalPlusOne_WhenSaving() {
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.getTotalStage("project-1")).thenReturn(3);
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.addNewStage("project-1", mockRequest);

        verify(stageDb).save(argThat(stage -> stage.getOrder() == 4));
    }

    @Test
    void addNewStage_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                stageService.addNewStage("project-x", mockRequest));
    }

    @Test
    void addNewStage_ShouldCallValidateProjectCancellation_WhenValidRequest() {
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.getTotalStage("project-1")).thenReturn(0);
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.addNewStage("project-1", mockRequest);

        verify(authService, times(1)).validateProjectCancellation(eq(mockProject));
    }

    // ===================== editStage =====================

    @Test
    void editStage_ShouldReturnOk_WhenValidRequest() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.editStage("stage-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void editStage_ShouldKeepExistingStageName_WhenRequestStageNameIsNull() {
        mockRequest.setStageName(null);

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.editStage("stage-1", mockRequest);

        verify(stageDb).save(argThat(stage ->
                "Stage One".equals(stage.getStageName())));
    }

    @Test
    void editStage_ShouldKeepExistingDescription_WhenRequestDescriptionIsNull() {
        mockRequest.setDescription(null);

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.editStage("stage-1", mockRequest);

        verify(stageDb).save(argThat(stage ->
                "Stage desc".equals(stage.getDescription())));
    }

    @Test
    void editStage_ShouldThrowNotFoundException_WhenStageNotFound() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateStage("stage-x"))
                .thenThrow(new NotFoundException("STAGE_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                stageService.editStage("stage-x", mockRequest));
    }

    @Test
    void editStage_ShouldCallValidateManagerAccess() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.save(any())).thenReturn(mockStage);
        stubBuildResponse(HttpStatus.OK);

        stageService.editStage("stage-1", mockRequest);

        verify(authService, times(1)).validateManagerAccess(eq(mockProject), eq("manager1"));
    }

    // ===================== reorderStage =====================

    @Test
    void reorderStage_ShouldReturnOk_WhenMovingStageUp() {
        Stage stage = mockStage.toBuilder().order(3).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(1);

        List<Stage> stageList = List.of(
                Stage.builder().order(1).build(),
                Stage.builder().order(2).build(),
                stage
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.reorderStage("project-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderStage_ShouldReturnOk_WhenMovingStageDown() {
        Stage stage = mockStage.toBuilder().order(1).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(3);

        List<Stage> stageList = List.of(
                stage,
                Stage.builder().order(2).build(),
                Stage.builder().order(3).build()
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.reorderStage("project-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderStage_ShouldThrowConflictException_WhenOrderUnchanged() {
        Stage stage = mockStage.toBuilder().order(2).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(2);

        List<Stage> stageList = List.of(
                Stage.builder().order(1).build(),
                stage,
                Stage.builder().order(3).build()
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);

        assertThrows(ConflictException.class, () ->
                stageService.reorderStage("project-1", reorderRequest));
    }

    @Test
    void reorderStage_ShouldClampOrderToMin1_WhenRequestOrderBelowBound() {
        Stage stage = mockStage.toBuilder().order(2).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(-5);

        List<Stage> stageList = List.of(
                Stage.builder().order(1).build(),
                stage,
                Stage.builder().order(3).build()
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        stageService.reorderStage("project-1", reorderRequest);

        verify(stageDb).save(argThat(s -> s.getOrder() == 1));
    }

    @Test
    void reorderStage_ShouldClampOrderToMax_WhenRequestOrderAboveBound() {
        Stage stage = mockStage.toBuilder().order(1).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(100);

        List<Stage> stageList = List.of(
                stage,
                Stage.builder().order(2).build(),
                Stage.builder().order(3).build()
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        stageService.reorderStage("project-1", reorderRequest);

        verify(stageDb).save(argThat(s -> s.getOrder() == 3));
    }

    @Test
    void reorderStage_ShouldCallUpdateStageOrderAbove_WhenMovingStageUp() {
        Stage stage = mockStage.toBuilder().order(3).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(1);

        List<Stage> stageList = List.of(
                Stage.builder().order(1).build(),
                Stage.builder().order(2).build(),
                stage
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        stageService.reorderStage("project-1", reorderRequest);

        verify(stageDb, times(1)).updateStageOrderAbove(eq("project-1"), eq(1), eq(2));
    }

    @Test
    void reorderStage_ShouldCallUpdateStageOrderBelow_WhenMovingStageDown() {
        Stage stage = mockStage.toBuilder().order(1).build();
        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("stage-1");
        reorderRequest.setOrder(3);

        List<Stage> stageList = List.of(
                stage,
                Stage.builder().order(2).build(),
                Stage.builder().order(3).build()
        );
        Project project = mockProject.toBuilder().stageList(new ArrayList<>(stageList)).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(project);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
        when(stageDb.save(any())).thenReturn(stage);
        stubBuildResponse(HttpStatus.OK);

        stageService.reorderStage("project-1", reorderRequest);

        verify(stageDb, times(1)).updateStageOrderBelow(eq("project-1"), eq(4), eq(1));
    }

    // ===================== deleteStageById =====================

    @Test
    void deleteStageById_ShouldReturnOk_WhenValidRequest() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        doNothing().when(stageDb).delete(any());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = stageService.deleteStageById("project-1", "stage-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void deleteStageById_ShouldCallSoftDeleteMethods() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        doNothing().when(stageDb).delete(any());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        stageService.deleteStageById("project-1", "stage-1");

        verify(stageDb, times(1)).softDeleteSubTaskByStageId("stage-1");
        verify(stageDb, times(1)).softDeleteTaskByStageId("stage-1");
        verify(stageDb, times(1)).delete(mockStage);
    }

    @Test
    void deleteStageById_ShouldCallUpdateStageOrderAfterDelete() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        doNothing().when(stageDb).delete(any());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        stageService.deleteStageById("project-1", "stage-1");

        verify(stageDb, times(1)).updateStageOrderAfterDelete("project-1", 1);
    }

    @Test
    void deleteStageById_ShouldCallUpdateProjectSummary() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        doNothing().when(stageDb).delete(any());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        stageService.deleteStageById("project-1", "stage-1");

        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void deleteStageById_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                stageService.deleteStageById("project-x", "stage-1"));
    }

    @Test
    void deleteStageById_ShouldCallValidateManagerAccess() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        doNothing().when(stageDb).delete(any());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        stageService.deleteStageById("project-1", "stage-1");

        verify(authService, times(1)).validateManagerAccess(eq(mockProject), eq("manager1"));
    }
}