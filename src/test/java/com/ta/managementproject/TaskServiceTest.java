package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.task.TaskServiceImpl;
import com.ta.managementproject.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private TaskDb taskDb;
    @Mock private ProjectDb projectDb;
    @Mock private StageDb stageDb;
    @Mock private JwtUtils jwtUtils;
    @Mock private HttpServletRequest request;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private UserService userService;
    @Mock private SubTaskDb subTaskDb;
    @Mock private AuthService authService;
    @Mock private UtilService utilService;
    @Mock private TaskDbWithDsl taskDbWithDsl;

    @InjectMocks
    private TaskServiceImpl taskService;

    private static final String USERNAME  = "user_test";
    private static final String STAGE_ID  = "stage-001";
    private static final String TASK_ID   = "task-001";

    private Project      mockProject;
    private Stage        mockStage;
    private Task         mockTask;
    private ProjectMember mockMember;

    @BeforeEach
    void setUp() {
        mockProject = Project.builder()
                .projectId("project-001")
                .projectName("Test Project")
                .build();

        mockStage = Stage.builder()
                .stageId(STAGE_ID)
                .stageName("Stage Alpha")
                .project(mockProject)
                .build();

        mockMember = ProjectMember.builder()
                .username(USERNAME)
                .fullName("Test User")
                .build();

        mockTask = Task.builder()
                .taskId(TASK_ID)
                .taskName("Task Alpha")
                .description("Desc")
                .priority(1)
                .dueDate(java.time.Instant.now())
                .status("TODO")
                .projectMember(mockMember)
                .stage(mockStage)
                .order(1)
                .subTaskList(List.of())
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getAllTask
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getAllTask_withValidAccess_shouldReturnPageOfTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<TaskResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<TaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(taskDbWithDsl.findAll(eq(STAGE_ID), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.getAllTask(pageable, STAGE_ID, null, null, null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskDbWithDsl).findAll(eq(STAGE_ID), any(), any(), any(), any(), any(), any(), eq(pageable));
    }

    @Test
    void getAllTask_withAllFilters_shouldPassFiltersToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        Page<TaskResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<TaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(taskDbWithDsl.findAll(eq(STAGE_ID), eq(today), eq(today), eq(today), eq(1), eq(1), eq("Alpha"), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.getAllTask(pageable, STAGE_ID, today, today, today, 1, 1, "Alpha");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskDbWithDsl).findAll(eq(STAGE_ID), eq(today), eq(today), eq(today), eq(1), eq(1), eq("Alpha"), eq(pageable));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * addNewTask
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void addNewTask_withValidRequest_shouldSaveTaskAndReturnCreated() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setTaskName("New Task");
        requestDTO.setDescription("Desc");
        requestDTO.setPriority(2);
        requestDTO.setDueDate(LocalDate.now().plusDays(5));
        requestDTO.setProjectMember(mockMember);

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(taskDb.getTotalTask(STAGE_ID)).thenReturn(3);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Task created successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.addNewTask(STAGE_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(taskDb).save(any(Task.class));
    }

    @Test
    void addNewTask_shouldSetOrderAsCurrentTotalPlusOne() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setTaskName("Task X");
        requestDTO.setDescription("Desc");
        requestDTO.setPriority(1);
        requestDTO.setDueDate(LocalDate.now());
        requestDTO.setProjectMember(mockMember);

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(taskDb.getTotalTask(STAGE_ID)).thenReturn(4);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        taskService.addNewTask(STAGE_ID, requestDTO);

        verify(taskDb).save(argThat(t -> t.getOrder() == 5));
    }

    @Test
    void addNewTask_shouldSetStatusToTodo() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setTaskName("Task X");
        requestDTO.setDescription("Desc");
        requestDTO.setPriority(1);
        requestDTO.setDueDate(LocalDate.now());
        requestDTO.setProjectMember(mockMember);

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(taskDb.getTotalTask(STAGE_ID)).thenReturn(0);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        taskService.addNewTask(STAGE_ID, requestDTO);

        verify(taskDb).save(argThat(t -> "TODO".equals(t.getStatus())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateTask
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateTask_withValidRequest_shouldUpdateFieldsAndSave() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setTaskName("Updated Task");
        requestDTO.setDescription("Updated Desc");
        requestDTO.setPriority(3);
        requestDTO.setDueDate(LocalDate.now().plusDays(3));
        requestDTO.setStatus("IN_PROGRESS");
        requestDTO.setProjectMember(mockMember);

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Task updated successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.updateTask(TASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(taskDb).save(mockTask);
        assertThat(mockTask.getTaskName()).isEqualTo("Updated Task");
        assertThat(mockTask.getStatus()).isEqualTo("IN_PROGRESS");
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getDetailTask — with assigned member
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getDetailTask_withAssignedMember_shouldReturnFullNameInResponse() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.getDetailTask(TASK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
                argThat(dto -> "Test User".equals(((TaskDetailResponseDTO) dto).getProjectMemberName())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getDetailTask — with no assigned member
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getDetailTask_withNoAssignedMember_shouldReturnUnassigned() {
        Task taskNoMember = mockTask.toBuilder().projectMember(null).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(taskNoMember);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        taskService.getDetailTask(TASK_ID);

        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
                argThat(dto -> "Unassigned".equals(((TaskDetailResponseDTO) dto).getProjectMemberName())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteTaskById — no subtasks
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteTaskById_withNoSubTasks_shouldDeleteAndUpdateOrder() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Tasks deleted successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.deleteTaskById(STAGE_ID, TASK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDb, never()).deleteAll(any());
        verify(taskDb).deleteById(TASK_ID);
        verify(taskDb).updateTaskOrderAfterDelete(STAGE_ID, 1);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteTaskById — with subtasks (cascade delete)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteTaskById_withSubTasks_shouldDeleteSubTasksThenTask() {
        SubTask mockSubTask = SubTask.builder().subTaskId("sub-001").build();
        Task taskWithSubs = mockTask.toBuilder().subTaskList(List.of(mockSubTask)).build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(taskWithSubs);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Tasks deleted successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.deleteTaskById(STAGE_ID, TASK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDb).deleteAll(List.of(mockSubTask));
        verify(taskDb).deleteById(TASK_ID);
        verify(taskDb).updateTaskOrderAfterDelete(STAGE_ID, 1);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderTask — move up (new order < current)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderTask_whenMovingUp_shouldCallUpdateTaskOrderAbove() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(TASK_ID);
        requestDTO.setOrder(1);

        Task taskOrder3 = mockTask.toBuilder().order(3).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        when(authService.validateTask(TASK_ID)).thenReturn(taskOrder3);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Tasks reordered successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.reorderTask(STAGE_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskDb).updateTaskOrderAbove(STAGE_ID, 0, 3);
        verify(taskDb).save(taskOrder3);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderTask — move down (new order > current)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderTask_whenMovingDown_shouldCallUpdateTaskOrderBelow() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(TASK_ID);
        requestDTO.setOrder(4);

        Task taskOrder2 = mockTask.toBuilder().order(2).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        when(authService.validateTask(TASK_ID)).thenReturn(taskOrder2);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Tasks reordered successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.reorderTask(STAGE_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskDb).updateTaskOrderBelow(STAGE_ID, 4, 3);
        verify(taskDb).save(taskOrder2);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderTask — same order (no-op on ordering)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderTask_whenSameOrder_shouldNotCallUpdateMethods() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(TASK_ID);
        requestDTO.setOrder(1); // same as mockTask.order

        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Tasks reordered successfully"), isNull()))
                .thenReturn(ResponseEntity.ok().build());

        taskService.reorderTask(STAGE_ID, requestDTO);

        verify(taskDb, never()).updateTaskOrderAbove(any(), any(), any());
        verify(taskDb, never()).updateTaskOrderBelow(any(), any(), any());
        verify(taskDb).save(mockTask);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateTaskStatus
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateTaskStatus_withValidStatus_shouldUpdateAndSave() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setStatus("IN_PROGRESS");

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = taskService.updateTaskStatus(TASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mockTask.getStatus()).isEqualTo("IN_PROGRESS");
        verify(taskDb).save(mockTask);
    }

    @Test
    void updateTaskStatus_shouldReturnCrudResponseDTOWithTaskId() {
        CreateUpdateTaskRequestDTO requestDTO = new CreateUpdateTaskRequestDTO();
        requestDTO.setStatus("DONE");

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"),
                argThat(dto -> TASK_ID.equals(((CrudResponseDTO) dto).getMessage()))))
                .thenReturn(ResponseEntity.ok().build());

        taskService.updateTaskStatus(TASK_ID, requestDTO);

        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"),
                argThat(dto -> TASK_ID.equals(((CrudResponseDTO) dto).getMessage())));
    }
}