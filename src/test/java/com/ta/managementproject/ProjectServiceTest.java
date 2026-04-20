package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.*;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.exception.UnprocessableContentException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.project.ProjectServiceImpl;
import com.ta.managementproject.service.stage.StageService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectDb projectDb;
    @Mock private HttpServletRequest request;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthService authService;
    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private UserDb userDb;
    @Mock private StageService stageService;
    @Mock private UserService userService;
    @Mock private UtilService utilService;
    @Mock private ProjectDbWithDsl projectDbWithDsl;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static final String USERNAME = "user_test";
    private static final String PROJECT_ID = "project-001";

    private User mockUser;
    private Project mockProject;
    private ProjectManager mockPm;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .username(USERNAME)
                .build();

        mockPm = ProjectManager.builder()
                .username(USERNAME)
                .fullName("Test PM")
                .build();

        mockProject = Project.builder()
                .projectId(PROJECT_ID)
                .projectName("Test Project")
                .description("Test Description")
                .projectManager(mockPm)
                .status("NOT_STARTED")
                .startDate(Instant.now())
                .endDate(Instant.now().plusSeconds(86400))
                .stageList(List.of())
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getAllProject — as PROJECT_MANAGER
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void getAllProject_asProjectManager_shouldCallFindAllWithPmUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<ProjectResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MANAGER);
        when(projectDbWithDsl.findAll(eq(USERNAME), isNull(), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.getAllProject(pageable, null, null, null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDbWithDsl).findAll(eq(USERNAME), isNull(), any(), any(), any(), any(), any(), any(), eq(pageable));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getAllProject — as MEMBER
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void getAllProject_asMember_shouldCallFindAllWithMemberUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> mockPage = new PageImpl<>(List.of());
        ResponseEntity<BaseResponseDTO<Page<ProjectResponseDTO>>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MEMBER);
        when(projectDbWithDsl.findAll(isNull(), eq(USERNAME), any(), any(), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(mockPage);
        when(utilService.buildResponse(HttpStatus.OK, "SUCCESS", mockPage)).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.getAllProject(pageable, null, null, null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDbWithDsl).findAll(isNull(), eq(USERNAME), any(), any(), any(), any(), any(), any(), eq(pageable));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getAllProject — endDate before startDate throws BadRequestException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void getAllProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(1);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MANAGER);

        assertThatThrownBy(() -> projectService.getAllProject(pageable, startDate, endDate, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // addNewProject — success
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewProject_withValidRequest_shouldSaveAndReturnCreated() {
        CreateUpdateProjectRequestDTO requestDTO = new CreateUpdateProjectRequestDTO();
        requestDTO.setProjectName("New Project");
        requestDTO.setDescription("Desc");
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().plusDays(10));

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectManagerDb.findByUsername(USERNAME)).thenReturn(mockPm);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.addNewProject(requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(projectDb).save(any(Project.class));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // addNewProject — endDate before startDate throws BadRequestException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void addNewProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        CreateUpdateProjectRequestDTO requestDTO = new CreateUpdateProjectRequestDTO();
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> projectService.addNewProject(requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // updateProject — success
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void updateProject_withValidRequest_shouldSaveUpdatedProject() {
        CreateUpdateProjectRequestDTO requestDTO = new CreateUpdateProjectRequestDTO();
        requestDTO.setProjectName("Updated Project");
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().plusDays(5));

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.updateProject(PROJECT_ID, requestDTO);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDb).save(any(Project.class));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // updateProject — endDate before startDate throws BadRequestException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void updateProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        CreateUpdateProjectRequestDTO requestDTO = new CreateUpdateProjectRequestDTO();
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setEndDate(LocalDate.now().minusDays(1));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);

        assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, requestDTO))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getProjectDetail — success
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void getProjectDetail_withValidAccess_shouldReturnProjectDetail() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.getProjectDetail(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // deleteProjectById — success with no stages
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void deleteProjectById_withNoStages_shouldDeleteDirectly() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.deleteProjectById(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDb).delete(mockProject);
        verify(stageService, never()).deleteStageById(any(), any());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // deleteProjectById — success with stages (cascade delete)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void deleteProjectById_withStages_shouldDeleteEachStageThenProject() {
        Stage mockStage = Stage.builder().stageId("stage-001").build();
        Project projectWithStages = mockProject.toBuilder()
                .stageList(List.of(mockStage))
                .build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithStages);
        doNothing().when(authService).validateManagerAccess(projectWithStages, USERNAME);
        doReturn(mockResponse)
                .when(stageService)
                .deleteStageById(PROJECT_ID, "stage-001");
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.deleteProjectById(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService).deleteStageById(PROJECT_ID, "stage-001");
        verify(projectDb).delete(projectWithStages);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // generateJoinCode — create new code when joinCode is null
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void generateJoinCode_whenJoinCodeIsNull_shouldGenerateAndSave() {
        Project projectNoCode = mockProject.toBuilder()
                .joinCode(null)
                .joinCodeExpiredAt(null)
                .build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectNoCode);
        doNothing().when(authService).validateManagerAccess(projectNoCode, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.generateJoinCode(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(projectDb).save(projectNoCode);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // generateJoinCode — reuse existing valid code (not expired)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void generateJoinCode_whenJoinCodeStillValid_shouldNotRegenerateCode() {
        Project projectWithValidCode = mockProject.toBuilder()
                .joinCode("ABCD1234")
                .joinCodeExpiredAt(Instant.now().plusSeconds(3600))
                .build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithValidCode);
        doNothing().when(authService).validateManagerAccess(projectWithValidCode, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any())).thenReturn(mockResponse);

        projectService.generateJoinCode(PROJECT_ID);

        verify(projectDb, never()).save(any());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // generateJoinCode — regenerate when code is expired
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void generateJoinCode_whenJoinCodeExpired_shouldRegenerateAndSave() {
        Project projectExpiredCode = mockProject.toBuilder()
                .joinCode("EXPIRED1")
                .joinCodeExpiredAt(Instant.now().minusSeconds(100))
                .build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectExpiredCode);
        doNothing().when(authService).validateManagerAccess(projectExpiredCode, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any())).thenReturn(mockResponse);

        projectService.generateJoinCode(PROJECT_ID);

        verify(projectDb).save(projectExpiredCode);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // joinProject — success
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void joinProject_withValidCode_shouldSaveMemberAndReturnCreated() {
        String joinCode = "VALID123";
        ProjectMember mockMember = ProjectMember.builder().username(USERNAME).build();
        Project projectWithCode = mockProject.toBuilder()
                .joinCode(joinCode)
                .joinCodeExpiredAt(Instant.now().plusSeconds(3600))
                .build();

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.status(HttpStatus.CREATED).build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectMemberDb.findByUsername(USERNAME)).thenReturn(mockMember);
        when(projectDb.findByJoinCode(joinCode)).thenReturn(projectWithCode);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.joinProject(joinCode);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(memberInProjectDb).save(any(MemberInProject.class));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // joinProject — project not found throws NotFoundException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void joinProject_whenProjectNotFound_shouldThrowNotFoundException() {
        String joinCode = "NOTFOUND";

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectMemberDb.findByUsername(USERNAME)).thenReturn(new ProjectMember());
        when(projectDb.findByJoinCode(joinCode)).thenReturn(null);

        assertThatThrownBy(() -> projectService.joinProject(joinCode))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("PROJECT_NOT_FOUND!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // joinProject — expired join code throws UnprocessableContentException
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void joinProject_whenJoinCodeExpired_shouldThrowUnprocessableContentException() {
        String joinCode = "EXPIRED1";
        Project expiredProject = mockProject.toBuilder()
                .joinCode(joinCode)
                .joinCodeExpiredAt(Instant.now().minusSeconds(100))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectMemberDb.findByUsername(USERNAME)).thenReturn(new ProjectMember());
        when(projectDb.findByJoinCode(joinCode)).thenReturn(expiredProject);

        assertThatThrownBy(() -> projectService.joinProject(joinCode))
                .isInstanceOf(UnprocessableContentException.class)
                .hasMessage("Join code has beed expired!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getProjectStatistics — success with no stages
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    void getProjectStatistics_withNoStages_shouldReturnZeroProgress() {
        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.getProjectStatistics(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService, never()).getStageStatistics(any());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // getProjectStatistics — success with stages, aggregates totals
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @SuppressWarnings("unchecked")
    void getProjectStatistics_withStages_shouldAggregateStatisticsFromEachStage() {
        Stage mockStage = Stage.builder().stageId("stage-001").build();
        Project projectWithStages = mockProject.toBuilder()
                .stageList(List.of(mockStage))
                .build();

        ProgressResponseDTO stageProgress = ProgressResponseDTO.builder()
                .totalTask(10L)
                .finishedTask(5L)
                .inProgressTask(3L)
                .todoTask(2L)
                .build();

        BaseResponseDTO<ProgressResponseDTO> stageResponse = new BaseResponseDTO<>();
        stageResponse.setData(stageProgress);

        ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> stageResponseEntity =
                ResponseEntity.ok(stageResponse);

        ResponseEntity<BaseResponseDTO<Object>> mockResponse = ResponseEntity.ok().build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithStages);
        doNothing().when(authService).validateManagerAndMemberAccess(projectWithStages, USERNAME);
        when(stageService.getStageStatistics("stage-001")).thenReturn((ResponseEntity) stageResponseEntity);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any())).thenReturn(mockResponse);

        ResponseEntity<?> result = projectService.getProjectStatistics(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService).getStageStatistics("stage-001");
    }
}