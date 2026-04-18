package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.repository.MemberInProjectDb;
import com.ta.managementproject.repository.SubTaskDb;
import com.ta.managementproject.repository.SubTaskDbWithDsl;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.task.SubTaskServiceImpl;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubTaskServiceTest {

    @Mock private SubTaskDb subTaskDb;
    @Mock private TaskDb taskDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private JwtUtils jwtUtils;
    @Mock private HttpServletRequest request;
    @Mock private UserService userService;
    @Mock private AuthService authService;
    @Mock private UtilService utilService;
    @Mock private SubTaskDbWithDsl subTaskDbWithDsl;

    @InjectMocks
    private SubTaskServiceImpl subTaskService;

    private static final String USERNAME    = "user_test";
    private static final String TASK_ID     = "task-001";
    private static final String SUBTASK_ID  = "subtask-001";

    private Project       mockProject;
    private Stage         mockStage;
    private Task          mockTask;
    private SubTask       mockSubTask;
    private ProjectMember mockMember;

    @BeforeEach
    void setUp() {
        mockProject = Project.builder()
                .projectId("project-001")
                .projectName("Test Project")
                .build();

        mockStage = Stage.builder()
                .stageId("stage-001")
                .project(mockProject)
                .build();

        mockTask = Task.builder()
                .taskId(TASK_ID)
                .taskName("Task Alpha")
                .stage(mockStage)
                .build();

        mockMember = ProjectMember.builder()
                .username(USERNAME)
                .fullName("Test User")
                .build();

        mockSubTask = SubTask.builder()
                .subTaskId(SUBTASK_ID)
                .subTaskName("SubTask Alpha")
                .description("Desc")
                .dueDate(Instant.now())
                .status("TODO")
                .label("bug")
                .projectMember(mockMember)
                .task(mockTask)
                .order(1)
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getAllSubTask
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getAllSubTask_withValidAccess_shouldReturnPageOfSubTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SubTaskResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(subTaskDbWithDsl.findAll(eq(TASK_ID), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.getAllSubTask(TASK_ID, null, null, null, null, null, pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDbWithDsl).findAll(eq(TASK_ID), any(), any(), any(), any(), any(), eq(pageable));
    }

    @Test
    void getAllSubTask_withAllFilters_shouldPassFiltersToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now();
        Page<SubTaskResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(subTaskDbWithDsl.findAll(eq(TASK_ID), eq(today), eq(today), eq(today), eq(1), eq("Alpha"), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.getAllSubTask(TASK_ID, today, today, today, 1, "Alpha", pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDbWithDsl).findAll(eq(TASK_ID), eq(today), eq(today), eq(today), eq(1), eq("Alpha"), eq(pageable));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * addNewSubTask
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void addNewSubTask_withValidRequest_shouldSaveSubTaskAndReturnCreated() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName("New SubTask");
        requestDTO.setDescription("Desc");
        requestDTO.setDueDate(LocalDate.now().plusDays(3));
        requestDTO.setLabel("feature");
        requestDTO.setProjectMember(mockMember);

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(2);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Sub-task created successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.addNewSubTask(TASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(subTaskDb).save(any(SubTask.class));
    }

    @Test
    void addNewSubTask_shouldSetOrderAsCurrentTotalPlusOne() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName("SubTask X");
        requestDTO.setDescription("Desc");
        requestDTO.setDueDate(LocalDate.now());
        requestDTO.setLabel("bug");
        requestDTO.setProjectMember(mockMember);

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(4);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        subTaskService.addNewSubTask(TASK_ID, requestDTO);

        verify(subTaskDb).save(argThat(s -> s.getOrder() == 5));
    }

    @Test
    void addNewSubTask_whenTotalIsNull_shouldSetOrderToOne() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName("SubTask X");
        requestDTO.setDescription("Desc");
        requestDTO.setDueDate(LocalDate.now());
        requestDTO.setLabel("bug");
        requestDTO.setProjectMember(mockMember);

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(null);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        subTaskService.addNewSubTask(TASK_ID, requestDTO);

        verify(subTaskDb).save(argThat(s -> s.getOrder() == 1));
    }

    @Test
    void addNewSubTask_shouldSetStatusToTodo() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName("SubTask Y");
        requestDTO.setDescription("Desc");
        requestDTO.setDueDate(LocalDate.now());
        requestDTO.setLabel("bug");
        requestDTO.setProjectMember(mockMember);

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(0);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        subTaskService.addNewSubTask(TASK_ID, requestDTO);

        verify(subTaskDb).save(argThat(s -> "TODO".equals(s.getStatus())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateSubTask — with all fields provided
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateSubTask_withAllFields_shouldUpdateAndSave() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName("Updated SubTask");
        requestDTO.setDescription("Updated Desc");
        requestDTO.setDueDate(LocalDate.now().plusDays(5));
        requestDTO.setStatus("IN_PROGRESS");
        requestDTO.setLabel("feature");
        requestDTO.setProjectMember(mockMember);

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-task updated successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.updateSubTask(SUBTASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mockSubTask.getSubTaskName()).isEqualTo("Updated SubTask");
        assertThat(mockSubTask.getStatus()).isEqualTo("IN_PROGRESS");
        verify(subTaskDb).save(mockSubTask);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateSubTask — with null fields retains original values
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateSubTask_withNullFields_shouldKeepOriginalValues() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setSubTaskName(null);
        requestDTO.setDescription(null);
        requestDTO.setDueDate(null);
        requestDTO.setStatus(null);
        requestDTO.setLabel(null);
        requestDTO.setProjectMember(null);

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-task updated successfully"), any()))
                .thenReturn(ResponseEntity.ok().build());

        subTaskService.updateSubTask(SUBTASK_ID, requestDTO);

        verify(subTaskDb).save(argThat(s ->
                "SubTask Alpha".equals(s.getSubTaskName()) &&
                        "Desc".equals(s.getDescription()) &&
                        "TODO".equals(s.getStatus()) &&
                        "bug".equals(s.getLabel())
        ));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getDetailSubTask — with assigned member
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getDetailSubTask_withAssignedMember_shouldReturnFullNameInResponse() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.getDetailSubTask(SUBTASK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
                argThat(dto -> "Test User".equals(((SubTaskDetailResponseDTO) dto).getProjectMemberName())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getDetailSubTask — with no assigned member
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getDetailSubTask_withNoAssignedMember_shouldReturnUnassigned() {
        SubTask subTaskNoMember = mockSubTask.toBuilder().projectMember(null).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskNoMember);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        subTaskService.getDetailSubTask(SUBTASK_ID);

        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
                argThat(dto -> "Unassigned".equals(((SubTaskDetailResponseDTO) dto).getProjectMemberName())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteSubTaskById
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteSubTaskById_shouldDeleteAndUpdateOrder() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.deleteSubTaskById(TASK_ID, SUBTASK_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(subTaskDb).delete(mockSubTask);
        verify(subTaskDb).updateSubTaskOrderAfterDelete(TASK_ID, 1);
    }

    @Test
    void deleteSubTaskById_shouldReturnCrudResponseWithDeletedStatus() {
        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"),
                argThat(dto -> "DELETED".equals(((CrudResponseDTO) dto).getMessageDetail()))))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        subTaskService.deleteSubTaskById(TASK_ID, SUBTASK_ID);

        verify(utilService).buildResponse(eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"),
                argThat(dto -> "DELETED".equals(((CrudResponseDTO) dto).getMessageDetail())));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderSubTask — move up (new order < current)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderSubTask_whenMovingUp_shouldCallUpdateSubTaskOrderAbove() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(SUBTASK_ID);
        requestDTO.setOrder(1);

        SubTask subTaskOrder3 = mockSubTask.toBuilder().order(3).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskOrder3);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.reorderSubTask(TASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDb).updateSubTaskOrderAbove(TASK_ID, 0, 3);
        verify(subTaskDb).save(subTaskOrder3);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderSubTask — move down (new order > current)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderSubTask_whenMovingDown_shouldCallUpdateSubTaskOrderBelow() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(SUBTASK_ID);
        requestDTO.setOrder(4);

        SubTask subTaskOrder2 = mockSubTask.toBuilder().order(2).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskOrder2);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.reorderSubTask(TASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDb).updateSubTaskOrderBelow(TASK_ID, 4, 3);
        verify(subTaskDb).save(subTaskOrder2);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderSubTask — same order (no-op on ordering)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderSubTask_whenSameOrder_shouldNotCallUpdateMethods() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(SUBTASK_ID);
        requestDTO.setOrder(1); // same as mockSubTask.order

        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
                .thenReturn(ResponseEntity.ok().build());

        subTaskService.reorderSubTask(TASK_ID, requestDTO);

        verify(subTaskDb, never()).updateSubTaskOrderAbove(any(), any(), any());
        verify(subTaskDb, never()).updateSubTaskOrderBelow(any(), any(), any());
        verify(subTaskDb).save(mockSubTask);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateSubTaskStatus
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateSubTaskStatus_withValidStatus_shouldUpdateAndSave() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setStatus("IN_PROGRESS");

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
                .thenReturn(mockResponse);

        ResponseEntity<?> result = subTaskService.updateSubTaskStatus(SUBTASK_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mockSubTask.getStatus()).isEqualTo("IN_PROGRESS");
        verify(subTaskDb).save(mockSubTask);
    }

    @Test
    void updateSubTaskStatus_shouldReturnCrudResponseWithSubTaskId() {
        CreateUpdateSubTaskRequestDTO requestDTO = new CreateUpdateSubTaskRequestDTO();
        requestDTO.setStatus("DONE");

        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"),
                argThat(dto -> SUBTASK_ID.equals(((CrudResponseDTO) dto).getMessage()))))
                .thenReturn(ResponseEntity.ok().build());

        subTaskService.updateSubTaskStatus(SUBTASK_ID, requestDTO);

        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("Task status updated successfully"),
                argThat(dto -> SUBTASK_ID.equals(((CrudResponseDTO) dto).getMessage())));
    }
}