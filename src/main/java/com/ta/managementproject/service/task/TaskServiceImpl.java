package com.ta.managementproject.service.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

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
    private final JwtUtils jwtUtils;
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
            JwtUtils jwtUtils,
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
        this.jwtUtils = jwtUtils;
        this.request = request;
        this.memberInProjectDb = memberInProjectDb;
        this.userService = userService;
        this.subTaskDb = subTaskDb;
        this.authService = authService;
        this.utilService = utilService;
        this.taskDbWithDsl = taskDbWithDsl;
    }
    
    private String getUsernameFromToken() {

        return jwtUtils.getUserNameFromRequest(request);

    }

    @Override
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
        Stage stage = authService.validateStage(stageId);
        authService.validateManagerAndMemberAccess(stage.getProject(), getUsernameFromToken());

        Page<TaskResponseDTO> tasks = taskDbWithDsl.findAll(
                stageId,
                dueDate,
                createdAt,
                updatedAt,
                priority,
                order,
                keyword,
                pageable
        );

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", tasks);
    }

    @Override
    @Transactional
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) {
            Stage stage = authService.validateStage(stageId);
            authService.validateManagerAccess(stage.getProject(), getUsernameFromToken());
            authService.validateProjectCancellation(stage.getProject());

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

            return utilService.buildResponse(HttpStatus.CREATED, "Task created successfully", new CrudResponseDTO(newTask.getTaskId(), Instant.now().toString()));
    }

    @Override
    public ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO) {
        Task task = authService.validateTask(taskId);

        authService.validateManagerAccess(task.getStage().getProject(), getUsernameFromToken());
        authService.validateProjectCancellation(task.getStage().getProject());

        task.setTaskName(requestDTO.getTaskName());
        task.setDescription(requestDTO.getDescription());
        task.setPriority(requestDTO.getPriority());
        task.setDueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant());
        task.setStatus(requestDTO.getStatus());
        task.setProjectMember(requestDTO.getProjectMember());

        taskDb.save(task);

        return utilService.buildResponse(HttpStatus.CREATED, "Task updated successfully", new CrudResponseDTO(taskId, Instant.now().toString()));
      }


    @Override
    public ResponseEntity<?> getDetailTask(String taskId) {
        Task task = authService.validateTask(taskId);

        authService.validateManagerAndMemberAccess(task.getStage().getProject(), getUsernameFromToken());

        TaskDetailResponseDTO response = new TaskDetailResponseDTO(
                task.getTaskId(),
                task.getTaskName(),
                task.getDescription(),
                task.getPriority(),
                task.getLabel(),
                task.getDueDate(),
                task.getStatus(),
                task.getProjectMember() != null ? task.getProjectMember().getFullName() : "Unassigned",
                task.getCreatedAt()
        );

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteTaskById(String stageId, String taskId) {
            Task task = authService.validateTask(taskId);
            authService.validateManagerAccess(task.getStage().getProject(), getUsernameFromToken());
            authService.validateProjectCancellation(task.getStage().getProject());

            Integer order = task.getOrder();

            if (!task.getSubTaskList().isEmpty()){
                subTaskDb.deleteAll(task.getSubTaskList());
            }
            taskDb.deleteById(taskId);
            taskDb.updateTaskOrderAfterDelete(stageId, order);

            return utilService.buildResponse(HttpStatus.OK, "Tasks deleted successfully", null);
    }

    @Override
    @Transactional
    public ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO) {
        authService.validateStage(stageId);
        Task task = authService.validateTask(requestDTO.getId());

        authService.validateManagerAccess(task.getStage().getProject(), getUsernameFromToken());
        authService.validateProjectCancellation(task.getStage().getProject());

        if (task.getOrder() > requestDTO.getOrder()){
            taskDb.updateTaskOrderAbove(stageId, requestDTO.getOrder() - 1, task.getOrder());
        }else if (task.getOrder() < requestDTO.getOrder()){
            taskDb.updateTaskOrderBelow(stageId, requestDTO.getOrder(), task.getOrder() + 1);
        }

        task.setOrder(requestDTO.getOrder());
        taskDb.save(task);

        return utilService.buildResponse(HttpStatus.OK, "Tasks reordered successfully", null);
    }

    @Override
    public ResponseEntity<?> updateTaskStatus(String taskId, CreateUpdateTaskRequestDTO requestDTO) {
        Task task = authService.validateTask(taskId);
        authService.validateManagerAndMemberAccess(task.getStage().getProject(), getUsernameFromToken());
        authService.validateProjectCancellation(task.getStage().getProject());

        if (!task.getSubTaskList().isEmpty()){
            throw new ConflictException("Status is automatically set based on sub task");
        }

        task.setStatus(requestDTO.getStatus());
        taskDb.save(task);

        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", new CrudResponseDTO(taskId, Instant.now().toString()));
    }
}
