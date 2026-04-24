package com.ta.managementproject.test;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.stage.StageServiceImpl;
import com.ta.managementproject.service.task.TaskService;
import com.ta.managementproject.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageServiceTest {

    @Mock private StageDb stageDb;
    @Mock private HttpServletRequest request;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserDb userDb;
    @Mock private ProjectDb projectDb;
    @Mock private TaskDb taskDb;
    @Mock private UserService userService;
    @Mock private SubTaskDb subTaskDb;
    @Mock private TaskService taskService;
    @Mock private UtilService utilService;
    @Mock private AuthService authService;
    @Mock private StageDbWithDsl stageDbWithDsl;

    @InjectMocks
    private StageServiceImpl stageService;

    private static final String USERNAME     = "user_test";
    private static final String PROJECT_ID   = "project-001";
    private static final String STAGE_ID     = "stage-001";

    private User        mockUserPm;
    private User        mockUserMember;
    private Project     mockProject;
    private Stage       mockStage;

    /* ── helper role builder ─────────────────────────────────────────────── */
    private User buildUser(String roleName) {
        Role role =
                Role.builder().name(roleName).build();
        return User.builder().username(USERNAME).role(role).build();
    }

    @BeforeEach
    void setUp() {
        mockUserPm     = buildUser("PROJECT_MANAGER");
        mockUserMember = buildUser("PROJECT_MEMBER");

        mockProject = Project.builder()
                .projectId(PROJECT_ID)
                .projectName("Test Project")
                .build();

        mockStage = Stage.builder()
                .stageId(STAGE_ID)
                .stageName("Stage Alpha")
                .description("Desc")
                .order(1)
                .project(mockProject)
                .taskList(List.of())
                .build();
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getAllStage
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getAllStage_asProjectManager_shouldCallFindAllWithPmUsername() {
        List<StageResponseDTO> mockList = List.of();
        ResponseEntity<BaseResponseDTO<List<StageResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(stageDbWithDsl.findAll(eq(USERNAME), isNull(), eq(PROJECT_ID))).thenReturn(mockList);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockList)).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.getAllStage(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDbWithDsl).findAll(eq(USERNAME), isNull(), eq(PROJECT_ID));
    }

    @Test
    void getAllStage_asMember_shouldCallFindAllWithMemberUsername() {
        List<StageResponseDTO> mockList = List.of();
        ResponseEntity<BaseResponseDTO<List<StageResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserMember);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(stageDbWithDsl.findAll(isNull(), eq(USERNAME), eq(PROJECT_ID))).thenReturn(mockList);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockList)).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.getAllStage(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDbWithDsl).findAll(isNull(), eq(USERNAME), eq(PROJECT_ID));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * addNewStage
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void addNewStage_withValidRequest_shouldSaveStageAndReturnOk() {
        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
        requestDTO.setStageName("New Stage");
        requestDTO.setDescription("Desc");

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDbWithDsl.totalStageByProject(PROJECT_ID)).thenReturn(2L);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.addNewStage(PROJECT_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDb).save(any(Stage.class));
    }

    @Test
    void addNewStage_shouldSetOrderAsNextAfterTotal() {
        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
        requestDTO.setStageName("Stage X");
        requestDTO.setDescription("Desc");

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDbWithDsl.totalStageByProject(PROJECT_ID)).thenReturn(3L);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        stageService.addNewStage(PROJECT_ID, requestDTO);

        verify(stageDb).save(argThat(s -> s.getOrder() == 4));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * editStage
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void editStage_withValidRequest_shouldSaveUpdatedStageAndReturnOk() {
        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
        requestDTO.setStageName("Updated Stage");
        requestDTO.setDescription("Updated Desc");

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.editStage(STAGE_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDb).save(any(Stage.class));
    }

    @Test
    void editStage_withNullFields_shouldKeepOriginalValues() {
        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
        requestDTO.setStageName(null);
        requestDTO.setDescription(null);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        stageService.editStage(STAGE_ID, requestDTO);

        verify(stageDb).save(argThat(s ->
                s.getStageName().equals("Stage Alpha") &&
                        s.getDescription().equals("Desc")
        ));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getStageStatistics — no tasks
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getStageStatistics_withNoTasks_shouldReturnZeroProgress() {
        Stage stageNoTasks = mockStage.toBuilder().taskList(List.of()).build();
        ProgressResponseDTO mockProgress = ProgressResponseDTO.builder()
                .totalTask(0L).finishedTask(0L).todoTask(0L).inProgressTask(0L).progress(0.00).build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse =
                ResponseEntity.ok(new BaseResponseDTO<>(200, "SUCCESS", new Date(), mockProgress));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateStage(STAGE_ID)).thenReturn(stageNoTasks);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> result =
                stageService.getStageStatistics(STAGE_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(subTaskDb, never()).getSubTaskSummary(any());
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getStageStatistics — task with subtasks (use subtask summary)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getStageStatistics_withTaskHavingNoSubTasks_shouldUseSubTaskSummary() {
        Task taskNoSub = Task.builder()
                .taskId("task-001")
                .status("TODO")
                .subTaskList(List.of())
                .build();

        Stage stageWithTask = mockStage.toBuilder().taskList(List.of(taskNoSub)).build();

        ProgressResponseDTO subSummary = ProgressResponseDTO.builder()
                .totalTask(3L).finishedTask(1L).todoTask(1L).inProgressTask(1L).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateStage(STAGE_ID)).thenReturn(stageWithTask);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(subTaskDb.getSubTaskSummary("task-001")).thenReturn(subSummary);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        stageService.getStageStatistics(STAGE_ID);

        verify(subTaskDb).getSubTaskSummary("task-001");
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getStageStatistics — task with subtasks (count by task status)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getStageStatistics_withTaskHavingSubTasks_shouldCountByTaskStatus() {
        SubTask dummySub = SubTask.builder().subTaskId("sub-001").build();

        Task taskTodo       = Task.builder().taskId("t1").status("TODO").subTaskList(List.of(dummySub)).build();
        Task taskInProgress = Task.builder().taskId("t2").status("IN_PROGRESS").subTaskList(List.of(dummySub)).build();
        Task taskDone       = Task.builder().taskId("t3").status("DONE").subTaskList(List.of(dummySub)).build();

        Stage stageWithTasks = mockStage.toBuilder()
                .taskList(List.of(taskTodo, taskInProgress, taskDone))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateStage(STAGE_ID)).thenReturn(stageWithTasks);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), argThat(dto -> {
            ProgressResponseDTO p = (ProgressResponseDTO) dto;
            return p.getTotalTask() == 3L &&
                    p.getTodoTask() == 1L &&
                    p.getInProgressTask() == 1L &&
                    p.getFinishedTask() == 1L;
        }))).thenReturn(ResponseEntity.ok().build());

        stageService.getStageStatistics(STAGE_ID);

        verify(subTaskDb, never()).getSubTaskSummary(any());
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderStage — move up (new order < current order)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderStage_whenMovingUp_shouldCallUpdateStageOrderAbove() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(STAGE_ID);
        requestDTO.setOrder(1);

        Stage stageOrder3 = mockStage.toBuilder().order(3).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDb.findByStageId(STAGE_ID)).thenReturn(stageOrder3);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.reorderStage(PROJECT_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDb).updateStageOrderAbove(PROJECT_ID, 0, 3);
        verify(stageDb).save(stageOrder3);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderStage — move down (new order > current order)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderStage_whenMovingDown_shouldCallUpdateStageOrderBelow() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(STAGE_ID);
        requestDTO.setOrder(4);

        Stage stageOrder2 = mockStage.toBuilder().order(2).build();
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDb.findByStageId(STAGE_ID)).thenReturn(stageOrder2);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.reorderStage(PROJECT_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageDb).updateStageOrderBelow(PROJECT_ID, 4, 3);
        verify(stageDb).save(stageOrder2);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * reorderStage — same order (no-op on ordering)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void reorderStage_whenSameOrder_shouldNotCallUpdateMethods() {
        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
        requestDTO.setId(STAGE_ID);
        requestDTO.setOrder(1); // same as mockStage.order

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDb.findByStageId(STAGE_ID)).thenReturn(mockStage);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        stageService.reorderStage(PROJECT_ID, requestDTO);

        verify(stageDb, never()).updateStageOrderAbove(any(), any(), any());
        verify(stageDb, never()).updateStageOrderBelow(any(), any(), any());
        verify(stageDb).save(mockStage);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteStageById — stage with no tasks
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteStageById_withNoTasks_shouldDeleteDirectlyAndUpdateOrder() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDb.findByStageId(STAGE_ID)).thenReturn(mockStage);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.deleteStageById(PROJECT_ID, STAGE_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskService, never()).deleteTaskById(any(), any());
        verify(stageDb).delete(mockStage);
        verify(stageDb).updateStageOrderAfterDelete(PROJECT_ID, 1);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteStageById — stage with tasks (cascade delete)
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteStageById_withTasks_shouldDeleteEachTaskThenStage() {
        Task mockTask = Task.builder().taskId("task-001").build();
        Stage stageWithTasks = mockStage.toBuilder().taskList(List.of(mockTask)).build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(stageDb.findByStageId(STAGE_ID))
                .thenReturn(stageWithTasks)  // first call: get stage + order
                .thenReturn(stageWithTasks); // second call: inside stageDb.delete(stageDb.findByStageId(...))
        doReturn(mockResponse)
                .when(taskService)
                .deleteTaskById(STAGE_ID, "task-001");
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = stageService.deleteStageById(PROJECT_ID, STAGE_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(taskService).deleteTaskById(STAGE_ID, "task-001");
        verify(stageDb).delete(stageWithTasks);
        verify(stageDb).updateStageOrderAfterDelete(PROJECT_ID, 1);
    }
}