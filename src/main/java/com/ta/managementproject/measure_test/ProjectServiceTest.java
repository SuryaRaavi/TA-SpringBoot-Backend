package com.ta.managementproject.measure_test;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ConflictException;
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

    private static final String USERNAME   = "user_test";
    private static final String PROJECT_ID = "project-001";

    private User           mockUser;
    private Project        mockProject;
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

        // isCancelled default false — digunakan di semua happy-path updateProject
        mockProject = Project.builder()
                .projectId(PROJECT_ID)
                .projectName("Test Project")
                .description("Test Description")
                .projectManager(mockPm)
                .startDate(Instant.now())
                .endDate(Instant.now().plusSeconds(86400))
                .stageList(List.of())
                .isCancelled(false)
                .build();
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getAllProject
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getAllProject_asProjectManager_shouldCallFindAllWithPmUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> mockPage = new PageImpl<>(List.of());

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MANAGER);
        when(projectDbWithDsl.findAll(
                eq(USERNAME), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable))
        ).thenReturn(mockPage);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.getAllProject(
                pageable, null, null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDbWithDsl).findAll(
                eq(USERNAME), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable));
    }

    @Test
    void getAllProject_asMember_shouldCallFindAllWithMemberUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> mockPage = new PageImpl<>(List.of());

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MEMBER);
        when(projectDbWithDsl.findAll(
                isNull(), eq(USERNAME),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable))
        ).thenReturn(mockPage);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.getAllProject(
                pageable, null, null, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDbWithDsl).findAll(
                isNull(), eq(USERNAME),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(pageable));
    }

    @Test
    void getAllProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = startDate.minusDays(1);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userService.getUserRoleByUsername(USERNAME)).thenReturn(Role.PROJECT_MANAGER);

        assertThatThrownBy(() ->
                projectService.getAllProject(pageable, startDate, endDate, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
    }

    /* ══════════════════════════════════════════════════════════════════════
     * addNewProject
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void addNewProject_withValidRequest_shouldSaveAndReturnCreated() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("New Project");
        dto.setDescription("Desc");
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(10));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectManagerDb.findByUsername(USERNAME)).thenReturn(mockPm);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        ResponseEntity<?> result = projectService.addNewProject(dto);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(projectDb).save(any(Project.class));
    }

    @Test
    void addNewProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> projectService.addNewProject(dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
    }

    /* ══════════════════════════════════════════════════════════════════════
     * updateProject
     * ══════════════════════════════════════════════════════════════════════
     *
     * Alur di service:
     *   1. validateProject(projectId)
     *   2. validateManagerAccess(project, username)
     *   3. if (project.isCancelled()) throw ConflictException   ← pengecekan LANGSUNG
     *   4. validasi tanggal
     *   5. projectDb.save(...)
     *
     * CATATAN PENTING:
     *   updateProject() TIDAK memanggil authService.validateProjectCancellation().
     *   Pengecekan cancelled dilakukan secara manual lewat project.isCancelled().
     *   Oleh karena itu TIDAK ADA stub untuk validateProjectCancellation di sini.
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void updateProject_withValidRequest_shouldSaveUpdatedProject() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("Updated Project");
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(5));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        // mockProject.isCancelled() == false → tidak melempar exception
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.updateProject(PROJECT_ID, dto);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDb).save(any(Project.class));
        // Pastikan validateProjectCancellation (authService) TIDAK dipanggil
        verify(authService, never()).validateProjectCancellation(any());
    }

    @Test
    void updateProject_whenProjectIsCancelled_shouldThrowConflictException() {
        // Project dengan isCancelled = true
        Project cancelledProject = mockProject.toBuilder()
                .isCancelled(true)
                .build();

        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(5));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(cancelledProject);
        doNothing().when(authService).validateManagerAccess(cancelledProject, USERNAME);

        assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Update project is not allowed, project status is cancelled!");

        // projectDb.save tidak boleh dipanggil saat project cancelled
        verify(projectDb, never()).save(any());
    }

    @Test
    void updateProject_whenEndDateBeforeStartDate_shouldThrowBadRequestException() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().minusDays(1));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        // isCancelled = false → lolos pengecekan cancelled, baru validasi tanggal

        assertThatThrownBy(() -> projectService.updateProject(PROJECT_ID, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");

        verify(projectDb, never()).save(any());
    }

    @Test
    void updateProject_withNullFields_shouldKeepOriginalValues() {
        // Semua field null → service pertahankan nilai lama dari project
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName(null);
        dto.setDescription(null);
        // startDate & endDate harus di-set agar validasi tanggal tidak NPE;
        // gunakan nilai yang sama agar tidak melempar exception
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(1));
        dto.setIsCancelled(null);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        projectService.updateProject(PROJECT_ID, dto);

        verify(projectDb).save(argThat(p ->
                "Test Project".equals(p.getProjectName()) &&
                        "Test Description".equals(p.getDescription()) &&
                        !p.isCancelled()
        ));
    }

    /* ══════════════════════════════════════════════════════════════════════
     * getProjectDetail
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void getProjectDetail_withValidAccess_shouldReturnProjectDetail() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.getProjectDetail(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService, atLeast(1)).validateManagerAndMemberAccess(mockProject, USERNAME);
    }

    @Test
    void getProjectDetail_withNoStages_shouldNotCallStageService() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        projectService.getProjectDetail(PROJECT_ID);

        verify(stageService, never()).getStageStatistics(any());
    }

    @Test
    void getProjectDetail_withStages_shouldAggregateStatisticsFromEachStage() {
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

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithStages);
        doNothing().when(authService).validateManagerAndMemberAccess(projectWithStages, USERNAME);
        when(stageService.getStageStatistics("stage-001")).thenReturn(stageProgress);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.getProjectDetail(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService).getStageStatistics("stage-001");
    }

    /* ══════════════════════════════════════════════════════════════════════
     * deleteProjectById
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void deleteProjectById_withNoStages_shouldDeleteDirectly() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(mockProject, USERNAME);
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.deleteProjectById(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(projectDb).delete(mockProject);
        verify(stageService, never()).deleteStageById(any(), any());
    }

    @Test
    void deleteProjectById_withStages_shouldDeleteEachStageThenProject() {
        Stage mockStage = Stage.builder().stageId("stage-001").build();
        Project projectWithStages = mockProject.toBuilder()
                .stageList(List.of(mockStage))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(userDb.findByUsername(USERNAME)).thenReturn(mockUser);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithStages);
        doNothing().when(authService).validateManagerAccess(projectWithStages, USERNAME);
        doReturn(ResponseEntity.ok().build())
                .when(stageService).deleteStageById(PROJECT_ID, "stage-001");
        when(utilService.buildResponse(eq(HttpStatus.OK), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.ok().build());

        ResponseEntity<?> result = projectService.deleteProjectById(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(stageService).deleteStageById(PROJECT_ID, "stage-001");
        verify(projectDb).delete(projectWithStages);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * generateJoinCode
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void generateJoinCode_whenJoinCodeIsNull_shouldGenerateAndSave() {
        Project projectNoCode = mockProject.toBuilder()
                .joinCode(null)
                .joinCodeExpiredAt(null)
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectNoCode);
        doNothing().when(authService).validateManagerAccess(projectNoCode, USERNAME);
        doNothing().when(authService).validateProjectCancellation(projectNoCode);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        ResponseEntity<?> result = projectService.generateJoinCode(PROJECT_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(projectDb).save(projectNoCode);
    }

    @Test
    void generateJoinCode_whenJoinCodeStillValid_shouldNotRegenerateCode() {
        Project projectWithValidCode = mockProject.toBuilder()
                .joinCode("ABCD1234")
                .joinCodeExpiredAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectWithValidCode);
        doNothing().when(authService).validateManagerAccess(projectWithValidCode, USERNAME);
        doNothing().when(authService).validateProjectCancellation(projectWithValidCode);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        projectService.generateJoinCode(PROJECT_ID);

        verify(projectDb, never()).save(any());
    }

    @Test
    void generateJoinCode_whenJoinCodeExpired_shouldRegenerateAndSave() {
        Project projectExpiredCode = mockProject.toBuilder()
                .joinCode("EXPIRED1")
                .joinCodeExpiredAt(Instant.now().minusSeconds(100))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(authService.validateProject(PROJECT_ID)).thenReturn(projectExpiredCode);
        doNothing().when(authService).validateManagerAccess(projectExpiredCode, USERNAME);
        doNothing().when(authService).validateProjectCancellation(projectExpiredCode);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        projectService.generateJoinCode(PROJECT_ID);

        verify(projectDb).save(projectExpiredCode);
    }

    /* ══════════════════════════════════════════════════════════════════════
     * joinProject
     * ══════════════════════════════════════════════════════════════════════ */

    @Test
    void joinProject_withValidCode_shouldSaveMemberAndReturnCreated() {
        String joinCode = "VALID123";
        ProjectMember mockMember = ProjectMember.builder().username(USERNAME).build();
        Project projectWithCode = mockProject.toBuilder()
                .joinCode(joinCode)
                .joinCodeExpiredAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME);
        when(projectMemberDb.findByUsername(USERNAME)).thenReturn(mockMember);
        when(projectDb.findByJoinCode(joinCode)).thenReturn(projectWithCode);
        doNothing().when(authService).validateProjectCancellation(projectWithCode);
        when(utilService.buildResponse(eq(HttpStatus.CREATED), eq("SUCCESS"), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).build());

        ResponseEntity<?> result = projectService.joinProject(joinCode);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(memberInProjectDb).save(any(MemberInProject.class));
    }

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
        doNothing().when(authService).validateProjectCancellation(expiredProject);

        assertThatThrownBy(() -> projectService.joinProject(joinCode))
                .isInstanceOf(UnprocessableContentException.class)
                .hasMessage("Join code has beed expired!");
    }
}