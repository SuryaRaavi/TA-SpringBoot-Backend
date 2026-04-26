package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.security.util.JwtUtils;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.task.TaskService;
import com.ta.managementproject.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class StageServiceImpl implements StageService{

    private final StageDb stageDb;
    private final HttpServletRequest request;
    private final JwtUtils jwtUtils;
    private final UserDb userDb;
    private final ProjectDb projectDb;
    private final TaskDb taskDb;
    private final UserService userService;
    private final SubTaskDb subTaskDb;
    private final TaskService taskService;
    private final UtilService utilService;
    private final AuthService authService;
    private final StageDbWithDsl stageDbWithDsl;

    public StageServiceImpl(
            StageDb stageDb,
            HttpServletRequest request,
            JwtUtils jwtUtils,
            UserDb userDb,
            ProjectDb projectDb,
            TaskDb taskDb,
            UserService userService,
            SubTaskDb subTaskDb,
            TaskService taskService,
            UtilService utilService,
            AuthService authService,
            StageDbWithDsl stageDbWithDsl
    ) {
        this.stageDb = stageDb;
        this.request = request;
        this.jwtUtils = jwtUtils;
        this.userDb = userDb;
        this.projectDb = projectDb;
        this.taskDb = taskDb;
        this.userService = userService;
        this.subTaskDb = subTaskDb;
        this.taskService = taskService;
        this.utilService = utilService;
        this.authService = authService;
        this.stageDbWithDsl = stageDbWithDsl;
    }

    @Override
    public ResponseEntity<?> getAllStage(String projectId) {
        User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

        Project project = authService.validateProject(projectId);

        authService.validateManagerAndMemberAccess(project, user.getUsername());

        String username = jwtUtils.getUserNameFromRequest(request);

        List<StageResponseDTO> stageList =
                Role.valueOf(user.getRole().getName()) == Role.PROJECT_MANAGER ?
                stageDbWithDsl.findAll(username, null, projectId) :
                stageDbWithDsl.findAll(null, username, projectId);

        List<StageResponseDTO> stageListWithSummary = new ArrayList<>();

        for (StageResponseDTO stage: stageList){
            stageListWithSummary.add(assignProgressToStage(stage, getStageStatistics(stage.getStageId())));
        }

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", stageListWithSummary);
    }

    @Override
    public ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO) {
        String username = jwtUtils.getUserNameFromRequest(request);

        Project project = authService.validateProject(projectId);

        authService.validateManagerAccess(project, username);
        authService.validateProjectCancellation(project);

        Stage newStage = Stage.builder()
                .stageName(requestDTO.getStageName())
                .description(requestDTO.getDescription())
                .createdAt(Instant.now())
                .order((int) (long) stageDbWithDsl.totalStageByProject(projectId) + 1)
                .project(project)
                .build();

        stageDb.save(newStage);

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "New stage has been added!"));
    }

    @Override
    public ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO) {
        User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

        Stage stage = authService.validateStage(stageId);

        authService.validateManagerAccess(stage.getProject(), user.getUsername());
        authService.validateProjectCancellation(stage.getProject());

        stageDb.save(
                stage.toBuilder()
                        .stageName(requestDTO.getStageName() == null ? stage.getStageName() : requestDTO.getStageName())
                        .description(requestDTO.getDescription() == null ? stage.getDescription() : requestDTO.getDescription())
                        .build()
        );

        return utilService.buildResponse(HttpStatus.OK,"SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
        }

    @Override
    @Transactional
    public ResponseEntity<?> reorderStage(String projectId, ReorderRequestDTO requestDTO) {
        User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));
        Project project = authService.validateProject(projectId);

        authService.validateManagerAccess(project, user.getUsername());
        authService.validateProjectCancellation(project);

        Stage stage = stageDb.findByStageId(requestDTO.getId());

        if (stage.getOrder() > requestDTO.getOrder()){
            stageDb.updateStageOrderAbove(projectId, requestDTO.getOrder() - 1, stage.getOrder());
        }else if (stage.getOrder() < requestDTO.getOrder()){
            stageDb.updateStageOrderBelow(projectId, requestDTO.getOrder(), stage.getOrder() + 1);
        }

        stage.setOrder(requestDTO.getOrder());
        stageDb.save(stage);

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteStageById(String projectId, String stageId) {
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));
            Project project = authService.validateProject(projectId);

            authService.validateManagerAccess(project, user.getUsername());
            authService.validateProjectCancellation(project);

            Stage stage = stageDb.findByStageId(stageId);
            Integer order = stage.getOrder();

            if (!stage.getTaskList().isEmpty()){
                for (Task task: stage.getTaskList()){
                    taskService.deleteTaskById(stageId, task.getTaskId());
                }
            }
            stageDb.delete(stageDb.findByStageId(stageId));
            stageDb.updateStageOrderAfterDelete(projectId, order);

            return utilService.buildResponse(HttpStatus.OK, "SUCCESS", new CrudResponseDTO("SUCCESS", "Stage has been deleted!"));
    }

    public ProgressResponseDTO getStageStatistics(String stageId) {
        String username = jwtUtils.getUserNameFromRequest(request);
        Stage stage = authService.validateStage(stageId);

        Project project = stage.getProject();
        authService.validateManagerAndMemberAccess(project, username);

        ProgressResponseDTO responseDTO = new ProgressResponseDTO();

        // Jika task memiliki subtask, akumulasi task berdasarkan subtask
        Long totalFinishedTask = 0L;
        Long totalTask = 0L;
        Long totalTodoTask = 0L;
        Long totalInProgressTask = 0L;

        for (Task t: stage.getTaskList()){
            if (t.getSubTaskList().isEmpty()){
                ProgressResponseDTO progressResponseDTO = subTaskDb.getSubTaskSummary(t.getTaskId());
                totalFinishedTask += progressResponseDTO.getFinishedTask();
                totalTask += progressResponseDTO.getTotalTask();
                totalTodoTask += progressResponseDTO.getTodoTask();
                totalInProgressTask += progressResponseDTO.getInProgressTask();
            }else{
                switch (t.getStatus()) {
                    case "TODO" -> totalTodoTask++;
                    case "IN_PROGRESS" -> totalInProgressTask++;
                    default -> totalFinishedTask++;
                }

                totalTask++;
            }
        }

        responseDTO.setTotalTask(totalTask);
        responseDTO.setFinishedTask(totalFinishedTask);
        responseDTO.setTodoTask(totalTodoTask);
        responseDTO.setInProgressTask(totalInProgressTask);

        responseDTO.setProgress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100));

        return responseDTO;
    }

    private StageResponseDTO assignProgressToStage(StageResponseDTO stage, ProgressResponseDTO progress){
        return stage.toBuilder()
                .progress(progress.getProgress())
                .finishedTask(progress.getFinishedTask())
                .todoTask(progress.getTodoTask())
                .inProgressTask(progress.getInProgressTask())
                .totalTask(progress.getTotalTask())
                .build();
    }
}
