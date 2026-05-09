package com.ta.managementproject.service.auth;

import com.ta.managementproject.dto.request.LoginRequestDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.SubTask;
import com.ta.managementproject.entity.Task;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<?> doLogin(LoginRequestDTO request) throws Exception;

    Project validateProject(String projectId);

    void validateManagerAccess(Project project, String email);

    void validateManagerAndMemberAccess(Project project, String email);

    Stage validateStage(String stageId);

    Task validateTask(String taskId);

    SubTask validateSubTask(String subTaskId);

    void validateProjectCancellation(Project project);
}
