package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.response.*;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.stage.StageService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectDb projectDb;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProjectManagerDb projectManagerDb;

    @Autowired
    private MemberInProjectDb memberInProjectDb;

    @Autowired
    private ProjectMemberDb projectMemberDb;

    @Autowired
    private UserDb userDb;

    @Autowired
    private UserInProjectDb userInProjectDb;

    @Autowired
    private StageService stageService;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllProject(int page, int size, LocalDate startDate, LocalDate endDate) {
        var baseResponseDTO = new BaseResponseDTO<Page<ProjectResponseDTO>>();
        try{
            Role userRole = jwtUtils.getUserRoleFromJwtToken(request);

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(userRole == Role.PROJECT_MEMBER ? "id" : "createdAt").descending()
            );

            String username = jwtUtils.getUserNameFromRequest(request);

            Page<ProjectResponseDTO> projectList;

            if ((startDate == null && endDate != null) ||
                    (startDate != null && endDate == null)
            ){
                baseResponseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Tanggal mulai dan tanggal selesai harus terisi!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.BAD_REQUEST);
            }

            if (startDate != null && endDate != null){
                if (endDate.isBefore(startDate)){
                    baseResponseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
                    baseResponseDTO.setTimestamp(new Date());
                    baseResponseDTO.setMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
                    return new ResponseEntity<>(baseResponseDTO, HttpStatus.BAD_REQUEST);
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

            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(projectList);
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Role role = jwtUtils.getUserRoleFromJwtToken(request);

            if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
                baseResponseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.BAD_REQUEST);
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
                            .createdAt(LocalDateTime.now(ZoneId.of("Asia/Jakarta")))
                            .build();

            newProyek = projectDb.save(newProyek);

            UserInProject userInProject = UserInProject
                    .builder()
                    .user(pm)
                    .project(projectDb.findByProjectId(newProyek.getProjectId()))
                    .build();

            userInProjectDb.save(userInProject);

            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "The project has been CREATED!"));
            baseResponseDTO.setStatus(HttpStatus.CREATED.value());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setTimestamp(new Date());
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.CREATED);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();


        try{
            Role role = jwtUtils.getUserRoleFromJwtToken(request);

            if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())){
                baseResponseDTO.setStatus(HttpStatus.BAD_REQUEST.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Tanggal mulai tidak boleh lebih dari tanggal selesai!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.BAD_REQUEST);
            }

            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));
            Project project = projectDb.findByProjectId(projectId);

            if (!project.getProjectManager().getUsername().equals(user.getUsername())){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
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

            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "The project has been UPDATED!"));
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setTimestamp(new Date());
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getProjectDetail(String projectId) {
        var baseResponseDTO = new BaseResponseDTO<ProjectDetailResponseDTO>();

        try{
            Role role = jwtUtils.getUserRoleFromJwtToken(request);

            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

            Project project = projectDb.findByProjectId(projectId);

            if (
                    !project.getProjectManager().getUsername().equals(user.getUsername()) &&
                            project.getMemberInProjectList().stream().noneMatch(
                                    p -> p.getProjectMember().getUsername().equals(user.getUsername())

                            )){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }


            ProjectDetailResponseDTO responseDTO = new ProjectDetailResponseDTO();

            responseDTO.setProjectId(projectId);
            responseDTO.setProjectName(project.getProjectName());
            responseDTO.setDescription(project.getDescription());
            responseDTO.setFullNamePm(project.getProjectManager().getFullName());
            responseDTO.setStartDate(project.getStartDate());
            responseDTO.setEndDate(project.getEndDate());
            responseDTO.setStatus(project.getStatus());


            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(responseDTO);
            baseResponseDTO.setStatus(HttpStatus.OK.value());

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> deleteProjectById(String projectId) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Role role = jwtUtils.getUserRoleFromJwtToken(request);

            if (role != Role.PROJECT_MANAGER){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));


            Project project = projectDb.findByProjectId(projectId);
            if (project.getProjectManager().getUsername().equals(user.getUsername())){
                projectDb.delete(project);
            }

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "All selected project has been DELETED."));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> searchProject(int page, int size, String parameter) {
        var baseResponseDTO = new BaseResponseDTO<Page<ProjectResponseDTO>>();


        try{
            Role userRole = jwtUtils.getUserRoleFromJwtToken(request);
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(userRole == Role.PROJECT_MEMBER ? "id" : "createdAt").descending()
            );

            String username = jwtUtils.getUserNameFromRequest(request);

            Page<ProjectResponseDTO> projectList;

            if (userRole == Role.PROJECT_MANAGER){
                projectList = projectDb.findPMProjectByProjectNameOrProjectId(username, parameter, pageable);
            }else {
                projectList = memberInProjectDb.findPMBProjectByProjectNameAndProjectId(username, parameter, pageable);
            }

            baseResponseDTO.setData(projectList);
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> generateJoinCode(String projectId) {
        var baseResponseDTO = new BaseResponseDTO<String>();

        try{
            String username = jwtUtils.getUserNameFromRequest(request);

            Project project = projectDb.findByProjectId(projectId);

            if (!project.getProjectManager().getUsername().equals(username)){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            if (
                    project.getJoinCode() == null ||
                            project.getJoinCodeExpiredAt().isBefore(LocalDateTime.now())
            ) {
                project.setJoinCode(
                        UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                );
                project.setJoinCodeExpiredAt(LocalDateTime.now().plusDays(1));
                projectDb.save(project);
            }


            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setData(project.getJoinCode());
            baseResponseDTO.setStatus(HttpStatus.CREATED.value());
            baseResponseDTO.setMessage("SUCCESS");

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.CREATED);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> joinProject(String joinCode) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Project project = projectDb.findProjectByJoinCode(joinCode);
            String username = jwtUtils.getUserNameFromRequest(request);

            ProjectMember pmb = projectMemberDb.findByUsername(username);

            if (project == null){
                baseResponseDTO.setStatus(HttpStatus.NOT_FOUND.value());
                baseResponseDTO.setMessage("Kode yang dimasukkan tidak valid!");
                baseResponseDTO.setTimestamp(new Date());
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.NOT_FOUND);
            }

            if (project.getJoinCodeExpiredAt().isBefore(LocalDateTime.now())){
                baseResponseDTO.setStatus(HttpStatus.GONE.value());
                baseResponseDTO.setMessage("Join code sudah expired!");
                baseResponseDTO.setTimestamp(new Date());
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.GONE);
            }


            MemberInProject memberInProject = MemberInProject
                                    .builder()
                                    .projectMember(pmb)
                                    .project(project)
                                    .build();

            memberInProjectDb.save(memberInProject);

            UserInProject userProyek = UserInProject
                    .builder()
                    .project(project)
                    .user(pmb)
                    .build();

            userInProjectDb.save(userProyek);

            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setData(new CrudResponseDTO("Kode Project", project.getProjectId()));
            baseResponseDTO.setStatus(HttpStatus.CREATED.value());
            baseResponseDTO.setMessage("SUCCESS");

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.CREATED);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getUsersInProject(String projectId, int page, int size, Integer role) {
        var baseResponseDTO = new BaseResponseDTO<Page<UsersInProjectResponseDTO>>();

        try{
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("id").descending()
            );

            String username = jwtUtils.getUserNameFromRequest(request);
            Project project = projectDb.findByProjectId(projectId);

            if (
                    !project.getProjectManager().getUsername().equals(username) &&
                           project.getMemberInProjectList().stream().noneMatch(
                                   p -> p.getProjectMember().getUsername().equals(username)

            )){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            if (role == 0){
                baseResponseDTO.setData(projectDb.findProjectPM(projectId, pageable));
            }else if (role == 1) {
                baseResponseDTO.setData(memberInProjectDb.findProjectPMB(projectId, pageable));
            }else{
                baseResponseDTO.setData(userInProjectDb.findUsersByProjectId(projectId, pageable));
            }

            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> searchUserInProject(String projectId, String searchQuery, int page, int size) {
        var baseResponseDTO = new BaseResponseDTO<Page<UsersInProjectResponseDTO>>();

        try{
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("id").descending()
            );

            Project project = projectDb.findByProjectId(projectId);
            String username = jwtUtils.getUserNameFromRequest(request);

            if (
                    !project.getProjectManager().getUsername().equals(username) &&
                            project.getMemberInProjectList().stream().noneMatch(
                                    p -> p.getProjectMember().getUsername().equals(username)

                            )){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            Page<UsersInProjectResponseDTO> users = userInProjectDb.findUsersByProjectIdAndQuery(projectId, searchQuery, pageable);

            baseResponseDTO.setData(users);
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> deleteProjectMemberFromProject(String projectId, String username) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            String accessUser = jwtUtils.getUserNameFromRequest(request);

            Project project = projectDb.findByProjectId(projectId);

            if (!project.getProjectManager().getUsername().equals(accessUser)){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            memberInProjectDb.delete(memberInProjectDb.findByProjectIdAndUsername(projectId, username));

            userInProjectDb.delete(userInProjectDb.findByProjectIdAndUsername(projectId, username));

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "User HSE has been deleted!"));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getProjectStatistics(String projectId) {
        var baseResponseDTO = new BaseResponseDTO<ProgressResponseDTO>();

        try{
            Project project = projectDb.findByProjectId(projectId);

            Integer totalTask = 0;
            Integer totalFinishedTask = 0;
            Integer totalToDoTask = 0;
            Integer totalInProgressTask = 0;
            Double progress = 0.00;

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

            progress = totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100);

            baseResponseDTO.setData(
                    ProgressResponseDTO.builder()
                            .progress(progress)
                            .finishedTask(totalFinishedTask)
                            .inProgressTask(totalInProgressTask)
                            .todoTask(totalToDoTask)
                            .totalTask(totalTask)
                            .build()
            );
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
