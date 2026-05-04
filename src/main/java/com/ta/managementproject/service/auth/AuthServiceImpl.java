package com.ta.managementproject.service.auth;

import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.dto.response.LoginResponseDTO;
import com.ta.managementproject.dto.response.RoleResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.ForbiddenException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private final AESUtil aesUtil;
    private final UserDb userDb;
    private final JwtUtils jwtUtils;
    private final RoleDb roleDb;
    private final UtilService utilService;
    private final ProjectDb projectDb;
    private final StageDb stageDb;
    private final TaskDb taskDb;
    private final SubTaskDb subTaskDb;

    public AuthServiceImpl(
            AESUtil aesUtil,
            UserDb userDb,
            JwtUtils jwtUtils,
            RoleDb roleDb,
            UtilService utilService,
            ProjectDb projectDb,
            StageDb stageDb,
            TaskDb taskDb,
            SubTaskDb subTaskDb
    ) {
        this.aesUtil = aesUtil;
        this.userDb = userDb;
        this.jwtUtils = jwtUtils;
        this.roleDb = roleDb;
        this.utilService = utilService;
        this.projectDb = projectDb;
        this.stageDb = stageDb;
        this.taskDb = taskDb;
        this.subTaskDb = subTaskDb;
    }

    @Override // Total CYC: 7, LOC: 50, COG: 2
    public ResponseEntity<?> doLogin(LoginRequestDTO request) throws Exception { // CYC: 3, LOC: 20, COG: 2
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

        String username = request.getUsername();
        String password = request.getPassword();

        User selectedUser = userDb.findByUsername(username);

        if (selectedUser == null){
            throw new NotFoundException("USERNAME_NOT_FOUND");
        }

        String encryptedPass = aesUtil.encrypt(password); // CYC: 1, LOC: 7, COG: 0

        if (!encryptedPass.equals(selectedUser.getPassword())) {
            throw new ForbiddenException("Username atau password yang dimasukkan salah!");
        }

        String jwtToken = jwtUtils.generateJwtToken(username, selectedUser.getRole().getName()); // CYC: 1, LOC: 9, COG: 0
        loginResponseDTO.setRole(new RoleResponseDTO(selectedUser.getRole().getName()));
        loginResponseDTO.setToken(jwtToken);
        loginResponseDTO.setUsername(username);
        loginResponseDTO.setExpirationDate(jwtUtils.getExpirationFromToken(jwtToken)); // CYC: 1, LOC: 5, COG: 0

        return utilService.buildResponse(HttpStatus.OK, "Login Successful", loginResponseDTO); // CYC: 1, LOC: 9, COG: 0
    }

    // CYC: 2, LOC: 8, COG: 1
    @Override
    public Project validateProject(String projectId) {
        Project project = projectDb.findByProjectId(projectId);
        if (project == null) {
            throw new NotFoundException("PROJECT_NOT_FOUND");
        }
        return project;
    }

    // CYC: 2, LOC: 6, COG: 1
    @Override
    public void validateManagerAccess(Project project, String username) {
        if (!project.getProjectManager().getUsername().equals(username)) {
            throw new ForbiddenException("FORBIDDEN");
        }
    }

    // CYC: 3, LOC: 10, COG: 2
    @Override
    public void validateManagerAndMemberAccess(Project project, String username){
        if (
                !project.getProjectManager().getUsername().equals(username) &&
                        project.getMemberInProjectList().stream().noneMatch(
                                p -> p.getProjectMember().getUsername().equals(username)

                        )){
            throw new ForbiddenException("Access Not Allowed!");
        }
    }

    @Override // CYC: 2, LOC: 8, COG: 1
    public Stage validateStage(String stageId){
        Stage stage = stageDb.findByStageId(stageId);
        if (stage == null){
            throw new NotFoundException("STAGE_NOT_FOUND");
        }
        return stage;
    }

    @Override // CYC: 2, LOC: 8, COG: 1
    public Task validateTask(String taskId){
        Task task = taskDb.findByTaskId(taskId);
        if (task == null){
            throw new NotFoundException("TASK_NOT_FOUND");
        }
        return task;
    }

    @Override // CYC: 2, LOC: 8, COG: 1
    public SubTask validateSubTask(String subTaskId){
        SubTask subTask = subTaskDb.findSubTaskBySubTaskId(subTaskId);
        if (subTask == null){
            throw new NotFoundException("SUB_TASK_NOT_FOUND");
        }
        return subTask;
    }

    @Override // CYC: 2, LOC: 6, COG: 1
    public void validateProjectCancellation(Project project){
        if (project.isCancelled()){
            throw new ForbiddenException("Project has been cancelled!");
        }
    }
}
