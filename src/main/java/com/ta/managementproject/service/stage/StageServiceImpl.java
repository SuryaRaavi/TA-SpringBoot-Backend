package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.StageDetailResponseDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class StageServiceImpl implements StageService{

    private final StageDb stageDb;
    private final HttpServletRequest request;
    private final UserDb userDb;
    private final ProjectDb projectDb;
    private final TaskDb taskDb;
    private final UserService userService;
    private final SubTaskDb subTaskDb;
    private final UtilService utilService;
    private final AuthService authService;
    private final StageDbWithDsl stageDbWithDsl;

    public StageServiceImpl(
            StageDb stageDb,
            HttpServletRequest request,
            UserDb userDb,
            ProjectDb projectDb,
            TaskDb taskDb,
            UserService userService,
            SubTaskDb subTaskDb,
            UtilService utilService,
            AuthService authService,
            StageDbWithDsl stageDbWithDsl
    ) {
        this.stageDb = stageDb;
        this.request = request;
        this.userDb = userDb;
        this.projectDb = projectDb;
        this.taskDb = taskDb;
        this.userService = userService;
        this.subTaskDb = subTaskDb;
        this.utilService = utilService;
        this.authService = authService;
        this.stageDbWithDsl = stageDbWithDsl;
    }

    @Override // Total CYC: 9, LOC: 52
    public ResponseEntity<?> getAllStage(String projectId) {  // CYC: 1, LOC: 8
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8

        authService.validateManagerAndMemberAccess(project, username); // CYC: 3, LOC: 10

        List<StageResponseDTO> stageList = stageDbWithDsl.findAll(projectId); // CYC: 1, LOC: 14

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", stageList);
    }

    @Override // Total CYC: 8, LOC: 47
    public ResponseEntity<?> getStage(String stageId){ // CYC: 1, LOC: 17
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3

        Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8

        authService.validateManagerAndMemberAccess(stage.getProject(), username); // CYC: 3, LOC: 10

        StageDetailResponseDTO responseDTO = StageDetailResponseDTO.builder()
                .stageName(stage.getStageName())
                .order(stage.getOrder())
                .stageId(stage.getStageId())
                .finishedTask(stage.getFinishedTask())
                .todoTask(stage.getTodoTask())
                .inProgressTask(stage.getInProgressTask())
                .totalTask(stage.getTotalTask())
                .progress(stage.getProgress())
                .build();

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", responseDTO);
    }

    @Override // Total CYC: 9, LOC: 49
    @Transactional
    public ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO) { // CYC: 1, LOC: 17
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8

        authService.validateManagerAccess(project, username); // CYC: 2, LOC: 6
        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6

        Stage newStage = Stage.builder()
                .stageName(requestDTO.getStageName())
                .description(requestDTO.getDescription())
                .createdAt(Instant.now())
                .order(stageDb.getTotalStage(projectId) + 1)
                .project(project)
                .build();

        stageDb.save(newStage);

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "New stage has been added!"));
    }

    @Override // Total CYC: 11, LOC: 46
    public ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO) { // CYC: 3, LOC: 14
        User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3

        Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8

        authService.validateManagerAccess(stage.getProject(), user.getUsername()); // CYC: 2, LOC: 6
        authService.validateProjectCancellation(stage.getProject()); // CYC: 2, LOC: 6

        stageDb.save(
                stage.toBuilder()
                        .stageName(requestDTO.getStageName() == null ? stage.getStageName() : requestDTO.getStageName())
                        .description(requestDTO.getDescription() == null ? stage.getDescription() : requestDTO.getDescription())
                        .build()
        );

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK,"SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
    }

    @Override // Total CYC: 9, LOC: 54
    @Transactional
    public ResponseEntity<?> reorderStage(String projectId, ReorderRequestDTO requestDTO) { // CYC: 3, LOC: 22
        User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3
        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8
        int totalStage = project.getStageList().size();

        authService.validateManagerAccess(project, user.getUsername()); // CYC: 2, LOC: 6
        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6

        Stage stage = stageDb.findByStageId(requestDTO.getId());

        int boundedOrder = Math.max(1, Math.min(totalStage, requestDTO.getOrder()));

        if (Objects.equals(stage.getOrder(), boundedOrder)) {
            throw new ConflictException("Task is already in the requested position!");
        }

        if (stage.getOrder() > boundedOrder){
            stageDb.updateStageOrderAbove(projectId, boundedOrder, stage.getOrder() - 1);
        }else {
            stageDb.updateStageOrderBelow(projectId, boundedOrder + 1, stage.getOrder());
        }

        stage.setOrder(boundedOrder);
        stageDb.save(stage);

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
    }

    @Override // Total CYC: 10, LOC: 47
    @Transactional
    public ResponseEntity<?> deleteStageById(String projectId, String stageId) { // CYC: 2, LOC: 15
            User user = userDb.findByUsername(JwtUtils.getCurrentUsername()); // CYC: 1, LOC: 3
            Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8

            authService.validateManagerAccess(project, user.getUsername()); // CYC: 2, LOC: 6
            authService.validateProjectCancellation(project); // CYC: 2, LOC: 6

            Stage stage = stageDb.findByStageId(stageId);
            Integer order = stage.getOrder();

            stageDb.softDeleteSubTaskByStageId(stageId);
            stageDb.softDeleteTaskByStageId(stageId);

            stageDb.delete(stage);
            stageDb.updateStageOrderAfterDelete(projectId, order);

            // CYC: 1, LOC: 9
            return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been deleted!"));
    }
}
