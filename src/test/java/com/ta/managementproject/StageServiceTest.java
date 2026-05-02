//package com.ta.managementproject;
//
//import com.ta.managementproject.dto.BaseResponseDTO;
//import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
//import com.ta.managementproject.dto.request.ReorderRequestDTO;
//import com.ta.managementproject.dto.response.ProgressResponseDTO;
//import com.ta.managementproject.dto.response.StageResponseDTO;
//import com.ta.managementproject.entity.*;
//import com.ta.managementproject.repository.*;
//import com.ta.managementproject.security.util.JwtUtils;
//import com.ta.managementproject.service.UtilService;
//import com.ta.managementproject.service.auth.AuthService;
//import com.ta.managementproject.service.stage.StageServiceImpl;
//import com.ta.managementproject.service.task.TaskService;
//import com.ta.managementproject.service.user.UserService;
//import jakarta.servlet.http.HttpServletRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class StageServiceTest {
//
//    @Mock private StageDb stageDb;
//    @Mock private HttpServletRequest request;
//    @Mock private JwtUtils jwtUtils;
//    @Mock private UserDb userDb;
//    @Mock private ProjectDb projectDb;
//    @Mock private TaskDb taskDb;
//    @Mock private UserService userService;
//    @Mock private SubTaskDb subTaskDb;
//    @Mock private TaskService taskService;
//    @Mock private UtilService utilService;
//    @Mock private AuthService authService;
//    @Mock private StageDbWithDsl stageDbWithDsl;
//
//    @InjectMocks
//    private StageServiceImpl stageService;
//
//    private static final String USERNAME   = "user_test";
//    private static final String PROJECT_ID = "project-001";
//    private static final String STAGE_ID   = "stage-001";
//
//    private User    mockUserPm;
//    private User    mockUserMember;
//    private Project mockProject;
//    private Stage   mockStage;
//
//    /* ── helper ──────────────────────────────────────────────────────────── */
//    private User buildUser(String roleName) {
//        com.ta.managementproject.entity.Role role =
//                com.ta.managementproject.entity.Role.builder().name(roleName).build();
//        return User.builder().username(USERNAME).role(role).build();
//    }
//
//    @BeforeEach
//    void setUp() {
//        mockUserPm     = buildUser("PROJECT_MANAGER");
//        mockUserMember = buildUser("PROJECT_MEMBER");
//
//        mockProject = Project.builder()
//                .projectId(PROJECT_ID)
//                .projectName("Test Project")
//                .build();
//
//        mockStage = Stage.builder()
//                .stageId(STAGE_ID)
//                .stageName("Stage Alpha")
//                .description("Desc")
//                .order(1)
//                .project(mockProject)
//                .taskList(List.of())
//                .build();
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // getAllStage — as PROJECT_MANAGER
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * getAllStage memanggil getStageStatistics (public) per item dalam list.
//     * Karena stageList yang dikembalikan oleh stageDbWithDsl kosong,
//     * getStageStatistics tidak akan dipanggil sehingga tidak perlu stub tambahan.
//     */
//    @Test
//    void getAllStage_asProjectManager_shouldCallFindAllWithPmUsername() {
//        List<StageResponseDTO> mockList = List.of();
//        ResponseEntity<BaseResponseDTO<List<StageResponseDTO>>> mockResponse =
//                ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        when(stageDbWithDsl.findAll(eq(USERNAME), isNull(), eq(PROJECT_ID)))
//                .thenReturn(mockList);
//        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockList))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.getAllStage(PROJECT_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDbWithDsl).findAll(eq(USERNAME), isNull(), eq(PROJECT_ID));
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // getAllStage — as PROJECT_MEMBER
//    // ════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void getAllStage_asMember_shouldCallFindAllWithMemberUsername() {
//        List<StageResponseDTO> mockList = List.of();
//        ResponseEntity<BaseResponseDTO<List<StageResponseDTO>>> mockResponse =
//                ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserMember);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        when(stageDbWithDsl.findAll(isNull(), eq(USERNAME), eq(PROJECT_ID)))
//                .thenReturn(mockList);
//        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockList))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.getAllStage(PROJECT_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDbWithDsl).findAll(isNull(), eq(USERNAME), eq(PROJECT_ID));
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // addNewStage — sukses, order = total + 1
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service memanggil validateProjectCancellation setelah validateManagerAccess.
//     */
//    @Test
//    void addNewStage_withValidRequest_shouldSaveStageAndReturnOk() {
//        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
//        requestDTO.setStageName("New Stage");
//        requestDTO.setDescription("Desc");
//
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDbWithDsl.totalStageByProject(PROJECT_ID)).thenReturn(2L);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.addNewStage(PROJECT_ID, requestDTO);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDb).save(any(Stage.class));
//        verify(authService).validateProjectCancellation(mockProject);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // addNewStage — order ditetapkan sebagai total + 1
//    // ════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void addNewStage_shouldSetOrderAsNextAfterTotal() {
//        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
//        requestDTO.setStageName("Stage X");
//        requestDTO.setDescription("Desc");
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDbWithDsl.totalStageByProject(PROJECT_ID)).thenReturn(3L);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        stageService.addNewStage(PROJECT_ID, requestDTO);
//
//        // total = 3 → order harus 4
//        verify(stageDb).save(argThat(s -> s.getOrder() == 4));
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // editStage — sukses
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service memanggil validateProjectCancellation(stage.getProject()) setelah
//     * validateManagerAccess.
//     */
//    @Test
//    void editStage_withValidRequest_shouldSaveUpdatedStageAndReturnOk() {
//        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
//        requestDTO.setStageName("Updated Stage");
//        requestDTO.setDescription("Updated Desc");
//
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.editStage(STAGE_ID, requestDTO);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDb).save(any(Stage.class));
//        verify(authService).validateProjectCancellation(mockProject);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // editStage — field null → tetap nilai lama
//    // ════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void editStage_withNullFields_shouldKeepOriginalValues() {
//        CreateUpdateStageRequestDTO requestDTO = new CreateUpdateStageRequestDTO();
//        requestDTO.setStageName(null);
//        requestDTO.setDescription(null);
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateStage(STAGE_ID)).thenReturn(mockStage);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        stageService.editStage(STAGE_ID, requestDTO);
//
//        verify(stageDb).save(argThat(s ->
//                s.getStageName().equals("Stage Alpha") &&
//                        s.getDescription().equals("Desc")
//        ));
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // getStageStatistics — tanpa task → progress nol, subTaskDb tidak dipanggil
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * getStageStatistics adalah public dan mengembalikan ProgressResponseDTO langsung,
//     * bukan ResponseEntity. Verifikasi dilakukan terhadap nilai DTO yang dikembalikan.
//     */
//    @Test
//    void getStageStatistics_withNoTasks_shouldReturnZeroProgress() {
//        Stage stageNoTasks = mockStage.toBuilder().taskList(List.of()).build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(authService.validateStage(STAGE_ID)).thenReturn(stageNoTasks);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//
//        ProgressResponseDTO result = stageService.getStageStatistics(STAGE_ID);
//
//        assertThat(result.getTotalTask()).isZero();
//        assertThat(result.getFinishedTask()).isZero();
//        assertThat(result.getTodoTask()).isZero();
//        assertThat(result.getInProgressTask()).isZero();
//        assertThat(result.getProgress()).isEqualTo(0.00);
//        verify(subTaskDb, never()).getSubTaskSummary(any());
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // getStageStatistics — task tanpa subtask → delegasi ke subTaskDb
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Logika service: jika subTaskList KOSONG → panggil subTaskDb.getSubTaskSummary.
//     * (kebalikan dari nama test lama yang menyesatkan)
//     */
//    @Test
//    void getStageStatistics_whenTaskHasNoSubTasks_shouldDelegateToSubTaskDb() {
//        Task taskNoSub = Task.builder()
//                .taskId("task-001")
//                .status("TODO")
//                .subTaskList(List.of())   // kosong → masuk blok subTaskDb
//                .build();
//
//        Stage stageWithTask = mockStage.toBuilder().taskList(List.of(taskNoSub)).build();
//
//        ProgressResponseDTO subSummary = ProgressResponseDTO.builder()
//                .totalTask(3L).finishedTask(1L).todoTask(1L).inProgressTask(1L).build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(authService.validateStage(STAGE_ID)).thenReturn(stageWithTask);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//        when(subTaskDb.getSubTaskSummary("task-001")).thenReturn(subSummary);
//
//        ProgressResponseDTO result = stageService.getStageStatistics(STAGE_ID);
//
//        verify(subTaskDb).getSubTaskSummary("task-001");
//        assertThat(result.getTotalTask()).isEqualTo(3L);
//        assertThat(result.getFinishedTask()).isEqualTo(1L);
//        assertThat(result.getTodoTask()).isEqualTo(1L);
//        assertThat(result.getInProgressTask()).isEqualTo(1L);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // getStageStatistics — task dengan subtask → hitung dari status task
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Logika service: jika subTaskList TIDAK KOSONG → hitung langsung dari status task,
//     * subTaskDb tidak dipanggil.
//     */
//    @Test
//    void getStageStatistics_whenTaskHasSubTasks_shouldCountByTaskStatus() {
//        SubTask dummySub = SubTask.builder().subTaskId("sub-001").build();
//
//        Task taskTodo       = Task.builder().taskId("t1").status("TODO")
//                .subTaskList(List.of(dummySub)).build();
//        Task taskInProgress = Task.builder().taskId("t2").status("IN_PROGRESS")
//                .subTaskList(List.of(dummySub)).build();
//        Task taskDone       = Task.builder().taskId("t3").status("DONE")
//                .subTaskList(List.of(dummySub)).build();
//
//        Stage stageWithTasks = mockStage.toBuilder()
//                .taskList(List.of(taskTodo, taskInProgress, taskDone))
//                .build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(authService.validateStage(STAGE_ID)).thenReturn(stageWithTasks);
//        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
//
//        ProgressResponseDTO result = stageService.getStageStatistics(STAGE_ID);
//
//        // subTaskDb tidak boleh dipanggil karena semua task punya subtask
//        verify(subTaskDb, never()).getSubTaskSummary(any());
//
//        assertThat(result.getTotalTask()).isEqualTo(3L);
//        assertThat(result.getTodoTask()).isEqualTo(1L);
//        assertThat(result.getInProgressTask()).isEqualTo(1L);
//        assertThat(result.getFinishedTask()).isEqualTo(1L);
//        assertThat(result.getProgress()).isEqualTo(1.0 / 3.0 * 100);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // reorderStage — pindah ke atas (newOrder < currentOrder)
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service: updateStageOrderAbove(projectId, newOrder - 1, currentOrder).
//     * Dengan newOrder = 1 dan currentOrder = 3 → updateStageOrderAbove(id, 0, 3).
//     */
//    @Test
//    void reorderStage_whenMovingUp_shouldCallUpdateStageOrderAbove() {
//        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
//        requestDTO.setId(STAGE_ID);
//        requestDTO.setOrder(1);
//
//        Stage stageOrder3 = mockStage.toBuilder().order(3).build();
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDb.findByStageId(STAGE_ID)).thenReturn(stageOrder3);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.reorderStage(PROJECT_ID, requestDTO);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDb).updateStageOrderAbove(PROJECT_ID, 0, 3);
//        verify(stageDb).save(stageOrder3);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // reorderStage — pindah ke bawah (newOrder > currentOrder)
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service: updateStageOrderBelow(projectId, newOrder, currentOrder + 1).
//     * Dengan newOrder = 4 dan currentOrder = 2 → updateStageOrderBelow(id, 4, 3).
//     */
//    @Test
//    void reorderStage_whenMovingDown_shouldCallUpdateStageOrderBelow() {
//        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
//        requestDTO.setId(STAGE_ID);
//        requestDTO.setOrder(4);
//
//        Stage stageOrder2 = mockStage.toBuilder().order(2).build();
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDb.findByStageId(STAGE_ID)).thenReturn(stageOrder2);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.reorderStage(PROJECT_ID, requestDTO);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(stageDb).updateStageOrderBelow(PROJECT_ID, 4, 3);
//        verify(stageDb).save(stageOrder2);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // reorderStage — order sama → tidak ada perubahan urutan
//    // ════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void reorderStage_whenSameOrder_shouldNotCallUpdateMethods() {
//        ReorderRequestDTO requestDTO = new ReorderRequestDTO();
//        requestDTO.setId(STAGE_ID);
//        requestDTO.setOrder(1); // sama dengan mockStage.order
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDb.findByStageId(STAGE_ID)).thenReturn(mockStage);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(ResponseEntity.ok().build());
//
//        stageService.reorderStage(PROJECT_ID, requestDTO);
//
//        verify(stageDb, never()).updateStageOrderAbove(any(), any(), any());
//        verify(stageDb, never()).updateStageOrderBelow(any(), any(), any());
//        verify(stageDb).save(mockStage);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // deleteStageById — tanpa task
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service memanggil validateProjectCancellation sebelum menghapus.
//     * Urutan di service: validateManagerAccess → validateProjectCancellation →
//     * findByStageId → delete → updateStageOrderAfterDelete.
//     */
//    @Test
//    void deleteStageById_withNoTasks_shouldDeleteDirectlyAndUpdateOrder() {
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        // Dipanggil dua kali: sekali untuk ambil order, sekali di dalam stageDb.delete(stageDb.findByStageId(...))
//        when(stageDb.findByStageId(STAGE_ID)).thenReturn(mockStage);
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.deleteStageById(PROJECT_ID, STAGE_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(taskService, never()).deleteTaskById(any(), any());
//        verify(stageDb).delete(mockStage);
//        verify(stageDb).updateStageOrderAfterDelete(PROJECT_ID, 1);
//        verify(authService).validateProjectCancellation(mockProject);
//    }
//
//    // ════════════════════════════════════════════════════════════════════════
//    // deleteStageById — dengan task (cascade delete)
//    // ════════════════════════════════════════════════════════════════════════
//
//    /**
//     * Service mengambil stage dua kali via stageDb.findByStageId:
//     *  1. untuk mendapatkan order dan iterasi taskList
//     *  2. sebagai argumen stageDb.delete(stageDb.findByStageId(stageId))
//     * Keduanya perlu di-stub agar tidak melempar NullPointerException.
//     */
//    @Test
//    void deleteStageById_withTasks_shouldDeleteEachTaskThenStage() {
//        Task mockTask = Task.builder().taskId("task-001").build();
//        Stage stageWithTasks = mockStage.toBuilder().taskList(List.of(mockTask)).build();
//
//        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();
//
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
//        when(userDb.findByUsername(USERNAME)).thenReturn(mockUserPm);
//        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
//        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
//        doNothing().when(authService).validateProjectCancellation(mockProject);
//        when(stageDb.findByStageId(STAGE_ID))
//                .thenReturn(stageWithTasks)   // panggilan pertama: ambil stage + order
//                .thenReturn(stageWithTasks);  // panggilan kedua: argumen stageDb.delete(...)
//        doReturn(mockResponse).when(taskService).deleteTaskById(STAGE_ID, "task-001");
//        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
//                .thenReturn(mockResponse);
//
//        ResponseEntity<?> result = stageService.deleteStageById(PROJECT_ID, STAGE_ID);
//
//        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
//        verify(taskService).deleteTaskById(STAGE_ID, "task-001");
//        verify(stageDb).delete(stageWithTasks);
//        verify(stageDb).updateStageOrderAfterDelete(PROJECT_ID, 1);
//    }
//}