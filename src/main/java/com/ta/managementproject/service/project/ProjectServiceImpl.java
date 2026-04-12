package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.response.*;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ForbiddenException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.exception.UnprocessableContentException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.stage.StageService;
import com.ta.managementproject.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {
    private final ProjectDb projectDb;
    private final HttpServletRequest request;
    private final JwtUtils jwtUtils;
    private final AuthService authService;
    private final ProjectManagerDb projectManagerDb;
    private final MemberInProjectDb memberInProjectDb;
    private final ProjectMemberDb projectMemberDb;
    private final UserDb userDb;
    private final StageService stageService;
    private final UserService userService;
    private final UtilService utilService;

    public ProjectServiceImpl(
            ProjectDb projectDb,
            HttpServletRequest request,
            JwtUtils jwtUtils,
            AuthService authService,
            ProjectManagerDb projectManagerDb,
            MemberInProjectDb memberInProjectDb,
            ProjectMemberDb projectMemberDb,
            UserDb userDb,
            StageService stageService,
            UserService userService,
            UtilService utilService
    ) {
        this.projectDb = projectDb;
        this.request = request;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
        this.projectManagerDb = projectManagerDb;
        this.memberInProjectDb = memberInProjectDb;
        this.projectMemberDb = projectMemberDb;
        this.userDb = userDb;
        this.stageService = stageService;
        this.userService = userService;
        this.utilService = utilService;
    }

    private static List<String> PROJECT_COLUMNS = List.of("projectName", "status", "createdAt");

    private static List<String> USER_COLUMNS = List.of("username", "fullName");

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllProject(
            int page,
            int size,
            Instant startDate,
            Instant endDate,
            String sortingColumn,
            String orderDirection
    ) {
        Role userRole = userService.getUserRoleByUsername(jwtUtils.getUserNameFromRequest(request));

        if (!PROJECT_COLUMNS.contains(sortingColumn)){
            throw new BadRequestException("Sorting column is not valid!");
        }

        Pageable pageable;
        if (orderDirection.equals("ascending")) {
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).ascending()
            );
        }else{
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).descending()
            );
        }

        String username = jwtUtils.getUserNameFromRequest(request);

        Page<ProjectResponseDTO> projectList;

        if ((startDate == null && endDate != null) ||
                (startDate != null && endDate == null)
        ){
            throw new BadRequestException("Tanggal mulai dan tanggal selesai harus terisi!");
        }

        if (startDate != null && endDate != null){
            if (endDate.isBefore(startDate)){
                throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
            }
        }

        if (userRole == Role.PROJECT_MANAGER){
            projectList = startDate == null && endDate == null ?
                    projectDb.findAllByProjectManager(username, pageable) :
                    projectDb.findAllByProjectManagerAndStartEndDate(username, startDate, endDate, pageable);
        }else {
            projectList = startDate == null && endDate == null ?
                    memberInProjectDb.findByProjectMember(username, pageable) :
                    memberInProjectDb.findAllByProjectMemberAndStartEndProyek(username, startDate, endDate, pageable);
        }

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", projectList);
    }

    @Override
    public ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO requestDTO) {
        if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
            throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
        }

        ProjectManager pm = projectManagerDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

        Project newProyek = Project
                        .builder()
                        .projectName(requestDTO.getProjectName())
                        .description(requestDTO.getDescription())
                        .projectManager(pm)
                        .status("NOT_STARTED")
                        .startDate(requestDTO.getStartDate())
                        .endDate(requestDTO.getEndDate())
                        .createdAt(Instant.now())
                        .build();

        projectDb.save(newProyek);

        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", new CrudResponseDTO("SUCCESS", "The project has been CREATED!"));
    }

    @Override
    public ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO requestDTO) {
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));
            Project project = authService.validateProject(projectId);

            authService.validateManagerAccess(project, user.getUsername());

            if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
                throw new BadRequestException("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
            }

            projectDb.save(
                    project.toBuilder()
                            .projectName(requestDTO.getProjectName() == null ? project.getProjectName() : requestDTO.getProjectName())
                            .description(requestDTO.getDescription() == null ? project.getDescription() : requestDTO.getDescription())
                            .startDate(requestDTO.getStartDate() == null ? project.getStartDate() : requestDTO.getStartDate())
                            .endDate(requestDTO.getEndDate() == null ? project.getEndDate() : requestDTO.getEndDate())
                            .status(requestDTO.getStatus() == null ? project.getStatus() : requestDTO.getStatus())
                            .build()
            );

            return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "The project has been UPDATED!"));
    }

    @Override
    public ResponseEntity<?> getProjectDetail(String projectId) {
        User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

        Project project = authService.validateProject(projectId);

        authService.validateManagerAndMemberAccess(project, user.getUsername());

        ProjectDetailResponseDTO responseDTO = new ProjectDetailResponseDTO();

        responseDTO.setProjectId(projectId);
        responseDTO.setProjectName(project.getProjectName());
        responseDTO.setDescription(project.getDescription());
        responseDTO.setFullNamePm(project.getProjectManager().getFullName());
        responseDTO.setStartDate(project.getStartDate());
        responseDTO.setEndDate(project.getEndDate());
        responseDTO.setStatus(project.getStatus());

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", responseDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteProjectById(String projectId) {
        User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

        Project project = authService.validateProject(projectId);

        authService.validateManagerAccess(project, user.getUsername());

        if (!project.getStageList().isEmpty()){
            for (Stage stage: project.getStageList()){
                stageService.deleteStageById(projectId, stage.getStageId());
            }
        }

        projectDb.delete(project);

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Project has been DELETED!"));
    }

    @Override
    public ResponseEntity<?> searchProject(
            int page,
            int size,
            String parameter,
            String sortingColumn,
            String orderDirection
    ) {
        Role userRole = userService.getUserRoleByUsername(jwtUtils.getUserNameFromRequest(request));

        if (!PROJECT_COLUMNS.contains(sortingColumn)){
            throw new BadRequestException("Sorting column is not valid!");
        }

        Pageable pageable;
        if (orderDirection.equals("ascending")) {
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).ascending()
            );
        }else{
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).descending()
            );
        }

        String username = jwtUtils.getUserNameFromRequest(request);

        Page<ProjectResponseDTO> projectList;

        if (userRole == Role.PROJECT_MANAGER){
            projectList = projectDb.findPMProjectByProjectNameOrProjectId(username, parameter, pageable);
        }else {
            projectList = memberInProjectDb.findPMBProjectByProjectNameAndProjectId(username, parameter, pageable);
        }

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", projectList);
    }

    @Override
    public ResponseEntity<?> generateJoinCode(String projectId) {
        String username = jwtUtils.getUserNameFromRequest(request);

        Project project = authService.validateProject(projectId);

        authService.validateManagerAccess(project, username);

        if (
                project.getJoinCode() == null ||
                        project.getJoinCodeExpiredAt().isBefore(Instant.now())
        ) {
            project.setJoinCode(
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            );
            project.setJoinCodeExpiredAt(Instant.now().plusSeconds(86400));
            projectDb.save(project);
        }

        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", project.getJoinCode());
    }

    @Override
    public ResponseEntity<?> joinProject(String joinCode) {
        String username = jwtUtils.getUserNameFromRequest(request);

        ProjectMember pmb = projectMemberDb.findByUsername(username);
        Project project = projectDb.findProjectByJoinCode(joinCode);

        if (project == null){
            throw new NotFoundException("PROJECT_NOT_FOUND!");
        }

        if (project.getJoinCodeExpiredAt().isBefore(Instant.now())){
            throw new UnprocessableContentException("Join code has beed expired!");
        }

        MemberInProject memberInProject = MemberInProject
                                .builder()
                                .projectMember(pmb)
                                .project(project)
                                .createdAt(project.getCreatedAt())
                                .build();

        memberInProjectDb.save(memberInProject);

        return utilService.buildResponse(HttpStatus.CREATED, "SUCCESS", new CrudResponseDTO("Project Code", project.getProjectId()));
    }

    @Override
    public ResponseEntity<?> getProjectStatistics(String projectId) {
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

            Project project = authService.validateProject(projectId);

            authService.validateManagerAndMemberAccess(project, user.getUsername());

            Integer totalTask = 0;
            Integer totalFinishedTask = 0;
            Integer totalToDoTask = 0;
            Integer totalInProgressTask = 0;

            for (Stage stage: project.getStageList()){
                ResponseEntity<?> response = stageService.getStageStatistics(stage.getStageId());
                BaseResponseDTO<ProgressResponseDTO> body =
                        (BaseResponseDTO<ProgressResponseDTO>) response.getBody();

                ProgressResponseDTO progressResponseDTO = body.getData();

                totalTask += progressResponseDTO.getTotalTask();
                totalFinishedTask += progressResponseDTO.getFinishedTask();
                totalInProgressTask += progressResponseDTO.getInProgressTask();
                totalToDoTask += progressResponseDTO.getTodoTask();
            }

            return utilService.buildResponse(
                    HttpStatus.OK,
                    "SUCCESS",
                    ProgressResponseDTO.builder()
                            .progress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100))
                            .finishedTask(totalFinishedTask)
                            .inProgressTask(totalInProgressTask)
                            .todoTask(totalToDoTask)
                            .totalTask(totalTask)
                            .build());
    }

}
