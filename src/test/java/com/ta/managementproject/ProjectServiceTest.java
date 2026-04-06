package com.ta.managementproject;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.*;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.project.ProjectService;
import com.ta.managementproject.service.project.ProjectServiceImpl;
import com.ta.managementproject.service.stage.StageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {
    @Mock private ProjectDb projectDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private UserDb userDb;
    @Mock private UserInProjectDb userInProjectDb;
    @Mock private HttpServletRequest request;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthService authService;
    @Mock private StageService stageService;

    @InjectMocks
    private ProjectServiceImpl projectService;

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final String USERNAME_PM  = "pm_user";
    private static final String USERNAME_PMB = "member_user";
    private static final String SEARCH_PARAM = "project alpha";
    private static final String PROJECT_ID   = "proj-001";
    private static final String JOIN_CODE    = "ABCD1234";

    private static final String SORT_CREATED_AT   = "createdAt";
    private static final String SORT_PROJECT_NAME = "projectName";
    private static final String SORT_STATUS       = "status";

    private static final String ORDER_ASC  = "ascending";
    private static final String ORDER_DESC = "descending";

    private static final Instant START_DATE = Instant.parse("2024-01-01T00:00:00Z");
    private static final Instant END_DATE   = Instant.parse("2024-12-31T23:59:59Z");

    // ─── Shared Fixtures ──────────────────────────────────────────────────────

    private Page<ProjectResponseDTO> dummyPage;
    private ProjectManager dummyPm;
    private Project dummyProject;
    private User dummyUser;

    @BeforeEach
    void setUp() {
        dummyPage = new PageImpl<>(List.of(new ProjectResponseDTO()));

        dummyPm = new ProjectManager();
        dummyPm.setUsername(USERNAME_PM);
        dummyPm.setFullName("PM Full Name");

        dummyUser = new User();
        dummyUser.setUsername(USERNAME_PM);

        dummyProject = new Project();
        dummyProject.setProjectId(PROJECT_ID);
        dummyProject.setProjectName("Project Alpha");
        dummyProject.setDescription("Description");
        dummyProject.setProjectManager(dummyPm);
        dummyProject.setStatus("NOT_STARTED");
        dummyProject.setStartDate(START_DATE);
        dummyProject.setEndDate(END_DATE);
        dummyProject.setCreatedAt(Instant.now());
        dummyProject.setMemberInProjectList(List.of());
        dummyProject.setStageList(List.of());
    }

    @Test
    void getAllProject_pmNoDateFilter_returns200(){
        when(jwtUtils.getUserRoleFromJwtToken(request)).
                thenReturn(Role.PROJECT_MANAGER).
                thenReturn(Role.PROJECT_MEMBER);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);

        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, null, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals(1, body.getData().getTotalElements());
        verify(projectDb).findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class));
        verifyNoInteractions(memberInProjectDb);
    }

    @Test
    void getAllProject_pmWithDateFilter_returns200(){

        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManagerAndStartEndDate(
                eq(USERNAME_PM), eq(START_DATE), eq(END_DATE), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, START_DATE, END_DATE, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        verify(projectDb).findAllByProjectManagerAndStartEndDate(
                eq(USERNAME_PM), eq(START_DATE), eq(END_DATE), any(Pageable.class));
    }

    @Test
    void getAllProject_memberNoDateFilter_returns200(){
        when(jwtUtils.getUserRoleFromJwtToken(request)).
                thenReturn(Role.PROJECT_MEMBER).
                thenReturn(Role.PROJECT_MANAGER);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);

        when(memberInProjectDb.findByProjectMember(eq(USERNAME_PMB), any(Pageable.class))).thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, null, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals("SUCCESS", body.getMessage());
        verify(memberInProjectDb).findByProjectMember(eq(USERNAME_PMB), any(Pageable.class));
        verifyNoInteractions(projectDb);
    }

    @Test
    void getAllProject_memberWithDateFilter_returns200() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MEMBER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(memberInProjectDb.findAllByProjectMemberAndStartEndProyek(
                eq(USERNAME_PMB), eq(START_DATE), eq(END_DATE), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, START_DATE, END_DATE, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        verify(memberInProjectDb).findAllByProjectMemberAndStartEndProyek(
                eq(USERNAME_PMB), eq(START_DATE), eq(END_DATE), any(Pageable.class));
    }

    @Test
    void getAllProject_startDateOnlyProvided_returns400() {
        ResponseEntity<?> response = projectService.getAllProject(0, 10, START_DATE, null, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Tanggal mulai dan tanggal selesai harus terisi!", body.getMessage());
        verifyNoInteractions(projectDb, memberInProjectDb);
    }

    @Test
    void getAllProject_endDateOnlyProvided_returns400() {
        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, END_DATE, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Tanggal mulai dan tanggal selesai harus terisi!", body.getMessage());
    }

    @Test
    void getAllProject_endDateBeforeStartDate_returns400() {
        Instant start = LocalDateTime.of(
                2024, 6, 1, 0, 0
        ).toInstant(ZoneOffset.UTC);

        Instant end = LocalDateTime.of(
                2024, 1, 1, 0, 0
        ).toInstant(ZoneOffset.UTC);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, start, end, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Tanggal mulai tidak boleh lebih dari tanggal selesai!", body.getMessage());
    }

    @Test
    void getAllProject_unexpectedException_returns500() {
        when(jwtUtils.getUserRoleFromJwtToken(request))
                .thenThrow(new RuntimeException("DB connection lost"));

        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, null, null, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("DB connection lost", body.getMessage());
    }

    @Test
    void getAllProject_invalidSortingColumn_returns400() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);

        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, null, "invalidColumn", ORDER_DESC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Sorting column is not valid!", body.getMessage());
        verifyNoInteractions(projectDb, memberInProjectDb);
    }

    @Test
    void getAllProject_sortAscending_pageableHasAscendingSort() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.getAllProject(0, 10, null, null, SORT_CREATED_AT, ORDER_ASC);

        verify(projectDb).findAllByProjectManager(
                eq(USERNAME_PM),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_CREATED_AT);
                    return order != null && order.isAscending();
                })
        );
    }

    @Test
    void getAllProject_sortDescending_pageableHasDescendingSort() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.getAllProject(0, 10, null, null, SORT_CREATED_AT, ORDER_DESC);

        verify(projectDb).findAllByProjectManager(
                eq(USERNAME_PM),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_CREATED_AT);
                    return order != null && order.isDescending();
                })
        );
    }

    @Test
    void getAllProject_sortByProjectName_pageableUsesProjectNameColumn() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.getAllProject(0, 10, null, null, SORT_PROJECT_NAME, ORDER_ASC);

        verify(projectDb).findAllByProjectManager(
                eq(USERNAME_PM),
                argThat(p -> p.getSort().getOrderFor(SORT_PROJECT_NAME) != null)
        );
    }

    @Test
    void getAllProject_sortByStatus_pageableUsesStatusColumn() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.getAllProject(0, 10, null, null, SORT_STATUS, ORDER_ASC);

        verify(projectDb).findAllByProjectManager(
                eq(USERNAME_PM),
                argThat(p -> p.getSort().getOrderFor(SORT_STATUS) != null)
        );
    }

    @Test
    void getAllProject_pmEmptyResult_returns200WithEmptyPage() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<?> response = projectService.getAllProject(0, 10, null, null, SORT_CREATED_AT, ORDER_DESC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(0, body.getData().getTotalElements());
    }

    @Test
    void getAllProject_paginationParamsForwardedCorrectly() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findAllByProjectManager(eq(USERNAME_PM), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.getAllProject(2, 5, null, null, SORT_CREATED_AT, ORDER_DESC);

        verify(projectDb).findAllByProjectManager(
                eq(USERNAME_PM),
                argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 5)
        );
    }

    @Test
    void searchProject_pmValidQuery_returns200() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), eq(SEARCH_PARAM), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.searchProject(0, 10, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals(1, body.getData().getTotalElements());
        verify(projectDb).findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), eq(SEARCH_PARAM), any(Pageable.class));
        verifyNoInteractions(memberInProjectDb);
    }

    @Test
    void searchProject_memberValidQuery_returns200() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MEMBER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(memberInProjectDb.findPMBProjectByProjectNameAndProjectId(
                eq(USERNAME_PMB), eq(SEARCH_PARAM), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.searchProject(0, 10, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        verify(memberInProjectDb).findPMBProjectByProjectNameAndProjectId(
                eq(USERNAME_PMB), eq(SEARCH_PARAM), any(Pageable.class));
        verifyNoInteractions(projectDb);
    }

    @Test
    void searchProject_pmNoMatchingProject_returns200WithEmptyPage() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<?> response = projectService.searchProject(0, 10, "nonexistent", SORT_CREATED_AT, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(0, body.getData().getTotalElements());
    }

    @Test
    void searchProject_pmBlankQuery_returns200() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), eq(""), any(Pageable.class)))
                .thenReturn(dummyPage);

        ResponseEntity<?> response = projectService.searchProject(0, 10, "", SORT_CREATED_AT, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(projectDb).findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), eq(""), any(Pageable.class));
    }

    @Test
    void searchProject_paginationParamsForwardedCorrectly() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM), anyString(), any(Pageable.class)))
                .thenReturn(dummyPage);

        projectService.searchProject(3, 15, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        verify(projectDb).findPMProjectByProjectNameOrProjectId(
                eq(USERNAME_PM),
                eq(SEARCH_PARAM),
                argThat(p -> p.getPageNumber() == 3 && p.getPageSize() == 15)
        );
    }

    @Test
    void searchProject_unexpectedException_returns500() {
        when(jwtUtils.getUserRoleFromJwtToken(request))
                .thenThrow(new RuntimeException("JWT parse error"));

        ResponseEntity<?> response = projectService.searchProject(0, 10, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<ProjectResponseDTO>> body =
                (BaseResponseDTO<Page<ProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("JWT parse error", body.getMessage());
    }

    @Test
    void searchProject_pmRoleNeverCallsMemberDb() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findPMProjectByProjectNameOrProjectId(anyString(), anyString(), any()))
                .thenReturn(dummyPage);

        projectService.searchProject(0, 10, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        verifyNoInteractions(memberInProjectDb);
    }

    @Test
    void searchProject_memberRoleNeverCallsProjectDb() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MEMBER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(memberInProjectDb.findPMBProjectByProjectNameAndProjectId(anyString(), anyString(), any()))
                .thenReturn(dummyPage);

        projectService.searchProject(0, 10, SEARCH_PARAM, SORT_CREATED_AT, ORDER_ASC);

        verifyNoInteractions(projectDb);
    }

    private CreateUpdateProjectRequestDTO buildValidCreateRequest() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("New Project");
        dto.setDescription("New Description");
        dto.setStartDate(START_DATE);
        dto.setEndDate(END_DATE);
        return dto;
    }

    @Test
    void addNewProject_validRequest_returns201() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectManagerDb.findByUsername(USERNAME_PM)).thenReturn(dummyPm);
        when(projectDb.save(any(Project.class))).thenReturn(dummyProject);
        when(projectDb.findByProjectId(any())).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.addNewProject(buildValidCreateRequest());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.CREATED.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals("SUCCESS", body.getData().getMessage());
        assertEquals("The project has been CREATED!", body.getData().getMessageDetail());
    }

    @Test
    void addNewProject_endDateBeforeStartDate_returns400() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("New Project");
        dto.setDescription("Desc");
        dto.setStartDate(END_DATE);   // end lebih besar dari start — dibalik agar trigger validasi
        dto.setEndDate(START_DATE);

        ResponseEntity<?> response = projectService.addNewProject(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Tanggal mulai tidak boleh lebih dari tanggal selesai!", body.getMessage());
        verifyNoInteractions(projectDb, projectManagerDb);
    }

    @Test
    void addNewProject_projectSavedAndUserInProjectCreated() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectManagerDb.findByUsername(USERNAME_PM)).thenReturn(dummyPm);
        when(projectDb.save(any(Project.class))).thenReturn(dummyProject);
        when(projectDb.findByProjectId(any())).thenReturn(dummyProject);

        projectService.addNewProject(buildValidCreateRequest());

        verify(projectDb).save(any(Project.class));

        verify(userInProjectDb).save(any(UserInProject.class));
    }

    @Test
    void addNewProject_statusDefaultNotStarted() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectManagerDb.findByUsername(USERNAME_PM)).thenReturn(dummyPm);
        when(projectDb.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            // Verifikasi status default sebelum dikembalikan
            assertEquals("NOT_STARTED", saved.getStatus());
            return dummyProject;
        });
        when(projectDb.findByProjectId(any())).thenReturn(dummyProject);

        projectService.addNewProject(buildValidCreateRequest());
    }

    @Test
    void addNewProject_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<?> response = projectService.addNewProject(buildValidCreateRequest());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("FAILED", body.getData().getMessage());
    }

    // =========================================================================
    // updateProject
    // =========================================================================

    private CreateUpdateProjectRequestDTO buildValidUpdateRequest() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("Updated Project");
        dto.setDescription("Updated Desc");
        dto.setStartDate(START_DATE);
        dto.setEndDate(END_DATE);
        dto.setStatus("IN_PROGRESS");
        return dto;
    }

    @Test
    void updateProject_pmOwner_returns200() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(userDb.findByUsername(USERNAME_PM)).thenReturn(dummyUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.save(any(Project.class))).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.updateProject(PROJECT_ID, buildValidUpdateRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals("The project has been UPDATED!", body.getData().getMessageDetail());
        verify(projectDb).save(any(Project.class));
    }

    @Test
    void updateProject_notOwner_returns403() {
        User otherUser = new User();
        otherUser.setUsername("other_user");

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
        when(userDb.findByUsername("other_user")).thenReturn(otherUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.updateProject(PROJECT_ID, buildValidUpdateRequest());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
        verify(projectDb, never()).save(any(Project.class));
    }

    @Test
    void updateProject_endDateBeforeStartDate_returns400() {
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName("Updated");
        dto.setDescription("Desc");
        dto.setStartDate(END_DATE);   // dibalik agar trigger validasi
        dto.setEndDate(START_DATE);
        dto.setStatus("IN_PROGRESS");

        ResponseEntity<?> response = projectService.updateProject(PROJECT_ID, dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals("Tanggal mulai tidak boleh lebih dari tanggal selesai!", body.getMessage());
        verifyNoInteractions(projectDb);
    }

    @Test
    void updateProject_nullFieldsKeepExistingValues() {
        // DTO hanya mengisi sebagian field (sisanya null)
        CreateUpdateProjectRequestDTO dto = new CreateUpdateProjectRequestDTO();
        dto.setProjectName(null);
        dto.setDescription(null);
        dto.setStartDate(START_DATE);
        dto.setEndDate(END_DATE);
        dto.setStatus(null);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(userDb.findByUsername(USERNAME_PM)).thenReturn(dummyUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            // Field null seharusnya fallback ke nilai existing
            assertEquals(dummyProject.getProjectName(), saved.getProjectName());
            assertEquals(dummyProject.getDescription(), saved.getDescription());
            assertEquals(dummyProject.getStatus(), saved.getStatus());
            return saved;
        });

        ResponseEntity<?> response = projectService.updateProject(PROJECT_ID, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateProject_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = projectService.updateProject(PROJECT_ID, buildValidUpdateRequest());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("FAILED", body.getData().getMessage());
    }

    // =========================================================================
    // deleteProjectById
    // =========================================================================

    @Test
    void deleteProjectById_pmOwner_returns200() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(userDb.findByUsername(USERNAME_PM)).thenReturn(dummyUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.deleteProjectById(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals("All selected project has been DELETED.", body.getData().getMessageDetail());
        verify(projectDb).delete(dummyProject);
    }

    @Test
    void deleteProjectById_notProjectManagerRole_returns403() {
        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MEMBER);

        ResponseEntity<?> response = projectService.deleteProjectById(PROJECT_ID);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
        verify(projectDb, never()).delete(any(Project.class));
    }

    @Test
    void deleteProjectById_pmNotOwner_projectNotDeleted() {
        // PM login tapi bukan owner project
        User otherPm = new User();
        otherPm.setUsername("other_pm");

        Project projectOwnedByOther = new Project();
        projectOwnedByOther.setProjectId(PROJECT_ID);
        ProjectManager otherManager = new ProjectManager();
        otherManager.setUsername("original_pm");
        projectOwnedByOther.setProjectManager(otherManager);

        when(jwtUtils.getUserRoleFromJwtToken(request)).thenReturn(Role.PROJECT_MANAGER);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_pm");
        when(userDb.findByUsername("other_pm")).thenReturn(otherPm);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(projectOwnedByOther);

        ResponseEntity<?> response = projectService.deleteProjectById(PROJECT_ID);

        // Seharusnya tetap 200 (tidak forbidden), tapi delete tidak dipanggil karena bukan owner
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(projectDb, never()).delete(any(Project.class));
    }

    @Test
    void deleteProjectById_unexpectedException_returns500() {
        when(jwtUtils.getUserRoleFromJwtToken(request))
                .thenThrow(new RuntimeException("Service unavailable"));

        ResponseEntity<?> response = projectService.deleteProjectById(PROJECT_ID);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("FAILED", body.getData().getMessage());
    }

    // =========================================================================
    // getProjectDetail
    // =========================================================================

    @Test
    void getProjectDetail_pmOwner_returns200WithCorrectData() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(userDb.findByUsername(USERNAME_PM)).thenReturn(dummyUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.getProjectDetail(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProjectDetailResponseDTO> body =
                (BaseResponseDTO<ProjectDetailResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());

        ProjectDetailResponseDTO data = body.getData();
        assertNotNull(data);
        assertEquals(PROJECT_ID, data.getProjectId());
        assertEquals(dummyProject.getProjectName(), data.getProjectName());
        assertEquals(dummyProject.getDescription(), data.getDescription());
        assertEquals(dummyPm.getFullName(), data.getFullNamePm());
        assertEquals(dummyProject.getStatus(), data.getStatus());
    }

    @Test
    void getProjectDetail_memberInProject_returns200() {
        // Setup member sebagai anggota project
        ProjectMember member = new ProjectMember();
        member.setUsername(USERNAME_PMB);

        MemberInProject mip = new MemberInProject();
        mip.setProjectMember(member);

        Project projectWithMember = new Project();
        projectWithMember.setProjectId(PROJECT_ID);
        projectWithMember.setProjectName("Project Alpha");
        projectWithMember.setDescription("Desc");
        projectWithMember.setStatus("IN_PROGRESS");
        projectWithMember.setStartDate(START_DATE);
        projectWithMember.setEndDate(END_DATE);
        projectWithMember.setProjectManager(dummyPm);
        projectWithMember.setMemberInProjectList(List.of(mip));

        User memberUser = new User();
        memberUser.setUsername(USERNAME_PMB);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(userDb.findByUsername(USERNAME_PMB)).thenReturn(memberUser);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(projectWithMember);

        ResponseEntity<?> response = projectService.getProjectDetail(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProjectDetailResponseDTO> body =
                (BaseResponseDTO<ProjectDetailResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals("SUCCESS", body.getMessage());
    }

    @Test
    void getProjectDetail_userNotInProject_returns403() {
        User outsider = new User();
        outsider.setUsername("outsider");

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("outsider");
        when(userDb.findByUsername("outsider")).thenReturn(outsider);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject); // memberList kosong

        ResponseEntity<?> response = projectService.getProjectDetail(PROJECT_ID);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProjectDetailResponseDTO> body =
                (BaseResponseDTO<ProjectDetailResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
    }

    @Test
    void getProjectDetail_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<?> response = projectService.getProjectDetail(PROJECT_ID);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProjectDetailResponseDTO> body =
                (BaseResponseDTO<ProjectDetailResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("JWT error", body.getMessage());
    }

    // =========================================================================
    // generateJoinCode
    // =========================================================================

    @Test
    void generateJoinCode_pmOwnerNoExistingCode_generatesNewCode() {
        // Tidak ada join code sebelumnya (null)
        dummyProject.setJoinCode(null);
        dummyProject.setJoinCodeExpiredAt(null);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.save(any(Project.class))).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.generateJoinCode(PROJECT_ID);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<String> body = (BaseResponseDTO<String>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.CREATED.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertNotNull(body.getData());
        verify(projectDb).save(dummyProject);
    }

    @Test
    void generateJoinCode_existingCodeStillValid_returnsExistingCode() {
        // Join code masih valid (expired di masa depan)
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().plusSeconds(3600));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.generateJoinCode(PROJECT_ID);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<String> body = (BaseResponseDTO<String>) response.getBody();

        assertNotNull(body);
        // Kode lama harus dikembalikan tanpa generate ulang
        assertEquals(JOIN_CODE, body.getData());
        // Save tidak dipanggil karena kode masih valid
        verify(projectDb, never()).save(any(Project.class));
    }

    @Test
    void generateJoinCode_existingCodeExpired_regeneratesCode() {
        // Join code sudah expired
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().minusSeconds(3600));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.save(any(Project.class))).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.generateJoinCode(PROJECT_ID);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        // Kode baru harus di-generate dan save dipanggil
        verify(projectDb).save(dummyProject);
    }

    @Test
    void generateJoinCode_notOwner_returns403() {
        User otherUser = new User();
        otherUser.setUsername("other_pm");

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_pm");
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.generateJoinCode(PROJECT_ID);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<String> body = (BaseResponseDTO<String>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
        verify(projectDb, never()).save(any(Project.class));
    }

    @Test
    void generateJoinCode_generatedCodeIsEightCharsUppercase() {
        dummyProject.setJoinCode(null);
        dummyProject.setJoinCodeExpiredAt(null);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.save(any(Project.class))).thenAnswer(inv -> {
            Project saved = inv.getArgument(0);
            String code = saved.getJoinCode();
            assertNotNull(code);
            assertEquals(8, code.length());
            assertEquals(code.toUpperCase(), code);
            return saved;
        });

        projectService.generateJoinCode(PROJECT_ID);
    }

    @Test
    void generateJoinCode_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<?> response = projectService.generateJoinCode(PROJECT_ID);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<String> body = (BaseResponseDTO<String>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Unexpected error", body.getMessage());
    }

    // =========================================================================
    // joinProject
    // =========================================================================

    private ProjectMember buildDummyProjectMember() {
        ProjectMember pmb = new ProjectMember();
        pmb.setUsername(USERNAME_PMB);
        return pmb;
    }

    @Test
    void joinProject_validCode_returns201() {
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().plusSeconds(3600));

        when(projectDb.findProjectByJoinCode(JOIN_CODE)).thenReturn(dummyProject);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectMemberDb.findByUsername(USERNAME_PMB)).thenReturn(buildDummyProjectMember());

        ResponseEntity<?> response = projectService.joinProject(JOIN_CODE);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.CREATED.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals(PROJECT_ID, body.getData().getMessageDetail());
    }

    @Test
    void joinProject_validCode_memberAndUserInProjectSaved() {
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().plusSeconds(3600));

        when(projectDb.findProjectByJoinCode(JOIN_CODE)).thenReturn(dummyProject);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectMemberDb.findByUsername(USERNAME_PMB)).thenReturn(buildDummyProjectMember());

        projectService.joinProject(JOIN_CODE);

        verify(memberInProjectDb).save(any(MemberInProject.class));
        verify(userInProjectDb).save(any(UserInProject.class));
    }

    @Test
    void joinProject_invalidCode_returns404() {
        when(projectDb.findProjectByJoinCode("INVALID")).thenReturn(null);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectMemberDb.findByUsername(USERNAME_PMB)).thenReturn(buildDummyProjectMember());

        ResponseEntity<?> response = projectService.joinProject("INVALID");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.NOT_FOUND.value(), body.getStatus());
        assertEquals("Kode yang dimasukkan tidak valid!", body.getMessage());
        verify(memberInProjectDb, never()).save(any(MemberInProject.class));
        verify(userInProjectDb, never()).save(any(UserInProject.class));
    }

    @Test
    void joinProject_expiredCode_returns410() {
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().minusSeconds(3600));

        when(projectDb.findProjectByJoinCode(JOIN_CODE)).thenReturn(dummyProject);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectMemberDb.findByUsername(USERNAME_PMB)).thenReturn(buildDummyProjectMember());

        ResponseEntity<?> response = projectService.joinProject(JOIN_CODE);

        assertEquals(HttpStatus.GONE, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.GONE.value(), body.getStatus());
        assertEquals("Join code sudah expired!", body.getMessage());
        verify(memberInProjectDb, never()).save(any(MemberInProject.class));
        verify(userInProjectDb, never()).save(any(UserInProject.class));
    }

    @Test
    void joinProject_unexpectedException_returns500() {
        when(projectDb.findProjectByJoinCode(anyString()))
                .thenThrow(new RuntimeException("DB connection failed"));

        ResponseEntity<?> response = projectService.joinProject(JOIN_CODE);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("DB connection failed", body.getMessage());
    }

    @Test
    void joinProject_responseContainsProjectId() {
        dummyProject.setJoinCode(JOIN_CODE);
        dummyProject.setJoinCodeExpiredAt(Instant.now().plusSeconds(3600));

        when(projectDb.findProjectByJoinCode(JOIN_CODE)).thenReturn(dummyProject);
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectMemberDb.findByUsername(USERNAME_PMB)).thenReturn(buildDummyProjectMember());

        ResponseEntity<?> response = projectService.joinProject(JOIN_CODE);

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        // Field "message" pada CrudResponseDTO berisi projectId
        assertEquals("Kode Project", body.getData().getMessage());
        assertEquals("proj-001", body.getData().getMessageDetail());
    }

    // =========================================================================
    // getUsersInProject
    // =========================================================================

    private static final String SORT_USERNAME  = "username";
    private static final String SORT_FULL_NAME = "fullName";

    /** Membangun project yang mengandung satu member agar cek akses lolos. */
    private Project buildProjectWithMember(String memberUsername) {
        ProjectMember member = new ProjectMember();
        member.setUsername(memberUsername);

        MemberInProject mip = new MemberInProject();
        mip.setProjectMember(member);

        Project p = new Project();
        p.setProjectId(PROJECT_ID);
        p.setProjectName("Project Alpha");
        p.setDescription("Desc");
        p.setStatus("IN_PROGRESS");
        p.setStartDate(START_DATE);
        p.setEndDate(END_DATE);
        p.setProjectManager(dummyPm);
        p.setMemberInProjectList(List.of(mip));
        p.setStageList(List.of());
        return p;
    }

    @Test
    void getUsersInProject_pmOwnerRoleZero_returnsOnlyPm() {
        // role=0 → hanya PM
        Page<UsersInProjectResponseDTO> pmPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(projectDb.findProjectPM(eq(PROJECT_ID), any(Pageable.class))).thenReturn(pmPage);

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 0, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals(1, body.getData().getTotalElements());
        verify(projectDb).findProjectPM(eq(PROJECT_ID), any(Pageable.class));
        verifyNoMoreInteractions(memberInProjectDb);
        verifyNoInteractions(userInProjectDb);
    }

    @Test
    void getUsersInProject_pmOwnerRoleOne_returnsOnlyMembers() {
        // role=1 → hanya member
        Page<UsersInProjectResponseDTO> memberPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(memberInProjectDb.findProjectPMB(eq(PROJECT_ID), any(Pageable.class))).thenReturn(memberPage);

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 1, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals("SUCCESS", body.getMessage());
        verify(memberInProjectDb).findProjectPMB(eq(PROJECT_ID), any(Pageable.class));
        verifyNoInteractions(userInProjectDb);
    }

    @Test
    void getUsersInProject_pmOwnerRoleOther_returnsAllUsers() {
        // role selain 0/1 → semua user
        Page<UsersInProjectResponseDTO> allUsersPage =
                new PageImpl<>(List.of(new UsersInProjectResponseDTO(), new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(allUsersPage);

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 2, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(2, body.getData().getTotalElements());
        verify(userInProjectDb).findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class));
    }

    @Test
    void getUsersInProject_memberInProject_returns200() {
        // Member yang terdaftar juga berhak melihat daftar user
        Page<UsersInProjectResponseDTO> allUsersPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));
        Project projectWithMember = buildProjectWithMember(USERNAME_PMB);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(projectWithMember);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(allUsersPage);

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 2, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getUsersInProject_userNotInProject_returns403() {
        // User asing tidak boleh melihat daftar user project
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("outsider");
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject); // memberList kosong

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 2, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
    }

    @Test
    void getUsersInProject_invalidSortingColumn_returns400() {
        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 0, "invalidColumn", ORDER_ASC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Sorting column is not valid!", body.getMessage());
    }

    @Test
    void getUsersInProject_sortAscending_pageableHasAscendingSort() {
        Page<UsersInProjectResponseDTO> page = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(page);

        projectService.getUsersInProject(PROJECT_ID, 0, 10, 2, SORT_USERNAME, ORDER_ASC);

        verify(userInProjectDb).findUsersByProjectId(
                eq(PROJECT_ID),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_USERNAME);
                    return order != null && order.isAscending();
                })
        );
    }

    @Test
    void getUsersInProject_sortDescending_pageableHasDescendingSort() {
        Page<UsersInProjectResponseDTO> page = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(page);

        projectService.getUsersInProject(PROJECT_ID, 0, 10, 2, SORT_USERNAME, ORDER_DESC);

        verify(userInProjectDb).findUsersByProjectId(
                eq(PROJECT_ID),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_USERNAME);
                    return order != null && order.isDescending();
                })
        );
    }

    @Test
    void getUsersInProject_sortByFullName_pageableUsesFullNameColumn() {
        Page<UsersInProjectResponseDTO> page = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(page);

        projectService.getUsersInProject(PROJECT_ID, 0, 10, 2, SORT_FULL_NAME, ORDER_ASC);

        verify(userInProjectDb).findUsersByProjectId(
                eq(PROJECT_ID),
                argThat(p -> p.getSort().getOrderFor(SORT_FULL_NAME) != null)
        );
    }

    @Test
    void getUsersInProject_paginationParamsForwardedCorrectly() {
        Page<UsersInProjectResponseDTO> page = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectId(eq(PROJECT_ID), any(Pageable.class))).thenReturn(page);

        projectService.getUsersInProject(PROJECT_ID, 3, 7, 2, SORT_USERNAME, ORDER_ASC);

        verify(userInProjectDb).findUsersByProjectId(
                eq(PROJECT_ID),
                argThat(p -> p.getPageNumber() == 3 && p.getPageSize() == 7)
        );
    }

    @Test
    void getUsersInProject_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = projectService.getUsersInProject(
                PROJECT_ID, 0, 10, 0, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("DB error", body.getMessage());
    }

    // =========================================================================
    // searchUserInProject
    // =========================================================================

    private static final String SEARCH_USER_QUERY = "john";

    @Test
    void searchUserInProject_pmOwner_returns200() {
        Page<UsersInProjectResponseDTO> resultPage =
                new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), eq(SEARCH_USER_QUERY), any(Pageable.class)))
                .thenReturn(resultPage);

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals(1, body.getData().getTotalElements());
        verify(userInProjectDb).findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), eq(SEARCH_USER_QUERY), any(Pageable.class));
    }

    @Test
    void searchUserInProject_memberInProject_returns200() {
        Page<UsersInProjectResponseDTO> resultPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));
        Project projectWithMember = buildProjectWithMember(USERNAME_PMB);

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PMB);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(projectWithMember);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), anyString(), any(Pageable.class)))
                .thenReturn(resultPage);

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void searchUserInProject_userNotInProject_returns403() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("outsider");
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject); // memberList kosong

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
        verifyNoInteractions(userInProjectDb);
    }

    @Test
    void searchUserInProject_noMatchFound_returns200WithEmptyPage() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), anyString(), any(Pageable.class)))
                .thenReturn(Page.empty());

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, "nonexistent", 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(0, body.getData().getTotalElements());
    }

    @Test
    void searchUserInProject_blankQuery_returns200() {
        Page<UsersInProjectResponseDTO> resultPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), eq(""), any(Pageable.class)))
                .thenReturn(resultPage);

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, "", 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userInProjectDb).findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), eq(""), any(Pageable.class));
    }

    @Test
    void searchUserInProject_invalidSortingColumn_returns400() {
        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, SEARCH_USER_QUERY, 0, 10, "invalidColumn", ORDER_ASC);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
        assertEquals("Sorting column is not valid!", body.getMessage());
        verifyNoInteractions(userInProjectDb);
    }

    @Test
    void searchUserInProject_sortAscending_pageableHasAscendingSort() {
        Page<UsersInProjectResponseDTO> resultPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), anyString(), any(Pageable.class)))
                .thenReturn(resultPage);

        projectService.searchUserInProject(PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_ASC);

        verify(userInProjectDb).findUsersByProjectIdAndQuery(
                eq(PROJECT_ID),
                eq(SEARCH_USER_QUERY),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_USERNAME);
                    return order != null && order.isAscending();
                })
        );
    }

    @Test
    void searchUserInProject_sortDescending_pageableHasDescendingSort() {
        Page<UsersInProjectResponseDTO> resultPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), anyString(), any(Pageable.class)))
                .thenReturn(resultPage);

        projectService.searchUserInProject(PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_DESC);

        verify(userInProjectDb).findUsersByProjectIdAndQuery(
                eq(PROJECT_ID),
                eq(SEARCH_USER_QUERY),
                argThat(p -> {
                    Sort.Order order = p.getSort().getOrderFor(SORT_USERNAME);
                    return order != null && order.isDescending();
                })
        );
    }

    @Test
    void searchUserInProject_paginationParamsForwardedCorrectly() {
        Page<UsersInProjectResponseDTO> resultPage = new PageImpl<>(List.of(new UsersInProjectResponseDTO()));

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(userInProjectDb.findUsersByProjectIdAndQuery(
                eq(PROJECT_ID), anyString(), any(Pageable.class)))
                .thenReturn(resultPage);

        projectService.searchUserInProject(PROJECT_ID, SEARCH_USER_QUERY, 2, 8, SORT_USERNAME, ORDER_ASC);

        verify(userInProjectDb).findUsersByProjectIdAndQuery(
                eq(PROJECT_ID),
                eq(SEARCH_USER_QUERY),
                argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 8)
        );
    }

    @Test
    void searchUserInProject_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("Connection timeout"));

        ResponseEntity<?> response = projectService.searchUserInProject(
                PROJECT_ID, SEARCH_USER_QUERY, 0, 10, SORT_USERNAME, ORDER_ASC);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<Page<UsersInProjectResponseDTO>> body =
                (BaseResponseDTO<Page<UsersInProjectResponseDTO>>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Connection timeout", body.getMessage());
    }

    // =========================================================================
    // deleteProjectMemberFromProject
    // =========================================================================

    private MemberInProject buildDummyMemberInProject() {
        ProjectMember member = new ProjectMember();
        member.setUsername(USERNAME_PMB);

        MemberInProject mip = new MemberInProject();
        mip.setProjectMember(member);
        mip.setProject(dummyProject);
        return mip;
    }

    private UserInProject buildDummyUserInProject() {
        ProjectMember member = new ProjectMember();
        member.setUsername(USERNAME_PMB);

        UserInProject uip = new UserInProject();
        uip.setUser(member);
        uip.setProject(dummyProject);
        return uip;
    }

    @Test
    void deleteProjectMemberFromProject_pmOwner_returns200() {
        MemberInProject mip = buildDummyMemberInProject();
        UserInProject uip = buildDummyUserInProject();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(memberInProjectDb.findByProjectIdAndUsername(PROJECT_ID, USERNAME_PMB)).thenReturn(mip);
        when(userInProjectDb.findByProjectIdAndUsername(PROJECT_ID, USERNAME_PMB)).thenReturn(uip);

        ResponseEntity<?> response = projectService.deleteProjectMemberFromProject(PROJECT_ID, USERNAME_PMB);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());
        assertEquals("User HSE has been deleted!", body.getData().getMessageDetail());
    }

    @Test
    void deleteProjectMemberFromProject_pmOwner_deletesBothMemberAndUserInProject() {
        MemberInProject mip = buildDummyMemberInProject();
        UserInProject uip = buildDummyUserInProject();

        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(memberInProjectDb.findByProjectIdAndUsername(PROJECT_ID, USERNAME_PMB)).thenReturn(mip);
        when(userInProjectDb.findByProjectIdAndUsername(PROJECT_ID, USERNAME_PMB)).thenReturn(uip);

        projectService.deleteProjectMemberFromProject(PROJECT_ID, USERNAME_PMB);

        // Kedua relasi harus dihapus
        verify(memberInProjectDb).delete(mip);
        verify(userInProjectDb).delete(uip);
    }

    @Test
    void deleteProjectMemberFromProject_notPmOwner_returns403() {
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn("other_user");
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);

        ResponseEntity<?> response = projectService.deleteProjectMemberFromProject(PROJECT_ID, USERNAME_PMB);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.FORBIDDEN.value(), body.getStatus());
        assertEquals("Access Not Allowed!", body.getMessage());
        verify(memberInProjectDb, never()).delete(any(MemberInProject.class));
        verify(userInProjectDb, never()).delete(any(UserInProject.class));
    }

    @Test
    void deleteProjectMemberFromProject_memberNotFound_returns500() {
        // findByProjectIdAndUsername melempar exception karena data tidak ditemukan
        when(jwtUtils.getUserNameFromRequest(request)).thenReturn(USERNAME_PM);
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        when(memberInProjectDb.findByProjectIdAndUsername(PROJECT_ID, "unknown_user"))
                .thenThrow(new RuntimeException("Member not found"));

        ResponseEntity<?> response = projectService.deleteProjectMemberFromProject(PROJECT_ID, "unknown_user");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("FAILED", body.getData().getMessage());
        assertEquals("Member not found", body.getData().getMessageDetail());
    }

    @Test
    void deleteProjectMemberFromProject_unexpectedException_returns500() {
        when(jwtUtils.getUserNameFromRequest(request))
                .thenThrow(new RuntimeException("JWT error"));

        ResponseEntity<?> response = projectService.deleteProjectMemberFromProject(PROJECT_ID, USERNAME_PMB);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<CrudResponseDTO> body = (BaseResponseDTO<CrudResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("FAILED", body.getData().getMessage());
    }

    // =========================================================================
    // getProjectStatistics
    // =========================================================================

    /**
     * Membuat stub stageService.getStageStatistics() yang mengembalikan
     * ProgressResponseDTO dengan nilai yang ditentukan.
     */
    private void stubStageStatistics(String stageId,
                                     int total, int finished, int inProgress, int todo) {
        ProgressResponseDTO progress = ProgressResponseDTO.builder()
                .totalTask(total)
                .finishedTask(finished)
                .inProgressTask(inProgress)
                .todoTask(todo)
                .progress(total == 0 ? 0.0 : (finished * 1.0 / total * 100))
                .build();

        BaseResponseDTO<ProgressResponseDTO> wrapped = new BaseResponseDTO<>();
        wrapped.setData(progress);
        wrapped.setStatus(HttpStatus.OK.value());
        wrapped.setMessage("SUCCESS");

        when(stageService.getStageStatistics(stageId))
                .thenReturn(new ResponseEntity<>(wrapped, HttpStatus.OK));
    }

    /** Membuat Stage dummy dan menyisipkannya ke project. */
    private Stage buildStage(String stageId) {
        Stage stage = new Stage();
        stage.setStageId(stageId);
        return stage;
    }

    @Test
    void getProjectStatistics_noStages_returnsZeroProgress() {
        // Project tanpa stage: semua counter = 0, progress = 0.00
        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject); // stageList kosong

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.OK.value(), body.getStatus());
        assertEquals("SUCCESS", body.getMessage());

        ProgressResponseDTO data = body.getData();
        assertNotNull(data);
        assertEquals(0, data.getTotalTask());
        assertEquals(0, data.getFinishedTask());
        assertEquals(0, data.getInProgressTask());
        assertEquals(0, data.getTodoTask());
        assertEquals(0.00, data.getProgress());
    }

    @Test
    void getProjectStatistics_singleStageAllDone_returns100Percent() {
        Stage stage = buildStage("stage-1");
        dummyProject.setStageList(List.of(stage));

        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        stubStageStatistics("stage-1", 5, 5, 0, 0);

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        ProgressResponseDTO data = body.getData();
        assertEquals(5, data.getTotalTask());
        assertEquals(5, data.getFinishedTask());
        assertEquals(0, data.getInProgressTask());
        assertEquals(0, data.getTodoTask());
        assertEquals(100.0, data.getProgress());
    }

    @Test
    void getProjectStatistics_multipleStagesAggregatesCorrectly() {
        // Stage 1: 4 task (2 done, 1 in-progress, 1 todo)
        // Stage 2: 6 task (3 done, 2 in-progress, 1 todo)
        // Total  : 10 task, 5 done → 50%
        Stage s1 = buildStage("stage-1");
        Stage s2 = buildStage("stage-2");
        dummyProject.setStageList(List.of(s1, s2));

        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        stubStageStatistics("stage-1", 4, 2, 1, 1);
        stubStageStatistics("stage-2", 6, 3, 2, 1);

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        ProgressResponseDTO data = body.getData();
        assertEquals(10, data.getTotalTask());
        assertEquals(5,  data.getFinishedTask());
        assertEquals(3,  data.getInProgressTask());
        assertEquals(2,  data.getTodoTask());
        assertEquals(50.0, data.getProgress());
    }

    @Test
    void getProjectStatistics_stagesWithNoTasks_progressIsZero() {
        // Stage ada tapi tidak punya task → tidak boleh terjadi pembagian nol
        Stage stage = buildStage("stage-empty");
        dummyProject.setStageList(List.of(stage));

        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        stubStageStatistics("stage-empty", 0, 0, 0, 0);

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(0.00, body.getData().getProgress());
    }

    @Test
    void getProjectStatistics_stageServiceCalledForEachStage() {
        Stage s1 = buildStage("stage-1");
        Stage s2 = buildStage("stage-2");
        Stage s3 = buildStage("stage-3");
        dummyProject.setStageList(List.of(s1, s2, s3));

        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        stubStageStatistics("stage-1", 2, 1, 1, 0);
        stubStageStatistics("stage-2", 3, 2, 0, 1);
        stubStageStatistics("stage-3", 1, 0, 0, 1);

        projectService.getProjectStatistics(PROJECT_ID);

        // stageService harus dipanggil tepat satu kali per stage
        verify(stageService).getStageStatistics("stage-1");
        verify(stageService).getStageStatistics("stage-2");
        verify(stageService).getStageStatistics("stage-3");
        verifyNoMoreInteractions(stageService);
    }

    @Test
    void getProjectStatistics_progressCalculationIsCorrect() {
        // 3 dari 8 task selesai → 37.5%
        Stage stage = buildStage("stage-1");
        dummyProject.setStageList(List.of(stage));

        when(projectDb.findByProjectId(PROJECT_ID)).thenReturn(dummyProject);
        stubStageStatistics("stage-1", 8, 3, 3, 2);

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(37.5, body.getData().getProgress(), 0.001);
    }

    @Test
    void getProjectStatistics_unexpectedException_returns500() {
        when(projectDb.findByProjectId(PROJECT_ID))
                .thenThrow(new RuntimeException("Project not found"));

        ResponseEntity<?> response = projectService.getProjectStatistics(PROJECT_ID);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        @SuppressWarnings("unchecked")
        BaseResponseDTO<ProgressResponseDTO> body =
                (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

        assertNotNull(body);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), body.getStatus());
        assertEquals("Project not found", body.getMessage());
    }
}
