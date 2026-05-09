package com.ta.managementproject;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.task.SubTaskServiceImpl;
import com.ta.managementproject.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ta.managementproject.security.util.JwtUtils;

@ExtendWith(MockitoExtension.class)
class SubTaskServiceTest {

    @Mock private SubTaskDb subTaskDb;
    @Mock private TaskDb taskDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private HttpServletRequest request;
    @Mock private UserService userService;
    @Mock private AuthService authService;
    @Mock private UtilService utilService;
    @Mock private SubTaskDbWithDsl subTaskDbWithDsl;

    @InjectMocks
    private SubTaskServiceImpl subTaskService;

    private MockedStatic<JwtUtils> jwtUtilsMock;

    private ProjectManager mockPm;
    private Project mockProject;
    private Stage mockStage;
    private Task mockTask;
    private SubTask mockSubTask;
    private ProjectMember mockProjectMember;
    private CreateUpdateSubTaskRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        jwtUtilsMock = mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getCurrentEmail).thenReturn("manager1@example.com");

        mockPm = new ProjectManager();
        mockPm.setEmail("manager1@example.com");
        mockPm.setFullName("Manager One");

        mockProject = Project.builder()
                .projectId("project-1")
                .projectName("Project Alpha")
                .description("Desc")
                .projectManager(mockPm)
                .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                .endDate(Instant.parse("2024-12-31T00:00:00Z"))
                .createdAt(Instant.now())
                .memberInProjectList(List.of())
                .isCancelled(false)
                .build();

        mockStage = Stage.builder()
                .stageId("stage-1")
                .stageName("Stage One")
                .project(mockProject)
                .order(1)
                .build();

        mockProjectMember = new ProjectMember();
        mockProjectMember.setEmail("member1@example.com");

        mockTask = Task.builder()
                .taskId("task-1")
                .taskName("Task One")
                .description("Task desc")
                .priority(1)
                .dueDate(Instant.parse("2024-06-30T00:00:00Z"))
                .status("TODO")
                .order(1)
                .isDeleted(false)
                .stage(mockStage)
                .projectMember(mockProjectMember)
                .subTaskList(new ArrayList<>())
                .build();

        mockSubTask = SubTask.builder()
                .subTaskId("subtask-1")
                .subTaskName("SubTask One")
                .description("SubTask desc")
                .dueDate(Instant.parse("2024-06-30T00:00:00Z"))
                .status("TODO")
                .label("label-1")
                .order(1)
                .isDeleted(false)
                .task(mockTask)
                .projectMember(mockProjectMember)
                .build();

        mockRequest = new CreateUpdateSubTaskRequestDTO();
        mockRequest.setSubTaskName("SubTask One");
        mockRequest.setDescription("SubTask desc");
        mockRequest.setDueDate(LocalDate.of(2024, 6, 30));
        mockRequest.setLabel("label-1");
        mockRequest.setProjectMember(mockProjectMember);
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

    // ===================== getAllSubTask =====================

    @Test
    void getAllSubTask_ShouldReturnOk_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());

        when(subTaskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.getAllSubTask(
                "task-1", null, null, null, null, null, pageable);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllSubTask_ShouldCallSubTaskDbWithDsl_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());

        when(subTaskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.getAllSubTask("task-1", null, null, null, null, null, pageable);

        verify(subTaskDbWithDsl, times(1))
                .findAll(any(), any(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void getAllSubTask_ShouldPassUsernameFromJwt_WhenCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());

        when(subTaskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), eq("manager1@example.com"), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.getAllSubTask("task-1", null, null, null, null, null, pageable);

        verify(subTaskDbWithDsl).findAll(
                any(), any(), any(), any(), any(), any(), eq("manager1@example.com"), any());
    }

    // ===================== addNewSubTask =====================

    @Test
    void addNewSubTask_ShouldReturnCreated_WhenValidRequest() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(2);
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = subTaskService.addNewSubTask("task-1", mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void addNewSubTask_ShouldCallSubTaskDbSave_WhenValidRequest() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(0);
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.addNewSubTask("task-1", mockRequest);

        verify(subTaskDb, times(1)).save(any(SubTask.class));
    }

    @Test
    void addNewSubTask_ShouldSetOrderToTotalPlusOne_WhenSaving() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(3);
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.addNewSubTask("task-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask -> subTask.getOrder() == 4));
    }

    @Test
    void addNewSubTask_ShouldSetStatusToTodo_WhenCreating() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(0);
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.addNewSubTask("task-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask -> "TODO".equals(subTask.getStatus())));
    }

    @Test
    void addNewSubTask_ShouldCallUpdateTaskStatusAndAllSummaries_WhenSubTaskCreated() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(0);
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.addNewSubTask("task-1", mockRequest);

        verify(utilService, times(1)).updateTaskStatusAndSummary("task-1");
        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void addNewSubTask_ShouldThrowNotFoundException_WhenTaskNotFound() {
        when(authService.validateTask("task-x"))
                .thenThrow(new NotFoundException("TASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                subTaskService.addNewSubTask("task-x", mockRequest));
    }

    // ===================== updateSubTask =====================

    @Test
    void updateSubTask_ShouldReturnOk_WhenValidRequest() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.updateSubTask("subtask-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateSubTask_ShouldCallSubTaskDbSave_WhenValidRequest() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTask("subtask-1", mockRequest);

        verify(subTaskDb, times(1)).save(any(SubTask.class));
    }

    @Test
    void updateSubTask_ShouldKeepExistingSubTaskName_WhenRequestSubTaskNameIsNull() {
        mockRequest.setSubTaskName(null);

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTask("subtask-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask ->
                "SubTask One".equals(subTask.getSubTaskName())));
    }

    @Test
    void updateSubTask_ShouldKeepExistingDescription_WhenRequestDescriptionIsNull() {
        mockRequest.setDescription(null);

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTask("subtask-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask ->
                "SubTask desc".equals(subTask.getDescription())));
    }

    @Test
    void updateSubTask_ShouldKeepExistingLabel_WhenRequestLabelIsNull() {
        mockRequest.setLabel(null);

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTask("subtask-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask ->
                "label-1".equals(subTask.getLabel())));
    }

    @Test
    void updateSubTask_ShouldThrowNotFoundException_WhenSubTaskNotFound() {
        when(authService.validateSubTask("subtask-x"))
                .thenThrow(new NotFoundException("SUBTASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                subTaskService.updateSubTask("subtask-x", mockRequest));
    }

    @Test
    void updateSubTask_ShouldCallValidateManagerAccess() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTask("subtask-1", mockRequest);

        verify(authService, times(1)).validateManagerAccess(eq(mockProject), eq("manager1@example.com"));
    }

    // ===================== getDetailSubTask =====================

    @Test
    void getDetailSubTask_ShouldReturnOk_WhenValidAccess() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getDetailSubTask_ShouldThrowNotFoundException_WhenSubTaskNotFound() {
        when(authService.validateSubTask("subtask-x"))
                .thenThrow(new NotFoundException("SUBTASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                subTaskService.getDetailSubTask("subtask-x"));
    }

    @Test
    void getDetailSubTask_ShouldCallValidateManagerAndMemberAccess() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        subTaskService.getDetailSubTask("subtask-1");

        verify(authService, times(1))
                .validateManagerAndMemberAccess(eq(mockProject), eq("manager1@example.com"));
    }

    // ===================== deleteSubTaskById =====================

    @Test
    void deleteSubTaskById_ShouldReturnCreated_WhenValidRequest() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(subTaskDb).delete(any());
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = subTaskService.deleteSubTaskById("task-1", "subtask-1");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void deleteSubTaskById_ShouldCallSubTaskDbDelete_WhenDeleting() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(subTaskDb).delete(any());
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.deleteSubTaskById("task-1", "subtask-1");

        verify(subTaskDb, times(1)).delete(mockSubTask);
    }

    @Test
    void deleteSubTaskById_ShouldCallUpdateSubTaskOrderAfterDelete() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(subTaskDb).delete(any());
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.deleteSubTaskById("task-1", "subtask-1");

        verify(subTaskDb, times(1)).updateSubTaskOrderAfterDelete("task-1", 1);
    }

    @Test
    void deleteSubTaskById_ShouldCallUpdateTaskStatusAndAllSummaries() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(subTaskDb).delete(any());
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.deleteSubTaskById("task-1", "subtask-1");

        verify(utilService, times(1)).updateTaskStatusAndSummary("task-1");
        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void deleteSubTaskById_ShouldThrowNotFoundException_WhenSubTaskNotFound() {
        when(authService.validateSubTask("subtask-x"))
                .thenThrow(new NotFoundException("SUBTASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                subTaskService.deleteSubTaskById("task-1", "subtask-x"));
    }

    @Test
    void deleteSubTaskById_ShouldCallValidateManagerAccess() {
        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(subTaskDb).delete(any());
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        subTaskService.deleteSubTaskById("task-1", "subtask-1");

        verify(authService, times(1)).validateManagerAccess(eq(mockProject), eq("manager1@example.com"));
    }

    // ===================== reorderSubTask =====================

    @Test
    void reorderSubTask_ShouldReturnOk_WhenMovingSubTaskUp() {
        SubTask subTask = mockSubTask.toBuilder().order(3).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        SubTask.builder().order(1).build(),
                        SubTask.builder().order(2).build(),
                        subTask
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(1);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.reorderSubTask("task-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderSubTask_ShouldReturnOk_WhenMovingSubTaskDown() {
        SubTask subTask = mockSubTask.toBuilder().order(1).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        subTask,
                        SubTask.builder().order(2).build(),
                        SubTask.builder().order(3).build()
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(3);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.reorderSubTask("task-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderSubTask_ShouldThrowConflictException_WhenOrderUnchanged() {
        SubTask subTask = mockSubTask.toBuilder().order(2).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        SubTask.builder().order(1).build(),
                        subTask,
                        SubTask.builder().order(3).build()
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(2);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());

        assertThrows(ConflictException.class, () ->
                subTaskService.reorderSubTask("task-1", reorderRequest));
    }

    @Test
    void reorderSubTask_ShouldClampOrderToMin1_WhenRequestOrderBelowBound() {
        SubTask subTask = mockSubTask.toBuilder().order(2).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        SubTask.builder().order(1).build(),
                        subTask,
                        SubTask.builder().order(3).build()
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(-10);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.reorderSubTask("task-1", reorderRequest);

        verify(subTaskDb).save(argThat(st -> st.getOrder() == 1));
    }

    @Test
    void reorderSubTask_ShouldClampOrderToMax_WhenRequestOrderAboveBound() {
        SubTask subTask = mockSubTask.toBuilder().order(1).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        subTask,
                        SubTask.builder().order(2).build(),
                        SubTask.builder().order(3).build()
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(999);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.reorderSubTask("task-1", reorderRequest);

        verify(subTaskDb).save(argThat(st -> st.getOrder() == 3));
    }

    @Test
    void reorderSubTask_ShouldCallUpdateSubTaskOrderAbove_WhenMovingSubTaskUp() {
        SubTask subTask = mockSubTask.toBuilder().order(3).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        SubTask.builder().order(1).build(),
                        SubTask.builder().order(2).build(),
                        subTask
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(1);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.reorderSubTask("task-1", reorderRequest);

        verify(subTaskDb, times(1)).updateSubTaskOrderAbove(eq("task-1"), eq(1), eq(2));
    }

    @Test
    void reorderSubTask_ShouldCallUpdateSubTaskOrderBelow_WhenMovingSubTaskDown() {
        SubTask subTask = mockSubTask.toBuilder().order(1).build();
        Task task = mockTask.toBuilder()
                .subTaskList(new ArrayList<>(List.of(
                        subTask,
                        SubTask.builder().order(2).build(),
                        SubTask.builder().order(3).build()
                )))
                .build();
        subTask = subTask.toBuilder().task(task).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("subtask-1");
        reorderRequest.setOrder(3);

        when(authService.validateTask("task-1")).thenReturn(task);
        when(authService.validateSubTask("subtask-1")).thenReturn(subTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(subTask);
        stubBuildResponse(HttpStatus.OK);

        subTaskService.reorderSubTask("task-1", reorderRequest);

        verify(subTaskDb, times(1)).updateSubTaskOrderBelow(eq("task-1"), eq(4), eq(1));
    }

    // ===================== updateSubTaskStatus =====================

    @Test
    void updateSubTaskStatus_ShouldReturnOk_WhenValidRequest() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = subTaskService.updateSubTaskStatus("subtask-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateSubTaskStatus_ShouldUpdateStatus_WhenValidRequest() {
        mockRequest.setStatus("DONE");

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTaskStatus("subtask-1", mockRequest);

        verify(subTaskDb).save(argThat(subTask -> "DONE".equals(subTask.getStatus())));
    }

    @Test
    void updateSubTaskStatus_ShouldCallUpdateTaskStatusAndAllSummaries() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTaskStatus("subtask-1", mockRequest);

        verify(utilService, times(1)).updateTaskStatusAndSummary("task-1");
        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void updateSubTaskStatus_ShouldThrowNotFoundException_WhenSubTaskNotFound() {
        mockRequest.setStatus("DONE");

        when(authService.validateSubTask("subtask-x"))
                .thenThrow(new NotFoundException("SUBTASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                subTaskService.updateSubTaskStatus("subtask-x", mockRequest));
    }

    @Test
    void updateSubTaskStatus_ShouldCallValidateManagerAndMemberAccess() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTaskStatus("subtask-1", mockRequest);

        verify(authService, times(1))
                .validateManagerAndMemberAccess(eq(mockProject), eq("manager1@example.com"));
    }

    @Test
    void updateSubTaskStatus_ShouldCallValidateProjectCancellation() {
        mockRequest.setStatus("DONE");

        when(authService.validateSubTask("subtask-1")).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(subTaskDb.save(any())).thenReturn(mockSubTask);
        doNothing().when(utilService).updateTaskStatusAndSummary(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        subTaskService.updateSubTaskStatus("subtask-1", mockRequest);

        verify(authService, times(1)).validateProjectCancellation(eq(mockProject));
    }
}