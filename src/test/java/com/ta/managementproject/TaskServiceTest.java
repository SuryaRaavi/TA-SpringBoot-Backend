package com.ta.managementproject;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.task.TaskServiceImpl;
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
class TaskServiceTest {

    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private TaskDb taskDb;
    @Mock private ProjectDb projectDb;
    @Mock private StageDb stageDb;
    @Mock private HttpServletRequest request;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private UserService userService;
    @Mock private SubTaskDb subTaskDb;
    @Mock private AuthService authService;
    @Mock private UtilService utilService;
    @Mock private TaskDbWithDsl taskDbWithDsl;

    @InjectMocks
    private TaskServiceImpl taskService;

    private MockedStatic<JwtUtils> jwtUtilsMock;

    private ProjectManager mockPm;
    private Project mockProject;
    private Stage mockStage;
    private Task mockTask;
    private ProjectMember mockProjectMember;
    private CreateUpdateTaskRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        jwtUtilsMock = mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getCurrentUsername).thenReturn("manager1");

        mockPm = new ProjectManager();
        mockPm.setUsername("manager1");
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
                .taskList(new ArrayList<>())
                .build();

        mockProjectMember = new ProjectMember();
        mockProjectMember.setUsername("member1");

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
                .finishedTask(0L)
                .todoTask(0L)
                .inProgressTask(0L)
                .totalTask(0L)
                .progress(0.00)
                .build();

        mockRequest = new CreateUpdateTaskRequestDTO();
        mockRequest.setTaskName("Task One");
        mockRequest.setDescription("Task desc");
        mockRequest.setPriority(1);
        mockRequest.setDueDate(LocalDate.of(2024, 6, 30));
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

    // ===================== getAllTask =====================

    @Test
    void getAllTask_ShouldReturnOk_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskResponseDTO> page = new PageImpl<>(List.of());

        when(taskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.getAllTask(
                pageable, "stage-1", null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllTask_ShouldCallTaskDbWithDsl_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskResponseDTO> page = new PageImpl<>(List.of());

        when(taskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any(), anyString(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        taskService.getAllTask(pageable, "stage-1", null, null, null, null, null, null);

        verify(taskDbWithDsl, times(1))
                .findAll(any(), any(), any(), any(), any(), any(), any(), anyString(), any());
    }

    @Test
    void getAllTask_ShouldPassUsernameFromJwt_WhenCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskResponseDTO> page = new PageImpl<>(List.of());

        when(taskDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any(), eq("manager1"), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        taskService.getAllTask(pageable, "stage-1", null, null, null, null, null, null);

        verify(taskDbWithDsl).findAll(
                any(), any(), any(), any(), any(), any(), any(), eq("manager1"), any());
    }

    // ===================== addNewTask =====================

    @Test
    void addNewTask_ShouldReturnCreated_WhenValidRequest() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.getTotalTask("stage-1")).thenReturn(2);
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = taskService.addNewTask("stage-1", mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void addNewTask_ShouldCallTaskDbSave_WhenValidRequest() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.getTotalTask("stage-1")).thenReturn(0);
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        taskService.addNewTask("stage-1", mockRequest);

        verify(taskDb, times(1)).save(any(Task.class));
    }

    @Test
    void addNewTask_ShouldSetOrderToTotalPlusOne_WhenSaving() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.getTotalTask("stage-1")).thenReturn(4);
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        taskService.addNewTask("stage-1", mockRequest);

        verify(taskDb).save(argThat(task -> task.getOrder() == 5));
    }

    @Test
    void addNewTask_ShouldSetStatusToTodo_WhenCreating() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.getTotalTask("stage-1")).thenReturn(0);
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        taskService.addNewTask("stage-1", mockRequest);

        verify(taskDb).save(argThat(task -> "TODO".equals(task.getStatus())));
    }

    @Test
    void addNewTask_ShouldCallUpdateStageSummary_WhenTaskCreated() {
        when(authService.validateStage("stage-1")).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.getTotalTask("stage-1")).thenReturn(0);
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.CREATED);

        taskService.addNewTask("stage-1", mockRequest);

        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void addNewTask_ShouldThrowNotFoundException_WhenStageNotFound() {
        when(authService.validateStage("stage-x"))
                .thenThrow(new NotFoundException("STAGE_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                taskService.addNewTask("stage-x", mockRequest));
    }

    // ===================== updateTask =====================

    @Test
    void updateTask_ShouldReturnCreated_WhenValidRequest() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = taskService.updateTask("task-1", mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void updateTask_ShouldCallTaskDbSave_WhenValidRequest() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        stubBuildResponse(HttpStatus.CREATED);

        taskService.updateTask("task-1", mockRequest);

        verify(taskDb, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_ShouldUpdateTaskFields_WhenValidRequest() {
        mockRequest.setTaskName("Updated Task");
        mockRequest.setDescription("Updated desc");
        mockRequest.setPriority(2);

        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        stubBuildResponse(HttpStatus.CREATED);

        taskService.updateTask("task-1", mockRequest);

        verify(taskDb).save(argThat(task ->
                "Updated Task".equals(task.getTaskName()) &&
                        "Updated desc".equals(task.getDescription()) &&
                        task.getPriority() == 2
        ));
    }

    @Test
    void updateTask_ShouldThrowNotFoundException_WhenTaskNotFound() {
        when(authService.validateTask("task-x"))
                .thenThrow(new NotFoundException("TASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                taskService.updateTask("task-x", mockRequest));
    }

    @Test
    void updateTask_ShouldCallValidateManagerAccess() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        stubBuildResponse(HttpStatus.CREATED);

        taskService.updateTask("task-1", mockRequest);

        verify(authService, times(1)).validateManagerAccess(eq(mockProject), eq("manager1"));
    }

    // ===================== getDetailTask =====================

    @Test
    void getDetailTask_ShouldReturnOk_WhenValidAccess() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.getDetailTask("task-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getDetailTask_ShouldThrowNotFoundException_WhenTaskNotFound() {
        when(authService.validateTask("task-x"))
                .thenThrow(new NotFoundException("TASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                taskService.getDetailTask("task-x"));
    }

    @Test
    void getDetailTask_ShouldCallValidateManagerAndMemberAccess() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.getDetailTask("task-1");

        verify(authService, times(1))
                .validateManagerAndMemberAccess(eq(mockProject), eq("manager1"));
    }

    // ===================== deleteTaskById =====================

    @Test
    void deleteTaskById_ShouldReturnOk_WhenValidRequest() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(taskDb).deleteById(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.deleteTaskById("stage-1", "task-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void deleteTaskById_ShouldCallSoftDeleteSubTask_WhenDeleting() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(taskDb).deleteById(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.deleteTaskById("stage-1", "task-1");

        verify(taskDb, times(1)).softDeleteSubTaskByTaskId("task-1");
        verify(taskDb, times(1)).deleteById("task-1");
    }

    @Test
    void deleteTaskById_ShouldCallUpdateTaskOrderAfterDelete() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(taskDb).deleteById(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.deleteTaskById("stage-1", "task-1");

        verify(taskDb, times(1)).updateTaskOrderAfterDelete("stage-1", 1);
    }

    @Test
    void deleteTaskById_ShouldCallUpdateStageSummaryAndProjectSummary() {
        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        doNothing().when(taskDb).deleteById(anyString());
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.deleteTaskById("stage-1", "task-1");

        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void deleteTaskById_ShouldThrowNotFoundException_WhenTaskNotFound() {
        when(authService.validateTask("task-x"))
                .thenThrow(new NotFoundException("TASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                taskService.deleteTaskById("stage-1", "task-x"));
    }

    // ===================== reorderTask =====================

    @Test
    void reorderTask_ShouldReturnOk_WhenMovingTaskUp() {
        Task task = mockTask.toBuilder().order(3).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        Task.builder().order(1).build(),
                        Task.builder().order(2).build(),
                        task
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(1);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.reorderTask("stage-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderTask_ShouldReturnOk_WhenMovingTaskDown() {
        Task task = mockTask.toBuilder().order(1).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        task,
                        Task.builder().order(2).build(),
                        Task.builder().order(3).build()
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(3);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.reorderTask("stage-1", reorderRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void reorderTask_ShouldThrowConflictException_WhenOrderUnchanged() {
        Task task = mockTask.toBuilder().order(2).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        Task.builder().order(1).build(),
                        task,
                        Task.builder().order(3).build()
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(2);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());

        Task finalTask = task;
        assertThrows(ConflictException.class, () ->
                taskService.reorderTask("stage-1", reorderRequest));
    }

    @Test
    void reorderTask_ShouldClampOrderToMin1_WhenRequestOrderBelowBound() {
        Task task = mockTask.toBuilder().order(2).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        Task.builder().order(1).build(),
                        task,
                        Task.builder().order(3).build()
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(-10);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        taskService.reorderTask("stage-1", reorderRequest);

        verify(taskDb).save(argThat(t -> t.getOrder() == 1));
    }

    @Test
    void reorderTask_ShouldClampOrderToMax_WhenRequestOrderAboveBound() {
        Task task = mockTask.toBuilder().order(1).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        task,
                        Task.builder().order(2).build(),
                        Task.builder().order(3).build()
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(999);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        taskService.reorderTask("stage-1", reorderRequest);

        verify(taskDb).save(argThat(t -> t.getOrder() == 3));
    }

    @Test
    void reorderTask_ShouldCallUpdateTaskOrderAbove_WhenMovingTaskUp() {
        Task task = mockTask.toBuilder().order(3).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        Task.builder().order(1).build(),
                        Task.builder().order(2).build(),
                        task
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(1);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        taskService.reorderTask("stage-1", reorderRequest);

        verify(taskDb, times(1)).updateTaskOrderAbove(eq("stage-1"), eq(1), eq(2));
    }

    @Test
    void reorderTask_ShouldCallUpdateTaskOrderBelow_WhenMovingTaskDown() {
        Task task = mockTask.toBuilder().order(1).build();
        Stage stage = mockStage.toBuilder()
                .taskList(new ArrayList<>(List.of(
                        task,
                        Task.builder().order(2).build(),
                        Task.builder().order(3).build()
                )))
                .build();
        task = task.toBuilder().stage(stage).build();

        ReorderRequestDTO reorderRequest = new ReorderRequestDTO();
        reorderRequest.setId("task-1");
        reorderRequest.setOrder(3);

        when(authService.validateStage("stage-1")).thenReturn(stage);
        when(authService.validateTask("task-1")).thenReturn(task);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(task);
        stubBuildResponse(HttpStatus.OK);

        taskService.reorderTask("stage-1", reorderRequest);

        verify(taskDb, times(1)).updateTaskOrderBelow(eq("stage-1"), eq(4), eq(2));
    }

    // ===================== updateTaskStatus =====================

    @Test
    void updateTaskStatus_ShouldReturnOk_WhenValidRequest() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = taskService.updateTaskStatus("task-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateTaskStatus_ShouldUpdateStatus_WhenSubTaskListEmpty() {
        mockRequest.setStatus("DONE");

        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.updateTaskStatus("task-1", mockRequest);

        verify(taskDb).save(argThat(task -> "DONE".equals(task.getStatus())));
    }

    @Test
    void updateTaskStatus_ShouldThrowConflictException_WhenSubTaskListNotEmpty() {
        mockRequest.setStatus("DONE");

        SubTask subTask = new SubTask();
        Task taskWithSubTasks = mockTask.toBuilder()
                .subTaskList(List.of(subTask))
                .build();

        when(authService.validateTask("task-1")).thenReturn(taskWithSubTasks);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());

        assertThrows(ConflictException.class, () ->
                taskService.updateTaskStatus("task-1", mockRequest));
    }

    @Test
    void updateTaskStatus_ShouldCallUpdateStageSummaryAndProjectSummary() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.updateTaskStatus("task-1", mockRequest);

        verify(utilService, times(1)).updateStageSummary("stage-1");
        verify(utilService, times(1)).updateProjectSummary("project-1");
    }

    @Test
    void updateTaskStatus_ShouldThrowNotFoundException_WhenTaskNotFound() {
        mockRequest.setStatus("DONE");

        when(authService.validateTask("task-x"))
                .thenThrow(new NotFoundException("TASK_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                taskService.updateTaskStatus("task-x", mockRequest));
    }

    @Test
    void updateTaskStatus_ShouldCallValidateManagerAndMemberAccess() {
        mockRequest.setStatus("IN_PROGRESS");

        when(authService.validateTask("task-1")).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(taskDb.save(any())).thenReturn(mockTask);
        doNothing().when(utilService).updateStageSummary(anyString());
        doNothing().when(utilService).updateProjectSummary(anyString());
        stubBuildResponse(HttpStatus.OK);

        taskService.updateTaskStatus("task-1", mockRequest);

        verify(authService, times(1))
                .validateManagerAndMemberAccess(eq(mockProject), eq("manager1"));
    }
}