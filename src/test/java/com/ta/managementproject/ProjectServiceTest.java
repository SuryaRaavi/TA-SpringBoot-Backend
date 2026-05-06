package com.ta.managementproject;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.exception.UnprocessableContentException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.project.ProjectServiceImpl;
import com.ta.managementproject.service.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ta.managementproject.security.util.JwtUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectDb projectDb;
    @Mock private AuthService authService;
    @Mock private ProjectManagerDb projectManagerDb;
    @Mock private MemberInProjectDb memberInProjectDb;
    @Mock private ProjectMemberDb projectMemberDb;
    @Mock private UserDb userDb;
    @Mock private UserService userService;
    @Mock private UtilService utilService;
    @Mock private ProjectDbWithDsl projectDbWithDsl;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private MockedStatic<JwtUtils> jwtUtilsMock;

    private User mockUser;
    private ProjectManager mockPm;
    private Project mockProject;
    private CreateUpdateProjectRequestDTO mockRequest;

    @BeforeEach
    void setUp() {
        jwtUtilsMock = mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getCurrentUsername).thenReturn("manager1");

        mockPm = new ProjectManager();
        mockPm.setUsername("manager1");
        mockPm.setFullName("Manager One");

        mockUser = new User();
        mockUser.setUsername("manager1");

        mockProject = Project.builder()
                .projectId("project-1")
                .projectName("Project Alpha")
                .description("Desc")
                .projectManager(mockPm)
                .startDate(Instant.parse("2024-01-01T00:00:00Z"))
                .endDate(Instant.parse("2024-12-31T00:00:00Z"))
                .createdAt(Instant.now())
                .memberInProjectList(List.of())
                .isCancelled(false)
                .joinCode(null)
                .joinCodeExpiredAt(Instant.now().minusSeconds(1000))
                .build();

        mockRequest = new CreateUpdateProjectRequestDTO();
        mockRequest.setProjectName("Project Alpha");
        mockRequest.setDescription("Desc");
        mockRequest.setStartDate(LocalDate.of(2024, 1, 1));
        mockRequest.setEndDate(LocalDate.of(2024, 12, 31));
    }

    @AfterEach
    void tearDown() {
        jwtUtilsMock.close();
    }

    // Helper: stub utilService.buildResponse
    private void stubBuildResponse(HttpStatus status) {
        when(utilService.buildResponse(eq(status), anyString(), any()))
                .thenReturn(ResponseEntity.status(status).build());
    }

    // Helper: stub assignToDto dependencies
    private void stubAssignToDto() {
        when(projectDb.getProjectStatus(anyString())).thenReturn("ON_TRACK");
    }

    // ===================== getAllProject =====================

    @Test
    void getAllProject_ShouldReturnOk_WhenNoDateFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> page = new PageImpl<>(List.of());

        when(projectDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = projectService.getAllProject(
                pageable, null, null, null, null, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllProject_ShouldThrowBadRequestException_WhenEndDateBeforeStartDate() {
        Pageable pageable = PageRequest.of(0, 10);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                projectService.getAllProject(
                        pageable,
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2024, 1, 1),
                        null, null, null
                )
        );

        assertEquals("Tanggal mulai tidak boleh lebih dari tanggal selesai!", ex.getMessage());
    }

    @Test
    void getAllProject_ShouldNotThrow_WhenStartDateEqualsEndDate() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDate date = LocalDate.of(2024, 6, 1);
        Page<ProjectResponseDTO> page = new PageImpl<>(List.of());

        when(projectDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        assertDoesNotThrow(() ->
                projectService.getAllProject(pageable, date, date, null, null, null));
    }

    @Test
    void getAllProject_ShouldCallProjectDbWithDsl_WhenValidRequest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProjectResponseDTO> page = new PageImpl<>(List.of());

        when(projectDbWithDsl.findAll(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(page);
        stubBuildResponse(HttpStatus.OK);

        projectService.getAllProject(pageable, null, null, null, null, null);

        verify(projectDbWithDsl, times(1))
                .findAll(any(), any(), any(), any(), any(), any(), any());
    }

    // ===================== addNewProject =====================

    @Test
    void addNewProject_ShouldReturnCreated_WhenValidRequest() {
        when(projectManagerDb.findByUsername("manager1")).thenReturn(mockPm);
        when(projectDb.save(any())).thenReturn(mockProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = projectService.addNewProject(mockRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void addNewProject_ShouldThrowBadRequestException_WhenEndDateBeforeStartDate() {
        mockRequest.setStartDate(LocalDate.of(2024, 12, 31));
        mockRequest.setEndDate(LocalDate.of(2024, 1, 1));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                projectService.addNewProject(mockRequest));

        assertEquals("Tanggal mulai tidak boleh lebih dari tanggal selesai!", ex.getMessage());
    }

    @Test
    void addNewProject_ShouldCallProjectDbSave_WhenValidRequest() {
        when(projectManagerDb.findByUsername("manager1")).thenReturn(mockPm);
        when(projectDb.save(any())).thenReturn(mockProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.CREATED);

        projectService.addNewProject(mockRequest);

        verify(projectDb, times(1)).save(any(Project.class));
    }

    // ===================== updateProject =====================

    @Test
    void updateProject_ShouldReturnOk_WhenValidRequest() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        when(projectDb.save(any())).thenReturn(mockProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = projectService.updateProject("project-1", mockRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateProject_ShouldThrowConflictException_WhenProjectCancelled() {
        Project cancelledProject = mockProject.toBuilder().isCancelled(true).build();

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(cancelledProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());

        assertThrows(ConflictException.class, () ->
                projectService.updateProject("project-1", mockRequest));
    }

    @Test
    void updateProject_ShouldThrowBadRequestException_WhenEndDateBeforeStartDate() {
        mockRequest.setStartDate(LocalDate.of(2024, 12, 31));
        mockRequest.setEndDate(LocalDate.of(2024, 1, 1));

        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());

        assertThrows(BadRequestException.class, () ->
                projectService.updateProject("project-1", mockRequest));
    }

    @Test
    void updateProject_ShouldCallValidateManagerAccess() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        when(projectDb.save(any())).thenReturn(mockProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        projectService.updateProject("project-1", mockRequest);

        verify(authService, times(1)).validateManagerAccess(any(), anyString());
    }

    // ===================== getProjectDetail =====================

    @Test
    void getProjectDetail_ShouldReturnOk_WhenValidAccess() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = projectService.getProjectDetail("project-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getProjectDetail_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                projectService.getProjectDetail("project-x"));
    }

    @Test
    void getProjectDetail_ShouldCallValidateManagerAndMemberAccess() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAndMemberAccess(any(), anyString());
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        projectService.getProjectDetail("project-1");

        verify(authService, times(1)).validateManagerAndMemberAccess(any(), anyString());
    }

    // ===================== deleteProjectById =====================

    @Test
    void deleteProjectById_ShouldReturnOk_WhenValidRequest() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(projectDb).delete(any());
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = projectService.deleteProjectById("project-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void deleteProjectById_ShouldCallSoftDeleteMethods() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(projectDb).delete(any());
        stubBuildResponse(HttpStatus.OK);

        projectService.deleteProjectById("project-1");

        verify(projectDb, times(1)).softDeleteSubTaskByProjectId("project-1");
        verify(projectDb, times(1)).softDeleteTaskByProjectId("project-1");
        verify(projectDb, times(1)).softDeleteStageByProjectId("project-1");
        verify(projectDb, times(1)).delete(mockProject);
    }

    @Test
    void deleteProjectById_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(userDb.findByUsername("manager1")).thenReturn(mockUser);
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                projectService.deleteProjectById("project-x"));
    }

    // ===================== generateJoinCode =====================

    @Test
    void generateJoinCode_ShouldReturnCreated_WhenJoinCodeExpired() {
        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        when(projectDb.save(any())).thenReturn(mockProject);
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = projectService.generateJoinCode("project-1");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(projectDb, times(1)).save(any());
    }

    @Test
    void generateJoinCode_ShouldNotSave_WhenJoinCodeStillValid() {
        Project validCodeProject = mockProject.toBuilder()
                .joinCode("validcode1234")
                .joinCodeExpiredAt(Instant.now().plusSeconds(86400))
                .build();

        when(authService.validateProject("project-1")).thenReturn(validCodeProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        doNothing().when(authService).validateProjectCancellation(any());
        stubBuildResponse(HttpStatus.CREATED);

        projectService.generateJoinCode("project-1");

        verify(projectDb, never()).save(any());
    }

    @Test
    void generateJoinCode_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                projectService.generateJoinCode("project-x"));
    }

    // ===================== joinProject =====================

    @Test
    void joinProject_ShouldReturnCreated_WhenJoinCodeValid() {
        Project validCodeProject = mockProject.toBuilder()
                .joinCode("validcode1234")
                .joinCodeExpiredAt(Instant.now().plusSeconds(86400))
                .build();

        ProjectMember pmb = new ProjectMember();
        pmb.setUsername("manager1");

        when(projectMemberDb.findByUsername("manager1")).thenReturn(pmb);
        when(projectDb.findByJoinCode("validcode1234")).thenReturn(validCodeProject);
        doNothing().when(authService).validateProjectCancellation(any());
        when(memberInProjectDb.save(any())).thenReturn(new MemberInProject());
        stubBuildResponse(HttpStatus.CREATED);

        ResponseEntity<?> result = projectService.joinProject("validcode1234");

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void joinProject_ShouldThrowNotFoundException_WhenJoinCodeNotFound() {
        when(projectMemberDb.findByUsername("manager1")).thenReturn(new ProjectMember());
        when(projectDb.findByJoinCode("invalidcode")).thenReturn(null);

        assertThrows(NotFoundException.class, () ->
                projectService.joinProject("invalidcode"));
    }

    @Test
    void joinProject_ShouldThrowUnprocessableContentException_WhenJoinCodeExpired() {
        when(projectMemberDb.findByUsername("manager1")).thenReturn(new ProjectMember());
        when(projectDb.findByJoinCode("expiredcode")).thenReturn(mockProject);
        doNothing().when(authService).validateProjectCancellation(any());

        assertThrows(UnprocessableContentException.class, () ->
                projectService.joinProject("expiredcode"));
    }

    @Test
    void joinProject_ShouldCallMemberInProjectDbSave_WhenJoinCodeValid() {
        Project validCodeProject = mockProject.toBuilder()
                .joinCode("validcode1234")
                .joinCodeExpiredAt(Instant.now().plusSeconds(86400))
                .build();

        ProjectMember pmb = new ProjectMember();
        pmb.setUsername("manager1");

        when(projectMemberDb.findByUsername("manager1")).thenReturn(pmb);
        when(projectDb.findByJoinCode("validcode1234")).thenReturn(validCodeProject);
        doNothing().when(authService).validateProjectCancellation(any());
        when(memberInProjectDb.save(any())).thenReturn(new MemberInProject());
        stubBuildResponse(HttpStatus.CREATED);

        projectService.joinProject("validcode1234");

        verify(memberInProjectDb, times(1)).save(any(MemberInProject.class));
    }

    // ===================== cancelProject =====================

    @Test
    void cancelProject_ShouldReturnOk_WhenValidRequest() {
        Project cancelledProject = mockProject.toBuilder().isCancelled(true).build();

        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        when(projectDb.save(any())).thenReturn(cancelledProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        ResponseEntity<?> result = projectService.cancelProject("project-1");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void cancelProject_ShouldSaveProjectWithCancelledTrue() {
        Project cancelledProject = mockProject.toBuilder().isCancelled(true).build();

        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        when(projectDb.save(any())).thenReturn(cancelledProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        projectService.cancelProject("project-1");

        verify(projectDb).save(argThat(Project::isCancelled));
    }

    @Test
    void cancelProject_ShouldThrowNotFoundException_WhenProjectNotFound() {
        when(authService.validateProject("project-x"))
                .thenThrow(new NotFoundException("PROJECT_NOT_FOUND"));

        assertThrows(NotFoundException.class, () ->
                projectService.cancelProject("project-x"));
    }

    @Test
    void cancelProject_ShouldCallValidateManagerAccess() {
        Project cancelledProject = mockProject.toBuilder().isCancelled(true).build();

        when(authService.validateProject("project-1")).thenReturn(mockProject);
        doNothing().when(authService).validateManagerAccess(any(), anyString());
        when(projectDb.save(any())).thenReturn(cancelledProject);
        stubAssignToDto();
        stubBuildResponse(HttpStatus.OK);

        projectService.cancelProject("project-1");

        verify(authService, times(1)).validateManagerAccess(any(), eq("manager1"));
    }
}