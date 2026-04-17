//package com.ta.managementproject;
//
//import com.ta.managementproject.dto.BaseResponseDTO;
//import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
//import com.ta.managementproject.dto.request.ReorderRequestDTO;
//import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
//import com.ta.managementproject.dto.response.TaskResponseDTO;
//import com.ta.managementproject.entity.*;
//import com.ta.managementproject.repository.*;
//import com.ta.managementproject.security.util.JwtUtils;
//import com.ta.managementproject.service.task.TaskServiceImpl;
//import jakarta.servlet.http.HttpServletRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class TaskServiceTest {
//
//    @InjectMocks
//    private TaskServiceImpl taskService;
//
//    @Mock
//    private TaskDb taskDb;
//    @Mock private ProjectDb projectDb;
//    @Mock private StageDb stageDb;
//    @Mock private JwtUtils jwtUtils;
//    @Mock private HttpServletRequest request;
//    @Mock private MemberInProjectDb memberInProjectDb;
//    @Mock private ProjectManagerDb projectManagerDb;
//    @Mock private ProjectMemberDb projectMemberDb;
//
//    // ─── Shared fixtures ─────────────────────────────────────────────────────────
//
//    private User pmUser;
//    private User memberUser;
//    private Project project;
//    private Stage stage;
//    private Task task;
//
//    @BeforeEach
//    void setUp() {
//        pmUser = new ProjectManager();
//        pmUser.setUsername("pm_user");
//
//        memberUser = new ProjectMember();
//        memberUser.setUsername("member_user");
//
//        project = new Project();
//        project.setProjectId("project-1");
//        project.setProjectManager((ProjectManager) pmUser);
//
//        stage = new Stage();
//        stage.setStageId("stage-1");
//        stage.setProject(project);
//
//        task = Task.builder()
//                .taskId("task-1")
//                .taskName("Task One")
//                .description("Desc")
//                .order(1)
//                .isDeleted(false)
//                .stage(stage)
//                .build();
//    }
//
//    // ─── Helper: authorize as PM ──────────────────────────────────────────────────
//
//    private void mockAsPM() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//    }
//
//    private void mockAsMember() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//        when(memberInProjectDb.findByProjectIdAndUsername("project-1", "member_user"))
//                .thenReturn(new MemberInProject()); // non-null = authorized
//    }
//
//    private void mockAsStranger() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("stranger");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//        when(memberInProjectDb.findByProjectIdAndUsername("project-1", "stranger")).thenReturn(null);
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // getAllTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void getAllTask_asPM_withoutDateFilter_returnsOk() {
//        mockAsPM();
//        Page<TaskResponseDTO> page = new PageImpl<>(List.of(new TaskResponseDTO()));
//        when(taskDb.findTaskByStageId(eq("stage-1"), any(Pageable.class))).thenReturn(page);
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", null, null, "taskName", "ascending");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(200, body.getStatus());
//        assertEquals("Success", body.getMessage());
//        assertNotNull(body.getData());
//    }
//
//    @Test
//    void getAllTask_asMember_withDateFilter_returnsOk() {
//        mockAsMember();
//        Instant start = Instant.parse("2024-01-01T00:00:00Z");
//        Instant end   = Instant.parse("2024-12-31T00:00:00Z");
//        Page<TaskResponseDTO> page = new PageImpl<>(List.of());
//        when(taskDb.findTaskByStageIdAndDueDate(eq("stage-1"), eq(start), eq(end), any(Pageable.class))).thenReturn(page);
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", start, end, "dueDate", "descending");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(taskDb).findTaskByStageIdAndDueDate(eq("stage-1"), eq(start), eq(end), any(Pageable.class));
//        verify(taskDb, never()).findTaskByStageId(any(), any());
//    }
//
//    @Test
//    void getAllTask_withInvalidSortingColumn_returnsBadRequest() {
//        mockAsPM();
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", null, null, "invalidColumn", "ascending");
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals("Sorting column is not valid!", body.getMessage());
//    }
//
//    @Test
//    void getAllTask_asStranger_returnsForbidden() {
//        mockAsStranger();
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", null, null, "taskName", "ascending");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(403, body.getStatus());
//    }
//
//    @Test
//    void getAllTask_stageNotFound_returnsForbidden() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.empty());
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", null, null, "taskName", "ascending");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void getAllTask_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("JWT error"));
//
//        ResponseEntity<?> response = taskService.getAllTask(0, 10, "stage-1", null, null, "taskName", "ascending");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(500, body.getStatus());
//        assertEquals("JWT error", body.getMessage());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // searchTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void searchTask_asPM_returnsOk() {
//        mockAsPM();
//        Page<TaskResponseDTO> page = new PageImpl<>(List.of(new TaskResponseDTO()));
//        when(taskDb.searchTaskByQuery(eq("stage-1"), eq("keyword"), any(Pageable.class))).thenReturn(page);
//
//        ResponseEntity<?> response = taskService.searchTask(0, 10, "stage-1", "keyword", "taskName", "ascending");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(taskDb).searchTaskByQuery(eq("stage-1"), eq("keyword"), any(Pageable.class));
//    }
//
//    @Test
//    void searchTask_withInvalidSortingColumn_returnsBadRequest() {
//        mockAsPM();
//
//        ResponseEntity<?> response = taskService.searchTask(0, 10, "stage-1", "keyword", "badColumn", "ascending");
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    @Test
//    void searchTask_asStranger_returnsForbidden() {
//        mockAsStranger();
//
//        ResponseEntity<?> response = taskService.searchTask(0, 10, "stage-1", "keyword", "taskName", "ascending");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void searchTask_descendingOrder_usesDescendingPageable() {
//        mockAsPM();
//        Page<TaskResponseDTO> page = new PageImpl<>(List.of());
//        when(taskDb.searchTaskByQuery(eq("stage-1"), eq("q"), any(Pageable.class))).thenReturn(page);
//
//        ResponseEntity<?> response = taskService.searchTask(0, 5, "stage-1", "q", "createdAt", "descending");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void searchTask_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));
//
//        ResponseEntity<?> response = taskService.searchTask(0, 10, "stage-1", "q", "taskName", "ascending");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // addNewTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void addNewTask_asPM_returnsOk() {
//        mockAsPM();
//        when(taskDb.getTotalTask("stage-1")).thenReturn(3);
//
//        CreateUpdateTaskRequestDTO dto = new CreateUpdateTaskRequestDTO();
//        dto.setTaskName("New Task");
//        dto.setDescription("Desc");
//
//        ResponseEntity<?> response = taskService.addNewTask("stage-1", dto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(201, body.getStatus());
//        assertEquals("Task created successfully", body.getMessage());
//        verify(taskDb).save(argThat(t -> t.getOrder() == 4 && !t.isDeleted()));
//    }
//
//    @Test
//    void addNewTask_notPM_returnsForbidden() {
//        // mockAsMember() makes isProjectManager return false (not PM)
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//
//        ResponseEntity<?> response = taskService.addNewTask("stage-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(taskDb, never()).save(any());
//    }
//
//    @Test
//    void addNewTask_stageNotFound_returnsForbidden() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.empty());
//
//        ResponseEntity<?> response = taskService.addNewTask("stage-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void addNewTask_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("DB error"));
//
//        ResponseEntity<?> response = taskService.addNewTask("stage-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(500, body.getStatus());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // updateTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void updateTask_asPM_returnsOk() {
//        mockAsPM();
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        CreateUpdateTaskRequestDTO dto = new CreateUpdateTaskRequestDTO();
//        dto.setTaskName("Updated Name");
//        dto.setDescription("Updated Desc");
//
//        ResponseEntity<?> response = taskService.updateTask("task-1", dto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(200, body.getStatus());
//        assertEquals("Task updated successfully", body.getMessage());
//        verify(taskDb).save(task);
//        assertEquals("Updated Name", task.getTaskName());
//    }
//
//    @Test
//    void updateTask_asMember_returnsOk() {
//        mockAsMember();
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        ResponseEntity<?> response = taskService.updateTask("task-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    void updateTask_taskNotFound_returns404() {
//        when(taskDb.findById("task-1")).thenReturn(Optional.empty());
//
//        ResponseEntity<?> response = taskService.updateTask("task-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(404, response.getStatusCode().value());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals("Task not found", body.getMessage());
//    }
//
//    @Test
//    void updateTask_asStranger_returnsForbidden() {
//        mockAsStranger();
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        ResponseEntity<?> response = taskService.updateTask("task-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(taskDb, never()).save(any());
//    }
//
//    @Test
//    void updateTask_whenExceptionThrown_returnsInternalServerError() {
//        when(taskDb.findById("task-1")).thenThrow(new RuntimeException("DB error"));
//
//        ResponseEntity<?> response = taskService.updateTask("task-1", new CreateUpdateTaskRequestDTO());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // getDetailTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void getDetailTask_asPM_returnsOk() {
//        mockAsPM();
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        ResponseEntity<?> response = taskService.getDetailTask("task-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(200, body.getStatus());
//        assertInstanceOf(TaskDetailResponseDTO.class, body.getData());
//        TaskDetailResponseDTO data = (TaskDetailResponseDTO) body.getData();
//        assertEquals("task-1", data.getTaskId());
//        assertEquals("Task One", data.getTaskName());
//    }
//
//    @Test
//    void getDetailTask_taskWithNullMember_showsUnassigned() {
//        mockAsPM();
//        task.setProjectMember(null);
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        ResponseEntity<?> response = taskService.getDetailTask("task-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        TaskDetailResponseDTO data = (TaskDetailResponseDTO) ((BaseResponseDTO<?>) response.getBody()).getData();
//        assertEquals("Unassigned", data.getProjectMemberName());
//    }
//
//    @Test
//    void getDetailTask_taskNotFound_returns404() {
//        when(taskDb.findById("task-1")).thenReturn(Optional.empty());
//
//        ResponseEntity<?> response = taskService.getDetailTask("task-1");
//
//        assertEquals(404, response.getStatusCode().value());
//        assertEquals("Task not found", ((BaseResponseDTO<?>) response.getBody()).getMessage());
//    }
//
//    @Test
//    void getDetailTask_asStranger_returnsForbidden() {
//        mockAsStranger();
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//
//        ResponseEntity<?> response = taskService.getDetailTask("task-1");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void getDetailTask_whenExceptionThrown_returnsInternalServerError() {
//        when(taskDb.findById("task-1")).thenThrow(new RuntimeException("error"));
//
//        ResponseEntity<?> response = taskService.getDetailTask("task-1");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // deleteTaskById
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void deleteTaskById_asPM_returnsOk() {
//        mockAsPM();
//
//        ResponseEntity<?> response = taskService.deleteTaskById("stage-1", "task-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(200, body.getStatus());
//        assertEquals("Tasks deleted successfully", body.getMessage());
//        verify(taskDb).deleteById("task-1");
//    }
//
//    @Test
//    void deleteTaskById_notPM_returnsForbidden() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//
//        ResponseEntity<?> response = taskService.deleteTaskById("stage-1", "task-1");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(taskDb, never()).deleteById(any());
//    }
//
//    @Test
//    void deleteTaskById_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));
//
//        ResponseEntity<?> response = taskService.deleteTaskById("stage-1", "task-1");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // reorderTask
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void reorderTask_asPM_updatesOrderAndReturnsOk() {
//        mockAsPM();
//
//        Task task2 = Task.builder().taskId("task-2").order(0).stage(stage).build();
//        ReorderRequestDTO r1 = new ReorderRequestDTO("task-1", 1);
//        ReorderRequestDTO r2 = new ReorderRequestDTO("task-2", 0);
//
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//        when(taskDb.findById("task-2")).thenReturn(Optional.of(task2));
//
//        ResponseEntity<?> response = taskService.reorderTask("stage-1", List.of(r1, r2));
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertEquals(200, body.getStatus());
//        assertEquals("Tasks reordered successfully", body.getMessage());
//        assertEquals(1, task.getOrder());
//        assertEquals(0, task2.getOrder());
//        verify(taskDb, times(2)).save(any(Task.class));
//    }
//
//    @Test
//    void reorderTask_skipsNotFoundTask() {
//        mockAsPM();
//
//        ReorderRequestDTO r1 = new ReorderRequestDTO("task-1", 2);
//        ReorderRequestDTO r2 = new ReorderRequestDTO("task-missing", 0);
//
//        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
//        when(taskDb.findById("task-missing")).thenReturn(Optional.empty());
//
//        ResponseEntity<?> response = taskService.reorderTask("stage-1", List.of(r1, r2));
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        // Only task-1 should be saved; task-missing is skipped silently
//        verify(taskDb, times(1)).save(any(Task.class));
//    }
//
//    @Test
//    void reorderTask_notPM_returnsForbidden() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
//        when(stageDb.findById("stage-1")).thenReturn(Optional.of(stage));
//
//        ResponseEntity<?> response = taskService.reorderTask("stage-1", List.of());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(taskDb, never()).save(any());
//    }
//
//    @Test
//    void reorderTask_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));
//
//        ResponseEntity<?> response = taskService.reorderTask("stage-1", List.of());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//}
