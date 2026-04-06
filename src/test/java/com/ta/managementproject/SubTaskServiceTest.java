package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.repository.MemberInProjectDb;
import com.ta.managementproject.repository.SubTaskDb;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.task.SubTaskServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubTaskServiceTest {
    @InjectMocks
    private SubTaskServiceImpl subTaskService;

    @Mock private SubTaskDb subTaskDb;
    @Mock private TaskDb taskDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private JwtUtils jwtUtils;
    @Mock private HttpServletRequest request;

    // ─── Shared fixtures ──────────────────────────────────────────────────────────

    private User pmUser;
    private User memberUser;
    private User assignedMember;
    private Project project;
    private Stage stage;
    private Task task;
    private SubTask subTask;

    @BeforeEach
    void setUp() {
        pmUser = new ProjectManager();
        pmUser.setUsername("pm_user");

        memberUser = new ProjectMember();
        memberUser.setUsername("member_user");

        assignedMember = new ProjectMember();
        assignedMember.setUsername("assigned_member");
        assignedMember.setFullName("Assigned Member");

        project = new Project();
        project.setProjectId("project-1");
        project.setProjectManager((ProjectManager) pmUser);

        stage = new Stage();
        stage.setStageId("stage-1");
        stage.setProject(project);

        task = Task.builder()
                .taskId("task-1")
                .taskName("Task One")
                .stage(stage)
                .build();

        subTask = SubTask.builder()
                .subTaskId("subtask-1")
                .subTaskName("SubTask One")
                .description("Desc")
                .order(1)
                .isDeleted(false)
                .task(task)
                .build();
    }

    // ─── Authorization helpers ────────────────────────────────────────────────────

    /** PM mengakses via taskId */
    private void mockAsPMForTask() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
    }

    /** Member yang diizinkan mengakses via taskId */
    private void mockAsMemberForTask() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
        when(memberInProjectDb.findByProjectIdAndUsername("project-1", "member_user"))
                .thenReturn(new MemberInProject());
    }

    /** User yang tidak punya akses sama sekali */
    private void mockAsStrangerForTask() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("stranger");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));
        when(memberInProjectDb.findByProjectIdAndUsername("project-1", "stranger")).thenReturn(null);
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // getAllSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void getAllSubTask_asPM_withoutDateFilter_returnsOk() {
        mockAsPMForTask();
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of(new SubTaskResponseDTO()));
        when(subTaskDb.findSubTaskByTaskId(eq("task-1"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", null, null, "subTaskName", "ascending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertNotNull(body.getData());
        verify(subTaskDb).findSubTaskByTaskId(eq("task-1"), any(Pageable.class));
        verify(subTaskDb, never()).findSubTaskByTaskIdAndDueDate(any(), any(), any(), any());
    }

    @Test
    void getAllSubTask_asMember_withDateFilter_returnsOk() {
        mockAsMemberForTask();
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end   = Instant.parse("2024-12-31T00:00:00Z");
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());
        when(subTaskDb.findSubTaskByTaskIdAndDueDate(eq("task-1"), eq(start), eq(end), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", start, end, "dueDate", "descending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(subTaskDb).findSubTaskByTaskIdAndDueDate(eq("task-1"), eq(start), eq(end), any(Pageable.class));
        verify(subTaskDb, never()).findSubTaskByTaskId(any(), any());
    }

    @Test
    void getAllSubTask_withInvalidSortingColumn_returnsBadRequest() {
        mockAsPMForTask();

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", null, null, "invalidCol", "ascending");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals("Sorting column is not valid!", body.getMessage());
    }

    @Test
    void getAllSubTask_asStranger_returnsForbidden() {
        mockAsStrangerForTask();

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", null, null, "subTaskName", "ascending");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, ((BaseResponseDTO<?>) response.getBody()).getStatus());
    }

    @Test
    void getAllSubTask_taskNotFound_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", null, null, "subTaskName", "ascending");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getAllSubTask_descendingOrder_usesDescendingPageable() {
        mockAsPMForTask();
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());
        when(subTaskDb.findSubTaskByTaskId(eq("task-1"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 5, "task-1", null, null, "createdAt", "descending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<?> response = subTaskService.getAllSubTask(0, 10, "task-1", null, null, "subTaskName", "ascending");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(500, body.getStatus());
        assertEquals("JWT error", body.getMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // searchSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void searchSubTask_asPM_returnsOk() {
        mockAsPMForTask();
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of(new SubTaskResponseDTO()));
        when(subTaskDb.searchSubTaskByQuery(eq("task-1"), eq("keyword"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = subTaskService.searchSubTask(0, 10, "task-1", "keyword", "subTaskName", "ascending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        verify(subTaskDb).searchSubTaskByQuery(eq("task-1"), eq("keyword"), any(Pageable.class));
    }

    @Test
    void searchSubTask_withInvalidSortingColumn_returnsBadRequest() {
        mockAsPMForTask();

        ResponseEntity<?> response = subTaskService.searchSubTask(0, 10, "task-1", "keyword", "badColumn", "ascending");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Sorting column is not valid!", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    @Test
    void searchSubTask_asStranger_returnsForbidden() {
        mockAsStrangerForTask();

        ResponseEntity<?> response = subTaskService.searchSubTask(0, 10, "task-1", "keyword", "subTaskName", "ascending");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void searchSubTask_asMember_descendingOrder_returnsOk() {
        mockAsMemberForTask();
        Page<SubTaskResponseDTO> page = new PageImpl<>(List.of());
        when(subTaskDb.searchSubTaskByQuery(eq("task-1"), eq("q"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = subTaskService.searchSubTask(0, 5, "task-1", "q", "order", "descending");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void searchSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = subTaskService.searchSubTask(0, 10, "task-1", "q", "subTaskName", "ascending");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // addNewSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewSubTask_asPM_withExistingSubTasks_setsCorrectOrder() {
        mockAsPMForTask();
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(4);

        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
        dto.setSubTaskName("New SubTask");
        dto.setDescription("Desc");

        ResponseEntity<?> response = subTaskService.addNewSubTask("task-1", dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(201, body.getStatus());
        assertEquals("Sub-task created successfully", body.getMessage());
        // order harus = totalSubTask + 1 = 5
        verify(subTaskDb).save(argThat(st -> st.getOrder() == 5 && !st.isDeleted()));
    }

    @Test
    void addNewSubTask_asPM_withNullTotal_setsOrderToOne() {
        mockAsPMForTask();
        when(subTaskDb.getTotalSubTask("task-1")).thenReturn(null);

        ResponseEntity<?> response = subTaskService.addNewSubTask("task-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // order fallback ke 1 saat total null
        verify(subTaskDb).save(argThat(st -> st.getOrder() == 1));
    }

    @Test
    void addNewSubTask_notPM_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));

        ResponseEntity<?> response = subTaskService.addNewSubTask("task-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals("Only Project Managers can add sub-tasks", body.getMessage());
        verify(subTaskDb, never()).save(any());
    }

    @Test
    void addNewSubTask_taskNotFound_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.addNewSubTask("task-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void addNewSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = subTaskService.addNewSubTask("task-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(500, body.getStatus());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // updateSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void updateSubTask_asPM_updatesAllFieldsIncludingMember() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));

        User newMember = new ProjectMember();
        newMember.setUsername("new_member");

        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
        dto.setSubTaskName("Updated Name");
        dto.setDescription("Updated Desc");
        dto.setProjectMember((ProjectMember) newMember);

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        assertEquals("Sub-task updated successfully", body.getMessage());
        // PM bisa mengubah projectMember
        verify(subTaskDb).save(argThat(st ->
                st.getSubTaskName().equals("Updated Name") &&
                        st.getProjectMember() == newMember
        ));
    }

    @Test
    void updateSubTask_asAssignedMember_canUpdateStatusButNotMember() {
        subTask.setProjectMember((ProjectMember) assignedMember);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("assigned_member");
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));

        User anotherUser = new ProjectMember();
        anotherUser.setUsername("another");

        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
        dto.setSubTaskName("Member Updated");
        dto.setProjectMember((ProjectMember) anotherUser); // member tidak boleh mengganti assignee

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // projectMember tidak berubah karena bukan PM
        verify(subTaskDb).save(argThat(st -> st.getProjectMember() == assignedMember));
    }

    @Test
    void updateSubTask_withNullFields_keepsOriginalValues() {
        subTask.setSubTaskName("Original");
        subTask.setDescription("Original Desc");
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));

        CreateUpdateSubTaskRequestDTO dto = new CreateUpdateSubTaskRequestDTO();
        // semua field null

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(subTaskDb).save(argThat(st ->
                st.getSubTaskName().equals("Original") &&
                        st.getDescription().equals("Original Desc")
        ));
    }

    @Test
    void updateSubTask_subTaskNotFound_returns404() {
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Sub-task not found", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    @Test
    void updateSubTask_asStranger_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("stranger");
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        // subTask.projectMember = null, stranger bukan PM → forbidden

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Not authorized to update this sub-task", ((BaseResponseDTO<?>) response.getBody()).getMessage());
        verify(subTaskDb, never()).save(any());
    }

    @Test
    void updateSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(subTaskDb.findById("subtask-1")).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = subTaskService.updateSubTask("subtask-1", new CreateUpdateSubTaskRequestDTO());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // getDetailSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void getDetailSubTask_asPM_returnsCorrectDetail() {
        subTask.setProjectMember((ProjectMember) assignedMember);
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        mockAsPMForTask();

        ResponseEntity<?> response = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        assertInstanceOf(SubTaskDetailResponseDTO.class, body.getData());
        SubTaskDetailResponseDTO detail = (SubTaskDetailResponseDTO) body.getData();
        assertEquals("subtask-1", detail.getSubTaskId());
        assertEquals("SubTask One", detail.getSubTaskName());
        assertEquals("Assigned Member", detail.getProjectMemberName());
    }

    @Test
    void getDetailSubTask_withNullMember_showsUnassigned() {
        subTask.setProjectMember(null);
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        mockAsPMForTask();

        ResponseEntity<?> response = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        SubTaskDetailResponseDTO detail = (SubTaskDetailResponseDTO) ((BaseResponseDTO<?>) response.getBody()).getData();
        assertEquals("Unassigned", detail.getProjectMemberName());
    }

    @Test
    void getDetailSubTask_subTaskNotFound_returns404() {
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Sub-task not found", ((BaseResponseDTO<?>) response.getBody()).getMessage());
    }

    @Test
    void getDetailSubTask_asStranger_returnsForbidden() {
        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        mockAsStrangerForTask();

        ResponseEntity<?> response = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getDetailSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(subTaskDb.findById("subtask-1")).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = subTaskService.getDetailSubTask("subtask-1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // deleteSubTaskById
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void deleteSubTaskById_asPM_returnsOk() {
        mockAsPMForTask();

        ResponseEntity<?> response = subTaskService.deleteSubTaskById("task-1", "subtask-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        assertEquals("Sub-tasks deleted successfully", body.getMessage());
        verify(subTaskDb).deleteById("subtask-1");
    }

    @Test
    void deleteSubTaskById_notPM_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));

        ResponseEntity<?> response = subTaskService.deleteSubTaskById("task-1", "subtask-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(subTaskDb, never()).deleteById(any());
    }

    @Test
    void deleteSubTaskById_taskNotFound_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.deleteSubTaskById("task-1", "subtask-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(subTaskDb, never()).deleteById(any());
    }

    @Test
    void deleteSubTaskById_whenExceptionThrown_returnsInternalServerError() {
        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = subTaskService.deleteSubTaskById("task-1", "subtask-1");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // reorderSubTask
    // ══════════════════════════════════════════════════════════════════════════════

    @Test
    void reorderSubTask_asPM_updatesOrderAndReturnsOk() {
        mockAsPMForTask();

        SubTask subTask2 = SubTask.builder().subTaskId("subtask-2").order(0).task(task).build();
        ReorderRequestDTO r1 = new ReorderRequestDTO("subtask-1", 1);
        ReorderRequestDTO r2 = new ReorderRequestDTO("subtask-2", 0);

        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        when(subTaskDb.findById("subtask-2")).thenReturn(Optional.of(subTask2));

        ResponseEntity<?> response = subTaskService.reorderSubTask("task-1", List.of(r1, r2));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
        assertEquals(200, body.getStatus());
        assertEquals("Sub-tasks reordered successfully", body.getMessage());
        assertEquals(1, subTask.getOrder());
        assertEquals(0, subTask2.getOrder());
        verify(subTaskDb, times(2)).save(any(SubTask.class));
    }

    @Test
    void reorderSubTask_skipsNotFoundSubTask() {
        mockAsPMForTask();

        ReorderRequestDTO r1 = new ReorderRequestDTO("subtask-1", 2);
        ReorderRequestDTO r2 = new ReorderRequestDTO("subtask-missing", 0);

        when(subTaskDb.findById("subtask-1")).thenReturn(Optional.of(subTask));
        when(subTaskDb.findById("subtask-missing")).thenReturn(Optional.empty());

        ResponseEntity<?> response = subTaskService.reorderSubTask("task-1", List.of(r1, r2));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // hanya subtask-1 yang di-save, subtask-missing dilewati
        verify(subTaskDb, times(1)).save(any(SubTask.class));
        assertEquals(2, subTask.getOrder());
    }

    @Test
    void reorderSubTask_notPM_returnsForbidden() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
        when(taskDb.findById("task-1")).thenReturn(Optional.of(task));

        ResponseEntity<?> response = subTaskService.reorderSubTask("task-1", List.of());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(subTaskDb, never()).save(any());
    }

    @Test
    void reorderSubTask_whenExceptionThrown_returnsInternalServerError() {
        when(jwtUtils.getUserNameFromRequest(request)).thenThrow(new RuntimeException("error"));

        ResponseEntity<?> response = subTaskService.reorderSubTask("task-1", List.of());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
