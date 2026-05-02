package com.ta.managementproject.service.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.security.util.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;


@Service
@Transactional
public class TaskServiceImpl implements TaskService {
    private final ProjectManagerDb projectManagerDb;
    private final ProjectMemberDb projectMemberDb;
    private final TaskDb taskDb;
    private final ProjectDb projectDb;
    private final StageDb stageDb;
    private final HttpServletRequest request;
    private final MemberInProjectDb memberInProjectDb;
    private final UserService userService;
    private final SubTaskDb subTaskDb;
    private final AuthService authService;
    private final UtilService utilService;
    private final TaskDbWithDsl taskDbWithDsl;

    public TaskServiceImpl(
            ProjectManagerDb projectManagerDb,
            ProjectMemberDb projectMemberDb,
            TaskDb taskDb,
            ProjectDb projectDb,
            StageDb stageDb,
            HttpServletRequest request,
            MemberInProjectDb memberInProjectDb,
            UserService userService,
            SubTaskDb subTaskDb,
            AuthService authService,
            UtilService utilService,
            TaskDbWithDsl taskDbWithDsl

    ) {
        this.projectManagerDb = projectManagerDb;
        this.projectMemberDb = projectMemberDb;
        this.taskDb = taskDb;
        this.projectDb = projectDb;
        this.stageDb = stageDb;
        this.request = request;
        this.memberInProjectDb = memberInProjectDb;
        this.userService = userService;
        this.subTaskDb = subTaskDb;
        this.authService = authService;
        this.utilService = utilService;
        this.taskDbWithDsl = taskDbWithDsl;
    }

    @Override // Total CYC: 26, LOC:  140
    public ResponseEntity<?> getAllTask(
            Pageable pageable,
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String keyword
    ) {
        Stage stage = authService.validateStage(stageId);  // CYC: 2, LOC: 8
        authService.validateManagerAndMemberAccess(stage.getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3
        // CYC: 3, LOC: 10

        Page<TaskResponseDTO> tasks = taskDbWithDsl.findAll( // CYC: 19, LOC: 110
                stageId,
                dueDate,
                createdAt,
                updatedAt,
                priority,
                order,
                keyword,
                pageable
        );

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", tasks);
    }

    @Override // Total CYC: 13, LOC: 88
    @Transactional
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 1, LOC: 20
            Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8
            authService.validateManagerAccess(stage.getProject(), JwtUtils.getCurrentUsername());
            // CYC: 2, LOC: 6
            // CYC: 1, LOC: 3
            authService.validateProjectCancellation(stage.getProject()); // CYC: 2, LOC: 6

            Integer currentTotal = taskDb.getTotalTask(stageId);

            Task newTask = Task.builder()
                    .taskName(requestDTO.getTaskName())
                    .description(requestDTO.getDescription())
                    .priority(requestDTO.getPriority())
                    .dueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                    .status("TODO")
                    .projectMember(requestDTO.getProjectMember())
                    .stage(stage)
                    .order(currentTotal + 1)
                    .isDeleted(false)
                    .build();

            taskDb.save(newTask);
            utilService.updateStageSummary(stageId); // CYC: 2, LOC: 18
            utilService.updateProjectSummary(stage.getProject().getProjectId()); // CYC: 2, LOC: 18

            // CYC: 1, LOC: 9
            return utilService.buildResponse(HttpStatus.CREATED, "Task created successfully", new CrudResponseDTO(newTask.getTaskId(), Instant.now().toString()));
    }

    @Override // Total CYC: 8, LOC: 45
    public ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 1, LOC: 13
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8

        authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6
        // CYC: 1, LOC: 3
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6

        task.setTaskName(requestDTO.getTaskName());
        task.setDescription(requestDTO.getDescription());
        task.setPriority(requestDTO.getPriority());
        task.setDueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant());
        task.setProjectMember(requestDTO.getProjectMember());

        taskDb.save(task);

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.CREATED, "Task updated successfully", new CrudResponseDTO(taskId, Instant.now().toString()));
      }


    @Override // Total CYC: 9, LOC: 53
    public ResponseEntity<?> getDetailTask(String taskId) { // CYC: 2, LOC: 23
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8

        authService.validateManagerAndMemberAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3
        // CYC: 3, LOC: 10

        TaskDetailResponseDTO response = new TaskDetailResponseDTO();
        response.setTaskId(task.getTaskId());
        response.setTaskName(task.getTaskName());
        response.setProjectMemberName(task.getProjectMember() != null ? task.getProjectMember().getFullName() : "Unassigned");
        response.setDescription(task.getDescription());
        response.setDueDate(task.getDueDate());
        response.setLabel(task.getLabel());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setFinishedTask(task.getFinishedTask());
        response.setTodoTask(task.getTodoTask());
        response.setInProgressTask(task.getInProgressTask());
        response.setTotalTask(task.getTotalTask());
        response.setProgress(task.getProgress());
        response.setOrder(task.getOrder());


        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", response);
    }

    @Override // Total CYC: 14, LOC: 82
    @Transactional
    public ResponseEntity<?> deleteTaskById(String stageId, String taskId) { // CYC: 2, LOC: 14
            Task task = authService.validateTask(taskId); // CYC: 2, LOC: 8
            authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
            // CYC: 2, LOC: 6
            // CYC: 1, LOC: 3
            authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6

            Integer order = task.getOrder();

            taskDb.softDeleteSubTaskByTaskId(taskId);

            taskDb.deleteById(taskId);
            taskDb.updateTaskOrderAfterDelete(stageId, order);
            utilService.updateStageSummary(stageId); // CYC: 2, LOC: 18
            utilService.updateProjectSummary(task.getStage().getProject().getProjectId()); // CYC: 2, LOC: 18

            // CYC: 1, LOC: 9
            return utilService.buildResponse(HttpStatus.OK, "Tasks deleted successfully", null);
    }

    @Override // Total CYC: 13, LOC: 61
    @Transactional
    public ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO) { // CYC: 3, LOC: 21
        authService.validateStage(stageId); // CYC: 2, LOC: 8
        Task task = authService.validateTask(requestDTO.getId()); // CYC: 2, LOC: 8
        int totalTask = task.getStage().getTaskList().size();

        authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername()); // CYC: 2, LOC: 6 // CYC: 1, LOC: 3
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6

        Integer boundedOrder = Math.max(1, Math.min(totalTask, requestDTO.getOrder()));

        if (Objects.equals(task.getOrder(), boundedOrder)) {
            throw new ConflictException("Task is already in the requested position!");
        }

        if (task.getOrder() > boundedOrder){
            taskDb.updateTaskOrderAbove(stageId, boundedOrder, task.getOrder() - 1);
        }else {
            taskDb.updateTaskOrderBelow(stageId, boundedOrder + 1, task.getOrder() + 1);
        }

        task.setOrder(boundedOrder);
        taskDb.save(task);

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "Tasks reordered successfully", null);
    }

    @Override // Total CYC: 13, LOC: 90
    @Transactional
    public ResponseEntity<?> updateTaskStatus(String taskId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 2, LOC: 15
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8
        authService.validateManagerAndMemberAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3
        // CYC: 3, LOC: 10
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6

        if (!task.getSubTaskList().isEmpty()){
            throw new ConflictException("Status is automatically set based on sub task");
        }

        task.setStatus(requestDTO.getStatus());
        taskDb.save(task);

        utilService.updateStageSummary(task.getStage().getStageId()); // CYC: 2, LOC: 18
        utilService.updateProjectSummary(task.getStage().getProject().getProjectId()); // CYC: 2, LOC: 18

        // CYC: 1, LOC: 9
        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", new CrudResponseDTO(taskId, Instant.now().toString()));
    }

}
