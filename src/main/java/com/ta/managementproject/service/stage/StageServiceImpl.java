package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
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

    @Override // Total CYC: 18, LOC: 99, COG: 13
    public ResponseEntity<?> getAllStage(String projectId, Pageable pageable, LocalDate createdAt, LocalDate updatedAt, String keyword) {  // CYC: 1, LOC: 6, COG: 0
        String email = JwtUtils.getCurrentEmail(); // CYC: 1, LOC: 3, COG: 0

        Page<StageResponseDTO> stageList = stageDbWithDsl.findAll(projectId, createdAt, updatedAt, keyword, email, pageable); // CYC: 15, LOC: 81, COG: 13

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", stageList);
    }

    @Override // Total CYC: 9, LOC: 53, COG: 3
    public ResponseEntity<?> getStage(String stageId){ // CYC: 1, LOC: 7, COG: 0
        String email = JwtUtils.getCurrentEmail(); // CYC: 1, LOC: 3, COG: 0

        Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAndMemberAccess(stage.getProject(), email); // CYC: 3, LOC: 10, COG: 2

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(stage)); // CYC: 1, LOC: 16, COG: 0
    }

    @Override // Total CYC: 10, LOC: 65, COG: 3
    @Transactional
    public ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO) { // CYC: 1, LOC: 17, COG: 0
        String email = JwtUtils.getCurrentEmail(); // CYC: 1, LOC: 3, COG: 0

        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(project, email); // CYC: 2, LOC: 6, COG: 1
        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6, COG: 1

        Stage newStage = Stage.builder()
                .stageName(requestDTO.getStageName())
                .description(requestDTO.getDescription())
                .createdAt(Instant.now())
                .order(stageDb.getTotalStage(projectId) + 1)
                .project(project)
                .build();

        Stage createdStage = stageDb.save(newStage);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(createdStage)); // CYC: 1, LOC: 16, COG: 0
    }

    @Override // Total CYC: 12, LOC: 62, COG: 5
    public ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO) { // CYC: 3, LOC: 14, COG: 2
        User user = userDb.findByEmail(JwtUtils.getCurrentEmail()); // CYC: 1, LOC: 3, COG: 0

        Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(stage.getProject(), user.getEmail()); // CYC: 2, LOC: 6, COG: 1
        authService.validateProjectCancellation(stage.getProject()); // CYC: 2, LOC: 6, COG: 1

        Stage updatedStage = stageDb.save(
                stage.toBuilder()
                        .stageName(requestDTO.getStageName() == null ? stage.getStageName() : requestDTO.getStageName())
                        .description(requestDTO.getDescription() == null ? stage.getDescription() : requestDTO.getDescription())
                        .build()
        );

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK,"SUCCESS", assignToDto(updatedStage)); // CYC: 1, LOC: 16, COG: 0
    }

    @Override // Total CYC: 12, LOC: 70, COG: 6
    @Transactional
    public ResponseEntity<?> reorderStage(String projectId, ReorderRequestDTO requestDTO) { // CYC: 3, LOC: 22, COG: 3
        User user = userDb.findByEmail(JwtUtils.getCurrentEmail()); // CYC: 1, LOC: 3, COG: 0
        Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1
        int totalStage = project.getStageList().size();

        authService.validateManagerAccess(project, user.getEmail()); // CYC: 2, LOC: 6, COG: 1
        authService.validateProjectCancellation(project); // CYC: 2, LOC: 6, COG: 1

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
        Stage reorderedStage = stageDb.save(stage);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(reorderedStage)); // CYC: 1, LOC: 16, COG: 0
    }

    @Override // Total CYC: 11, LOC: 66, COG: 4
    @Transactional
    public ResponseEntity<?> deleteStageById(String projectId, String stageId) { // CYC: 1, LOC: 16, COG: 0
            User user = userDb.findByEmail(JwtUtils.getCurrentEmail()); // CYC: 1, LOC: 3, COG: 0
            Project project = authService.validateProject(projectId); // CYC: 2, LOC: 8, COG: 1

            authService.validateManagerAccess(project, user.getEmail()); // CYC: 2, LOC: 6, COG: 1
            authService.validateProjectCancellation(project); // CYC: 2, LOC: 6, COG: 1

            Stage stage = stageDb.findByStageId(stageId);
            Integer order = stage.getOrder();

            stageDb.softDeleteSubTaskByStageId(stageId);
            stageDb.softDeleteTaskByStageId(stageId);

            stageDb.delete(stage);
            stageDb.updateStageOrderAfterDelete(projectId, order);
            utilService.updateProjectSummary(projectId); // CYC: 2, LOC: 18, COG: 1

            // CYC: 1, LOC: 9, COG: 0
            return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been deleted!"));
    }

    // CYC: 1, LOC: 16, COG: 0
    private StageDetailResponseDTO assignToDto(Stage stage){
        return StageDetailResponseDTO.builder()
                .stageName(stage.getStageName())
                .order(stage.getOrder())
                .stageId(stage.getStageId())
                .finishedTask(stage.getFinishedTask())
                .todoTask(stage.getTodoTask())
                .inProgressTask(stage.getInProgressTask())
                .totalTask(stage.getTotalTask())
                .progress(stage.getProgress())
                .description(stage.getDescription())
                .projectId(stage.getProject().getProjectId())
                .isDeleted(stage.isDeleted())
                .build();
    }
}
