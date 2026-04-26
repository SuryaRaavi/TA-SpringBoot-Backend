package com.ta.managementproject.service.auth;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.dto.response.LoginResponseDTO;
import com.ta.managementproject.dto.response.RoleResponseDTO;
import com.ta.managementproject.entity.*;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.exception.ForbiddenException;
import com.ta.managementproject.exception.NotFoundException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.AESUtil;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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

    @Override
    public ResponseEntity<?> doLogin(LoginRequestDTO request) throws Exception {
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();

        String username = request.getUsername();
        String password = request.getPassword();

        User selectedUser = userDb.findByUsername(username);

        if (selectedUser == null){
            throw new NotFoundException("USERNAME_NOT_FOUND");
        }

        String encryptedPass = aesUtil.encrypt(password);

        if (!encryptedPass.equals(selectedUser.getPassword())) {
            throw new ForbiddenException("Username atau password yang dimasukkan salah!");
        }

        String jwtToken = jwtUtils.generateJwtToken(username, selectedUser.getRole().getName());
        loginResponseDTO.setRole(new RoleResponseDTO(selectedUser.getRole().getName()));
        loginResponseDTO.setToken(jwtToken);
        loginResponseDTO.setUsername(username);
        loginResponseDTO.setExpirationDate(jwtUtils.getExpirationFromToken(jwtToken));

        return utilService.buildResponse(HttpStatus.OK, "Login Successful", loginResponseDTO);
    }

    @Override
    public Project validateProject(String projectId) {
        Project project = projectDb.findByProjectId(projectId);
        if (project == null) {
            throw new NotFoundException("PROJECT_NOT_FOUND");
        }
        return project;
    }

    @Override
    public void validateManagerAccess(Project project, String username) {
        if (!project.getProjectManager().getUsername().equals(username)) {
            throw new ForbiddenException("FORBIDDEN");
        }
    }

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

    @Override
    public Stage validateStage(String stageId){
        Stage stage = stageDb.findByStageId(stageId);
        if (stage == null){
            throw new NotFoundException("STAGE_NOT_FOUND");
        }
        return stage;
    }

    @Override
    public Task validateTask(String taskId){
        Task task = taskDb.findByTaskId(taskId);
        if (task == null){
            throw new NotFoundException("TASK_NOT_FOUND");
        }
        return task;
    }

    @Override
    public SubTask validateSubTask(String subTaskId){
        SubTask subTask = subTaskDb.findSubTaskBySubTaskId(subTaskId);
        if (subTask == null){
            throw new NotFoundException("SUB_TASK_NOT_FOUND");
        }
        return subTask;
    }

    @Override
    public void validateProjectCancellation(Project project){
        if (project.isCancelled()){
            throw new ForbiddenException("Project has been cancelled!");
        }
    }
}
