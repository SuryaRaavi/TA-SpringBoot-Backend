//package com.ta.managementproject;
//
//import com.ta.managementproject.dto.BaseResponseDTO;
//import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
//import com.ta.managementproject.dto.request.ReorderRequestDTO;
//import com.ta.managementproject.dto.response.ProgressResponseDTO;
//import com.ta.managementproject.dto.response.StageResponseDTO;
//import com.ta.managementproject.entity.Project;
//import com.ta.managementproject.entity.ProjectManager;
//import com.ta.managementproject.entity.Stage;
//import com.ta.managementproject.entity.User;
//import com.ta.managementproject.enums.Role;
//import com.ta.managementproject.repository.ProjectDb;
//import com.ta.managementproject.repository.StageDb;
//import com.ta.managementproject.repository.TaskDb;
//import com.ta.managementproject.repository.UserDb;
//import com.ta.managementproject.security.util.JwtUtils;
//import com.ta.managementproject.service.stage.StageServiceImpl;
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
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class StageServiceTest {
//    @InjectMocks
//    private StageServiceImpl stageService;
//
//    @Mock
//    private StageDb stageDb;
//
//    @Mock
//    private UserDb userDb;
//
//    @Mock
//    private ProjectDb projectDb;
//
//    @Mock
//    private TaskDb taskDb;
//
//    @Mock
//    private JwtUtils jwtUtils;
//
//    @Mock
//    private HttpServletRequest request;
//
//    // ─── Shared fixtures ────────────────────────────────────────────────────────
//
//    private User pmUser;
//    private User memberUser;
//    private Project project;
//    private Stage stage;
//
//    @BeforeEach
//    void setUp() {
//        pmUser = new ProjectManager();
//        pmUser.setUsername("pm_user");
//
//        memberUser = new User();
//        memberUser.setUsername("member_user");
//
//        project = new Project();
//        project.setProjectId("project-1");
//        project.setProjectManager((ProjectManager) pmUser);
//        project.setMemberInProjectList(List.of());
//
//        stage = Stage.builder()
//                .stageId("stage-1")
//                .stageName("Stage One")
//                .description("Description")
//                .order(0)
//                .project(project)
//                .build();
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // getAllStage
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void getAllStage_asProjectManager_returnsOk() {
//        List<StageResponseDTO> mockStages = List.of(new StageResponseDTO());
//
//        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findAllByProjectIdAndUsernamePM("pm_user", "project-1")).thenReturn(mockStages);
//
//        ResponseEntity<?> response = stageService.getAllStage("project-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertNotNull(body);
//        assertEquals(HttpStatus.OK.value(), body.getStatus());
//        assertEquals("SUCCESS", body.getMessage());
//        assertEquals(mockStages, body.getData());
//
//        verify(stageDb).findAllByProjectIdAndUsernamePM("pm_user", "project-1");
//        verify(stageDb, never()).findAllByProjectIdAndUsernamePMB(any(), any());
//    }
//
//    @Test
//    void getAllStage_asMember_returnsOk() {
//        List<StageResponseDTO> mockStages = List.of(new StageResponseDTO());
//
//        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MEMBER);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("member_user");
//        when(stageDb.findAllByProjectIdAndUsernamePMB("member_user", "project-1")).thenReturn(mockStages);
//
//        ResponseEntity<?> response = stageService.getAllStage("project-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(stageDb).findAllByProjectIdAndUsernamePMB("member_user", "project-1");
//        verify(stageDb, never()).findAllByProjectIdAndUsernamePM(any(), any());
//    }
//
//    @Test
//    void getAllStage_whenExceptionThrown_returnsInternalServerError() {
//        when(jwtUtils.getUserRoleFromJwtToken(request)).thenThrow(new RuntimeException("JWT error"));
//
//        ResponseEntity<?> response = stageService.getAllStage("project-1");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertNotNull(body);
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
//        assertEquals("JWT error", body.getMessage());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // addNewStage
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void addNewStage_asPM_returnsOk() {
//        CreateUpdateStageRequestDTO dto = new CreateUpdateStageRequestDTO();
//        dto.setStageName("New Stage");
//        dto.setDescription("Desc");
//
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.getTotalStageByProject("project-1")).thenReturn(2);
//
//        ResponseEntity<?> response = stageService.addNewStage("project-1", dto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertNotNull(body);
//        assertEquals("SUCCESS", body.getMessage());
//
//        verify(stageDb).save(any(Stage.class));
//    }
//
//    @Test
//    void addNewStage_notPM_returnsForbidden() {
//        CreateUpdateStageRequestDTO dto = new CreateUpdateStageRequestDTO();
//
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
//
//        ResponseEntity<?> response = stageService.addNewStage("project-1", dto);
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(stageDb, never()).save(any());
//    }
//
//    @Test
//    void addNewStage_whenExceptionThrown_returnsInternalServerError() {
//        when(projectDb.findByProjectId("project-1")).thenThrow(new RuntimeException("DB error"));
//
//        ResponseEntity<?> response = stageService.addNewStage("project-1", new CreateUpdateStageRequestDTO());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // editStage
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void editStage_asPM_returnsOk() {
//        CreateUpdateStageRequestDTO dto = new CreateUpdateStageRequestDTO();
//        dto.setStageName("Updated Name");
//        dto.setDescription("Updated Desc");
//
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(userDb.findByUsername("pm_user")).thenReturn(pmUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//
//        ResponseEntity<?> response = stageService.editStage("stage-1", dto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertNotNull(body);
//        assertEquals("SUCCESS", body.getMessage());
//        verify(stageDb).save(any(Stage.class));
//    }
//
//    @Test
//    void editStage_withNullFields_keepsOriginalValues() {
//        CreateUpdateStageRequestDTO dto = new CreateUpdateStageRequestDTO();
//        // stageName and description are null — should keep original
//
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(userDb.findByUsername("pm_user")).thenReturn(pmUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//
//        ResponseEntity<?> response = stageService.editStage("stage-1", dto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(stageDb).save(argThat(s ->
//                s.getStageName().equals("Stage One") &&
//                        s.getDescription().equals("Description")
//        ));
//    }
//
//    @Test
//    void editStage_notPM_returnsForbidden() {
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(userDb.findByUsername("other_user")).thenReturn(memberUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
//
//        ResponseEntity<?> response = stageService.editStage("stage-1", new CreateUpdateStageRequestDTO());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(stageDb, never()).save(any());
//    }
//
//    @Test
//    void editStage_whenExceptionThrown_returnsInternalServerError() {
//        when(stageDb.findByStageId("stage-1")).thenThrow(new RuntimeException("Not found"));
//
//        ResponseEntity<?> response = stageService.editStage("stage-1", new CreateUpdateStageRequestDTO());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // getStageStatistics
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void getStageStatistics_asPM_returnsCorrectProgress() {
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(taskDb.getTotalFinishedTask("stage-1")).thenReturn(3);
//        when(taskDb.getTotalTask("stage-1")).thenReturn(5);
//        when(taskDb.getTotalToDoTask("stage-1")).thenReturn(1);
//        when(taskDb.getTotalInProgressTask("stage-1")).thenReturn(1);
//
//        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> response = stageService.getStageStatistics("stage-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        ProgressResponseDTO data = response.getBody().getData();
//        assertNotNull(data);
//        assertEquals(5, data.getTotalTask());
//        assertEquals(3, data.getFinishedTask());
//        assertEquals(1, data.getTodoTask());
//        assertEquals(1, data.getInProgressTask());
//        assertEquals(60.0, data.getProgress(), 0.001);
//    }
//
//    @Test
//    void getStageStatistics_withZeroTasks_returnsZeroProgress() {
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(taskDb.getTotalFinishedTask("stage-1")).thenReturn(0);
//        when(taskDb.getTotalTask("stage-1")).thenReturn(0);
//        when(taskDb.getTotalToDoTask("stage-1")).thenReturn(0);
//        when(taskDb.getTotalInProgressTask("stage-1")).thenReturn(0);
//
//        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> response = stageService.getStageStatistics("stage-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(0.0, response.getBody().getData().getProgress(), 0.001);
//    }
//
//    @Test
//    void getStageStatistics_notPMOrMember_returnsForbidden() {
//        User stranger = new User();
//        stranger.setUsername("stranger");
//
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("stranger");
//
//        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> response = stageService.getStageStatistics("stage-1");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//    }
//
//    @Test
//    void getStageStatistics_whenExceptionThrown_returnsInternalServerError() {
//        when(stageDb.findByStageId("stage-1")).thenThrow(new RuntimeException("Stage not found"));
//
//        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> response = stageService.getStageStatistics("stage-1");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // reorderStage
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void reorderStage_asPM_returnsOk() {
//        ReorderRequestDTO r1 = new ReorderRequestDTO("stage-1", 1);
//        ReorderRequestDTO r2 = new ReorderRequestDTO("stage-2", 0);
//
//        Stage stage2 = Stage.builder().stageId("stage-2").order(1).project(project).build();
//
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(userDb.findByUsername("pm_user")).thenReturn(pmUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//        when(stageDb.findByStageId("stage-2")).thenReturn(stage2);
//
//        ResponseEntity<?> response = stageService.reorderStage("project-1", List.of(r1, r2));
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(stageDb).saveAll(anyList());
//        assertEquals(1, stage.getOrder());
//        assertEquals(0, stage2.getOrder());
//    }
//
//    @Test
//    void reorderStage_notPM_returnsForbidden() {
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(userDb.findByUsername("other_user")).thenReturn(memberUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
//
//        ResponseEntity<?> response = stageService.reorderStage("project-1", List.of());
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(stageDb, never()).saveAll(any());
//    }
//
//    @Test
//    void reorderStage_whenExceptionThrown_returnsInternalServerError() {
//        when(projectDb.findByProjectId("project-1")).thenThrow(new RuntimeException("DB error"));
//
//        ResponseEntity<?> response = stageService.reorderStage("project-1", List.of());
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//
//    // ══════════════════════════════════════════════════════════════════════════════
//    // deleteStageById
//    // ══════════════════════════════════════════════════════════════════════════════
//
//    @Test
//    void deleteStageById_asPM_returnsOk() {
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(userDb.findByUsername("pm_user")).thenReturn(pmUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("pm_user");
//        when(stageDb.findByStageId("stage-1")).thenReturn(stage);
//
//        ResponseEntity<?> response = stageService.deleteStageById("project-1", "stage-1");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        BaseResponseDTO<?> body = (BaseResponseDTO<?>) response.getBody();
//        assertNotNull(body);
//        assertEquals("SUCCESS", body.getMessage());
//        verify(stageDb).delete(stage);
//    }
//
//    @Test
//    void deleteStageById_notPM_returnsForbidden() {
//        when(projectDb.findByProjectId("project-1")).thenReturn(project);
//        when(userDb.findByUsername("other_user")).thenReturn(memberUser);
//        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
//
//        ResponseEntity<?> response = stageService.deleteStageById("project-1", "stage-1");
//
//        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
//        verify(stageDb, never()).delete(any());
//    }
//
//    @Test
//    void deleteStageById_whenExceptionThrown_returnsInternalServerError() {
//        when(projectDb.findByProjectId("project-1")).thenThrow(new RuntimeException("DB error"));
//
//        ResponseEntity<?> response = stageService.deleteStageById("project-1", "stage-1");
//
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
//    }
//}
