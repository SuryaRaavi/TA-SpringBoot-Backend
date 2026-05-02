//package com.ta.managementproject.measure_test;
//
//import com.ta.managementproject.dto.BaseResponseDTO;
//import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
//import com.ta.managementproject.dto.request.ReorderRequestDTO;
//import com.ta.managementproject.dto.response.CrudResponseDTO;
//import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
//import com.ta.managementproject.dto.response.SubTaskResponseDTO;
//import com.ta.managementproject.entity.*;
//import com.ta.managementproject.repository.MemberInProjectDb;
//import com.ta.managementproject.repository.SubTaskDb;
//import com.ta.managementproject.repository.SubTaskDbWithDsl;
//import com.ta.managementproject.repository.TaskDb;
//import com.ta.managementproject.security.util.JwtUtils;
//import com.ta.managementproject.service.UtilService;
//import com.ta.managementproject.service.auth.AuthService;
//import com.ta.managementproject.service.task.SubTaskServiceImpl;
//import com.ta.managementproject.service.user.UserService;
//import jakarta.servlet.http.HttpServletRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.time.Instant;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SubTaskServiceTest {
//
//    @Mock private SubTaskDb subTaskDb;
//    @Mock private TaskDb taskDb;
//    @Mock private MemberInProjectDb memberInProjectDb;
//    @Mock private JwtUtils jwtUtils;
//    @Mock private HttpServletRequest request;
//    @Mock private UserService userService;
//    @Mock private AuthService authService;
//    @Mock private UtilService utilService;
//    @Mock private SubTaskDbWithDsl subTaskDbWithDsl;
//
//    @InjectMocks
//    private SubTaskServiceImpl subTaskService;
//
//    private static final String USERNAME   = "user_test";
//    private static final String TASK_ID    = "task-001";
//    private static final String SUBTASK_ID = "subtask-001";
//
//    private Project       mockProject;
//    private Stage         mockStage;
//    private Task          mockTask;
//    private SubTask       mockSubTask;
//    private ProjectMember mockMember;
//
//    @BeforeEach
//    void setUp() {
//        mockProject = Project.builder()
//                .projectId("project-001")
//                .projectName("Test Project")
//                .build();
//
//        mockStage = Stage.builder()
//                .stageId("stage-001")
//                .project(mockProject)
//                .build();
//
//        // subTaskList kosong agar checkAndUpdateTask() tidak NPE
//        mockTask = Task.builder()
//                .taskId(TASK_ID)
//                .taskName("Task Alpha")
//                .stage(mockStage)
//                .subTaskList(new ArrayList<>())
//                .build();
//
//        mockMember = ProjectMember.builder()
//                .username(USERNAME)
//                .fullName("Test User")
//                .build();
//
//        mockSubTask = SubTask.builder()
//                .subTaskId(SUBTASK_ID)
//                .subTaskName("SubTask Alpha")
//                .description("Desc")
//                .dueDate(Instant.now())
//                .status("TODO")
//                .label("bug")
//                .projectMember(mockMember)
//                .task(mockTask)
//                .order(1)
//                .build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * getAllSubTask
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void getAllSubTask_withValidAccess_shouldReturnPageOfSubTasks() {
//        Pageable pageable = PageRequest.of(0, 10);
//        Page<SubTaskResponseDTO> mockPage = new PageImpl<>(List.of());
//        ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        when(subTaskDbWithDsl.findAll(eq(TASK_ID), any(), any(), any(), any(), any(), eq(pageable)))
//                .thenReturn(mockPage);
//        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);
//
//        ResponseEntity<?> result = subTaskService.getAllSubTask(
//                TASK_ID, null, null, null, null, null, pageable);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(subTaskDbWithDsl).findAll(
//                eq(TASK_ID), any(), any(), any(), any(), any(), eq(pageable));
//    }
//
//    @Test
//    void getAllSubTask_withAllFilters_shouldPassFiltersToRepository() {
//        Pageable pageable = PageRequest.of(0, 10);
//        LocalDate today = LocalDate.now();
//        Page<SubTaskResponseDTO> mockPage = new PageImpl<>(List.of());
//        ResponseEntity<BaseResponseDTO<Page<SubTaskResponseDTO>>> mockResponse = ResponseEntity.ok().build();
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        when(subTaskDbWithDsl.findAll(
//                eq(TASK_ID), eq(today), eq(today), eq(today), eq(1), eq("Alpha"), eq(pageable)))
//                .thenReturn(mockPage);
//        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);
//
//        ResponseEntity<?> result = subTaskService.getAllSubTask(
//                TASK_ID, today, today, today, 1, "Alpha", pageable);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(subTaskDbWithDsl).findAll(
//                eq(TASK_ID), eq(today), eq(today), eq(today), eq(1), eq("Alpha"), eq(pageable));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * addNewSubTask
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void addNewSubTask_withValidRequest_shouldSaveSubTaskAndReturnCreated() {
//        CreateUpdateSubTaskRequestDTO dto = buildCreateDTO("New SubTask", "feature");
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(2);
//        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("Sub-task created successfully"), any()))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        ResponseEntity<?> result = subTaskService.addNewSubTask(TASK_ID, dto);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        verify(subTaskDb).save(any(SubTask.class));
//    }
//
//    @Test
//    void addNewSubTask_shouldSetOrderAsCurrentTotalPlusOne() {
//        CreateUpdateSubTaskRequestDTO dto = buildCreateDTO("SubTask X", "bug");
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(4);
//        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        subTaskService.addNewSubTask(TASK_ID, dto);
//
//        verify(subTaskDb).save(argThat(s -> s.getOrder() == 5));
//    }
//
//    @Test
//    void addNewSubTask_whenTotalIsNull_shouldSetOrderToOne() {
//        CreateUpdateSubTaskRequestDTO dto = buildCreateDTO("SubTask X", "bug");
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(null);
//        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        subTaskService.addNewSubTask(TASK_ID, dto);
//
//        verify(subTaskDb).save(argThat(s -> s.getOrder() == 1));
//    }
//
//    @Test
//    void addNewSubTask_shouldSetStatusToTodo() {
//        CreateUpdateSubTaskRequestDTO dto = buildCreateDTO("SubTask Y", "bug");
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(subTaskDb.getTotalSubTask(TASK_ID)).thenReturn(0);
//        when(utilService.buildResponse(eq(HttpStatus.CREATED), anyString(), any()))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        subTaskService.addNewSubTask(TASK_ID, dto);
//
//        verify(subTaskDb).save(argThat(s -> "TODO".equals(s.getStatus())));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * updateSubTask
//     * ══════════════════════════════════════════════════════════════════════
//     * Catatan: updateSubTask() di service TIDAK mengupdate field status.
//     * Field yang diupdate: subTaskName, description, dueDate, label, projectMember.
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void updateSubTask_withAllFields_shouldUpdateAndSave() {
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setSubTaskName("Updated SubTask");
//        dto.setDescription("Updated Desc");
//        dto.setDueDate(LocalDate.now().plusDays(5));
//        dto.setLabel("feature");
//        dto.setProjectMember(mockMember);
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-task updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> result = subTaskService.updateSubTask(SUBTASK_ID, dto);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        // Hanya field yang benar-benar di-set oleh service yang diverifikasi
//        assertThat(mockSubTask.getSubTaskName()).isEqualTo("Updated SubTask");
//        assertThat(mockSubTask.getDescription()).isEqualTo("Updated Desc");
//        assertThat(mockSubTask.getLabel()).isEqualTo("feature");
//        verify(subTaskDb).save(mockSubTask);
//    }
//
//    @Test
//    void updateSubTask_withNullFields_shouldKeepOriginalValues() {
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        // semua field null → service mempertahankan nilai lama
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("Sub-task updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.updateSubTask(SUBTASK_ID, dto);
//
//        verify(subTaskDb).save(argThat(s ->
//                "SubTask Alpha".equals(s.getSubTaskName()) &&
//                        "Desc".equals(s.getDescription())         &&
//                        "TODO".equals(s.getStatus())              &&
//                        "bug".equals(s.getLabel())
//        ));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * getDetailSubTask
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void getDetailSubTask_withAssignedMember_shouldReturnFullNameInResponse() {
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> result = subTaskService.getDetailSubTask(SUBTASK_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
//                argThat(dto ->
//                        "Test User".equals(((SubTaskDetailResponseDTO) dto).getProjectMemberName())));
//    }
//
//    @Test
//    void getDetailSubTask_withNoAssignedMember_shouldReturnUnassigned() {
//        SubTask subTaskNoMember = mockSubTask.toBuilder().projectMember(null).build();
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskNoMember);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.getDetailSubTask(SUBTASK_ID);
//
//        verify(utilService).buildResponse(eq(HttpStatus.OK), eq("SUCCESS"),
//                argThat(dto ->
//                        "Unassigned".equals(((SubTaskDetailResponseDTO) dto).getProjectMemberName())));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * deleteSubTaskById
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void deleteSubTaskById_shouldDeleteAndUpdateOrder() {
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"), any()))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        ResponseEntity<?> result = subTaskService.deleteSubTaskById(TASK_ID, SUBTASK_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        verify(subTaskDb).delete(mockSubTask);
//        verify(subTaskDb).updateSubTaskOrderAfterDelete(TASK_ID, 1);
//    }
//
//    @Test
//    void deleteSubTaskById_shouldReturnCrudResponseWithDeletedStatus() {
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"),
//                argThat(dto -> "DELETED".equals(((CrudResponseDTO) dto).getMessageDetail()))))
//                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());
//
//        subTaskService.deleteSubTaskById(TASK_ID, SUBTASK_ID);
//
//        verify(utilService).buildResponse(
//                eq(HttpStatus.CREATED), eq("Sub-task deleted successfully"),
//                argThat(dto -> "DELETED".equals(((CrudResponseDTO) dto).getMessageDetail())));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * reorderSubTask
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void reorderSubTask_whenMovingUp_shouldCallUpdateSubTaskOrderAbove() {
//        // current order = 3, target = 1 → pindah ke atas
//        SubTask subTaskOrder3 = mockSubTask.toBuilder().order(3).build();
//        ReorderRequestDTO dto = new ReorderRequestDTO();
//        dto.setId(SUBTASK_ID);
//        dto.setOrder(1);
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskOrder3);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> result = subTaskService.reorderSubTask(TASK_ID, dto);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        // updateSubTaskOrderAbove(taskId, newOrder - 1, currentOrder)
//        verify(subTaskDb).updateSubTaskOrderAbove(TASK_ID, 0, 3);
//        verify(subTaskDb).save(subTaskOrder3);
//    }
//
//    @Test
//    void reorderSubTask_whenMovingDown_shouldCallUpdateSubTaskOrderBelow() {
//        // current order = 2, target = 4 → pindah ke bawah
//        SubTask subTaskOrder2 = mockSubTask.toBuilder().order(2).build();
//        ReorderRequestDTO dto = new ReorderRequestDTO();
//        dto.setId(SUBTASK_ID);
//        dto.setOrder(4);
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subTaskOrder2);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> result = subTaskService.reorderSubTask(TASK_ID, dto);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        // updateSubTaskOrderBelow(taskId, newOrder, currentOrder + 1)
//        verify(subTaskDb).updateSubTaskOrderBelow(TASK_ID, 4, 3);
//        verify(subTaskDb).save(subTaskOrder2);
//    }
//
//    @Test
//    void reorderSubTask_whenSameOrder_shouldNotCallUpdateMethods() {
//        // current order = 1, target = 1 → tidak ada perubahan
//        ReorderRequestDTO dto = new ReorderRequestDTO();
//        dto.setId(SUBTASK_ID);
//        dto.setOrder(1);
//
//        when(authService.validateTask(TASK_ID)).thenReturn(mockTask);
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Sub-tasks reordered successfully"), isNull()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.reorderSubTask(TASK_ID, dto);
//
//        verify(subTaskDb, never()).updateSubTaskOrderAbove(any(), any(), any());
//        verify(subTaskDb, never()).updateSubTaskOrderBelow(any(), any(), any());
//        verify(subTaskDb).save(mockSubTask);
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * updateSubTaskStatus
//     * ══════════════════════════════════════════════════════════════════════
//     * CrudResponseDTO: message = subTaskId, messageDetail = Instant.now().toString()
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void updateSubTaskStatus_withValidStatus_shouldUpdateAndSave() {
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setStatus("IN_PROGRESS");
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        ResponseEntity<?> result = subTaskService.updateSubTaskStatus(SUBTASK_ID, dto);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(mockSubTask.getStatus()).isEqualTo("IN_PROGRESS");
//        verify(subTaskDb).save(mockSubTask);
//    }
//
//    @Test
//    void updateSubTaskStatus_shouldReturnCrudResponseWithSubTaskIdAsMessage() {
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setStatus("FINISHED");
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(mockSubTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        // message = subTaskId; messageDetail = timestamp string (tidak perlu divalidasi nilainya)
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"),
//                argThat(dto2 -> SUBTASK_ID.equals(((CrudResponseDTO) dto2).getMessage()))))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.updateSubTaskStatus(SUBTASK_ID, dto);
//
//        verify(utilService).buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"),
//                argThat(dto2 -> SUBTASK_ID.equals(((CrudResponseDTO) dto2).getMessage())));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * checkAndUpdateTask — skenario status task
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    @Test
//    void updateSubTaskStatus_whenAllSubTasksFinished_shouldSetTaskStatusToFinished() {
//        SubTask finished = mockSubTask.toBuilder().status("FINISHED").build();
//        Task taskWithSubTasks = mockTask.toBuilder()
//                .subTaskList(List.of(finished))
//                .build();
//        // subtask yang sedang diupdate juga perlu merujuk ke task yang benar
//        SubTask subtaskUnderTest = finished.toBuilder().task(taskWithSubTasks).build();
//
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setStatus("FINISHED");
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subtaskUnderTest);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.updateSubTaskStatus(SUBTASK_ID, dto);
//
//        verify(taskDb).save(argThat(t -> "FINISHED".equals(t.getStatus())));
//    }
//
//    @Test
//    void updateSubTaskStatus_whenAllSubTasksTodo_shouldSetTaskStatusToTodo() {
//        SubTask todo = mockSubTask.toBuilder().status("TODO").build();
//        Task taskWithSubTasks = mockTask.toBuilder()
//                .subTaskList(List.of(todo))
//                .build();
//        SubTask subtaskUnderTest = todo.toBuilder().task(taskWithSubTasks).build();
//
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setStatus("TODO");
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subtaskUnderTest);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.updateSubTaskStatus(SUBTASK_ID, dto);
//
//        verify(taskDb).save(argThat(t -> "TODO".equals(t.getStatus())));
//    }
//
//    @Test
//    void updateSubTaskStatus_whenMixedStatuses_shouldSetTaskStatusToInProgress() {
//        SubTask todo     = mockSubTask.toBuilder().subTaskId("st-1").status("TODO").build();
//        SubTask finished = mockSubTask.toBuilder().subTaskId("st-2").status("FINISHED").build();
//        Task taskWithSubTasks = mockTask.toBuilder()
//                .subTaskList(List.of(todo, finished))
//                .build();
//        SubTask subtaskUnderTest = todo.toBuilder().task(taskWithSubTasks).build();
//
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setStatus("TODO");
//
//        when(authService.validateSubTask(SUBTASK_ID)).thenReturn(subtaskUnderTest);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(
//                eq(HttpStatus.OK), eq("Task status updated successfully"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        subTaskService.updateSubTaskStatus(SUBTASK_ID, dto);
//
//        verify(taskDb).save(argThat(t -> "IN_PROGRESS".equals(t.getStatus())));
//    }
//
//    /* ══════════════════════════════════════════════════════════════════════
//     * Helper
//     * ══════════════════════════════════════════════════════════════════════ */
//
//    private CreateUpdateSubTaskRequestDTO buildCreateDTO(String name, String label) {
//        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
//        dto.setSubTaskName(name);
//        dto.setDescription("Desc");
//        dto.setDueDate(LocalDate.now().plusDays(3));
//        dto.setLabel(label);
//        dto.setProjectMember(mockMember);
//        return dto;
//    }
//}