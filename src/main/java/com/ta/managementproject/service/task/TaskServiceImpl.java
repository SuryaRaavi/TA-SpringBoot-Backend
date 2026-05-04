package com.ta.managementproject.service.task;

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

    @Override // Total CYC: 23, LOC: 154, COG: 16
    public ResponseEntity<?> getAllTask( // CYC: 1, LOC: 25, COG: 0
            Pageable pageable,
            String stageId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer priority,
            Integer order,
            String keyword
    ) {
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0

        Page<TaskResponseDTO> tasks = taskDbWithDsl.findAll( // CYC: 20, LOC: 117, COG: 16
                stageId,
                dueDate,
                createdAt,
                updatedAt,
                priority,
                order,
                keyword,
                username,
                pageable
        );

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", tasks);
    }

    @Override // Total CYC: 15, LOC: 113, COG: 6
    @Transactional
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 1, LOC: 23, COG: 0
            Stage stage = authService.validateStage(stageId); // CYC: 2, LOC: 8, COG: 1
            authService.validateManagerAccess(stage.getProject(), JwtUtils.getCurrentUsername());
            // CYC: 2, LOC: 6, COG: 1
            // CYC: 1, LOC: 3, COG: 0
            authService.validateProjectCancellation(stage.getProject()); // CYC: 2, LOC: 6, COG: 1

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

            Task createdTask = taskDb.save(newTask);
            utilService.updateStageSummary(stageId); // CYC: 2, LOC: 18, COG: 1
            utilService.updateProjectSummary(stage.getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1

            // CYC: 1, LOC: 9, COG: 0
            return utilService.buildResponse(HttpStatus.CREATED, "Task created successfully", assignToDto(createdTask));// CYC: 2, LOC: 22, COG: 1
    }

    @Override // Total CYC: 11, LOC: 67, COG: 4
    public ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 1, LOC: 13, COG: 0
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        task.setTaskName(requestDTO.getTaskName());
        task.setDescription(requestDTO.getDescription());
        task.setPriority(requestDTO.getPriority());
        task.setDueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant());
        task.setProjectMember(requestDTO.getProjectMember());

        Task updatedTask = taskDb.save(task);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.CREATED, "Task updated successfully", assignToDto(updatedTask)); // CYC: 2, LOC: 22, COG: 1
      }


    @Override // Total CYC: 10, LOC: 59, COG: 4
    public ResponseEntity<?> getDetailTask(String taskId) { // CYC: 1, LOC: 6, COG: 0
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAndMemberAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3, COG: 0
        // CYC: 3, LOC: 10, COG: 2

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(task)); // CYC: 2, LOC: 22, COG: 1
    }

    @Override // Total CYC: 13, LOC: 82, COG: 5
    @Transactional
    public ResponseEntity<?> deleteTaskById(String stageId, String taskId) { // CYC: 1, LOC: 14, COG: 0
            Task task = authService.validateTask(taskId); // CYC: 2, LOC: 8, COG: 1
            authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
            // CYC: 2, LOC: 6, COG: 1
            // CYC: 1, LOC: 3, COG: 0
            authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

            Integer order = task.getOrder();

            taskDb.softDeleteSubTaskByTaskId(taskId);

            taskDb.deleteById(taskId);
            taskDb.updateTaskOrderAfterDelete(stageId, order);
            utilService.updateStageSummary(stageId); // CYC: 2, LOC: 18, COG: 1
            utilService.updateProjectSummary(task.getStage().getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1

            // CYC: 1, LOC: 9, COG: 0
            return utilService.buildResponse(HttpStatus.OK, "Tasks deleted successfully", null);
    }

    @Override // Total CYC: 15, LOC: 83, COG: 8
    @Transactional
    public ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO) { // CYC: 3, LOC: 21, COG: 3
        authService.validateStage(stageId); // CYC: 2, LOC: 8, COG: 1
        Task task = authService.validateTask(requestDTO.getId()); // CYC: 2, LOC: 8, COG: 1
        int totalTask = task.getStage().getTaskList().size();

        authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

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
        Task reordedTask = taskDb.save(task);

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "Tasks reordered successfully", assignToDto(reordedTask)); // CYC: 2, LOC: 22, COG: 1
    }

    @Override // Total CYC: 17, LOC: 109, COG: 8
    @Transactional
    public ResponseEntity<?> updateTaskStatus(String taskId, CreateUpdateTaskRequestDTO requestDTO) { // CYC: 2, LOC: 15, COG: 1
        Task task = authService.validateTask(taskId);  // CYC: 2, LOC: 8, COG: 1
        authService.validateManagerAndMemberAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3, COG: 0
        // CYC: 3, LOC: 10, COG: 2
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        if (!task.getSubTaskList().isEmpty()){
            throw new ConflictException("Status is automatically set based on sub task");
        }

        task.setStatus(requestDTO.getStatus());
        Task updatedTask = taskDb.save(task);

        utilService.updateStageSummary(task.getStage().getStageId()); // CYC: 2, LOC: 18, COG: 1
        utilService.updateProjectSummary(task.getStage().getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", assignToDto(updatedTask)); // CYC: 2, LOC: 22, COG: 1
    }

    // CYC: 2, LOC: 22, COG: 1
    private TaskDetailResponseDTO assignToDto(Task task){
        return TaskDetailResponseDTO.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .assigneeId(task.getProjectMember() != null ? task.getProjectMember().getUsername() : "Unassigned")
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .label(task.getLabel())
                .priority(task.getPriority())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .finishedTask(task.getFinishedTask())
                .todoTask(task.getTodoTask())
                .inProgressTask(task.getInProgressTask())
                .totalTask(task.getTotalTask())
                .progress(task.getProgress())
                .order(task.getOrder())
                .stageId(task.getStage().getStageId())
                .isDeleted(task.isDeleted())
                .build();
    }

}
