package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.*;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.exception.UnprocessableContentException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectDb projectDb;
    private final AuthService authService;
    private final ProjectManagerDb projectManagerDb;
    private final MemberInProjectDb memberInProjectDb;
    private final ProjectMemberDb projectMemberDb;
    private final UserDb userDb;
    private final UserService userService;
    private final UtilService utilService;
    private final ProjectDbWithDsl projectDbWithDsl;

    private final SecureRandom secureRandom = new SecureRandom();

    public ProjectServiceImpl(
            ProjectDb projectDb,
            AuthService authService,
            ProjectManagerDb projectManagerDb,
            MemberInProjectDb memberInProjectDb,
            ProjectMemberDb projectMemberDb,
            UserDb userDb,
            UserService userService,
            UtilService utilService,
            ProjectDbWithDsl projectDbWithDsl
    ) {
        this.projectDb = projectDb;
        this.authService = authService;
        this.projectManagerDb = projectManagerDb;
        this.memberInProjectDb = memberInProjectDb;
        this.projectMemberDb = projectMemberDb;
        this.userDb = userDb;
        this.userService = userService;
        this.utilService = utilService;
        this.projectDbWithDsl = projectDbWithDsl;
    }

    @Override // Total CYC: 24, LOC: 129, COG: 17
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllProject( // CYC: 4, LOC: 13, COG: 2
            Pageable pageable, LocalDate startDate, LocalDate endDate, LocalDate createdAt, LocalDate updatedAt, String keyword
    ) {
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0

        Page<ProjectResponseDTO> projectList;

        if (startDate != null && endDate != null && endDate.isBefore(startDate)){
            throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
        }

        projectList = projectDbWithDsl.findAll(username, startDate, endDate, createdAt, updatedAt, keyword, pageable); // CYC: 18, LOC: 104, COG: 15

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", projectList); // CYC: 1, LOC: 9, COG: 0
    }

    @Override // Total CYC: 5, LOC: 50, COG: 1
    public ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO requestDTO) { // CYC: 2, LOC: 18, COG: 1
        if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
            throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
        }

        ProjectManager pm = projectManagerDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3, COG: 0

        Project newProyek = Project
                        .builder()
                        .projectName(requestDTO.getProjectName())
                        .description(requestDTO.getDescription())
                        .projectManager(pm)
                        .startDate(requestDTO.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                        .endDate(requestDTO.getEndDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                        .createdAt(Instant.now())
                        .build();

        Project createdProject = projectDb.save(newProyek);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", assignToDto(createdProject)); // CYC: 1, LOC: 20, COG: 0
    }

    @Override // Total CYC: 14, LOC: 67, COG: 11
    public ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO requestDTO) { // CYC: 7, LOC: 21, COG: 6
            User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3, COG: 0
            Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

            authService.validateManagerAccess(project, user.getUsername()); // CYC: 2, LOC: 6, COG: 1
            if (project.isCancelled()){
                throw new ConflictException("Update project is not allowed, project is already cancelled!");
            }

            if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
                throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
            }

            Project updatedProject = projectDb.save(
                    project.toBuilder()
                            .projectName(requestDTO.getProjectName() == null ? project.getProjectName() : requestDTO.getProjectName())
                            .description(requestDTO.getDescription() == null ? project.getDescription() : requestDTO.getDescription())
                            .startDate(requestDTO.getStartDate() == null ? project.getStartDate() : requestDTO.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                            .endDate(requestDTO.getEndDate() == null ? project.getEndDate() : requestDTO.getEndDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                            .build()
            );

            // CYC: 1, LOC: 9, COG: 0
            return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(updatedProject)); // CYC: 1, LOC: 20, COG: 0
    }

    // Total CYC: 9, LOC: 57, COG: 3
    @Override
    public ResponseEntity<?> getProjectDetail(String projectId) { // CYC: 1, LOC: 7, COG: 0
        User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3, COG: 0

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAndMemberAccess(project, user.getUsername()); // CYC: 3, LOC: 10, COG: 2

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(project)); // CYC: 1, LOC: 20, COG: 0
    }

    @Override // Total CYC: 8, LOC: 38, COG: 2
    @Transactional
    public ResponseEntity<?> deleteProjectById(String projectId) { // CYC: 1, LOC: 12, COG: 0
        User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3, COG: 0

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(project, user.getUsername()); // CYC: 2, LOC: 6, COG: 1

        projectDb.softDeleteSubTaskByProjectId(projectId);
        projectDb.softDeleteTaskByProjectId(projectId);
        projectDb.softDeleteStageByProjectId(projectId);

        projectDb.delete(project);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Project has been DELETED!"));
    }

    @Override // Total CYC: 13, LOC: 63, COG: 6
    public ResponseEntity<?> generateJoinCode(String projectId) { // CYC: 2, LOC: 22, COG: 1
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(project, username); // CYC: 2, LOC: 6, COG: 1
        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6, COG: 1

        if (
             shouldRenewJoinCode(project) // CYC: 3, LOC: 9, COG: 2
        ) {
            byte[] randomBytes = new byte[16];
            secureRandom.nextBytes(randomBytes);

            project.setJoinCode(
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(randomBytes)
                            .substring(0, 16)
            );

            project.setJoinCodeExpiredAt(Instant.now().plusSeconds(86400));
            projectDb.save(project);
        }

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", project.getJoinCode());
    }

    @Override // Total CYC: 10, LOC: 48, COG: 5
    public ResponseEntity<?> joinProject(String joinCode) { // CYC: 3, LOC: 21, COG: 2
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0

        ProjectMember pmb = projectMemberDb.findByUsername(username);
        Project project = projectDb.findByJoinCode(joinCode);

        if (project == null){
            throw new NotFoundException("PROJECT_NOT_FOUND!");
        }

        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6, COG: 1

        if (shouldRenewJoinCode(project)){ // CYC: 3, LOC: 9, COG: 2
            throw new UnprocessableContentException("Join code has already expired or has not generated yet!");
        }

        MemberInProject memberInProject = MemberInProject
                                .builder()
                                .projectMember(pmb)
                                .project(project)
                                .createdAt(project.getCreatedAt())
                                .build();

        memberInProjectDb.save(memberInProject);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", new CrudResponseDTO("Project Code", project.getProjectId()));
    }

    @Override // Total CYC: 9, LOC: 54, COG: 2
    public ResponseEntity<?> cancelProject(String projectId){ // CYC: 2, LOC: 8, COG: 0
        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0
        authService.validateManagerAccess(project, username); // CYC: 2, LOC: 6, COG: 1

        Project cancelledProject = projectDb.save(project.toBuilder().isCancelled(true).build());

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(cancelledProject)); // CYC: 1, LOC: 20, COG: 0
    }

    // CYC: 3, LOC: 9, COG: 2
    private boolean shouldRenewJoinCode(Project project){
        if (
                project.getJoinCode() == null ||
                        project.getJoinCodeExpiredAt().isBefore(Instant.now())
        ){
            return true;
        }
        return false;
    }


    // Total CYC: 1, LOC: 20, COG: 0
    private ProjectDetailResponseDTO assignToDto(Project project){
        return ProjectDetailResponseDTO.builder()
                .projectId(project.getProjectId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .fullNamePm(project.getProjectManager().getFullName())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .status(projectDb.getProjectStatus(project.getProjectId()))
                .finishedTask(project.getFinishedTask())
                .todoTask(project.getTodoTask())
                .inProgressTask(project.getInProgressTask())
                .totalTask(project.getTotalTask())
                .progress(project.getProgress())
                .isCancelled(project.isCancelled())
                .build();
    }
}
