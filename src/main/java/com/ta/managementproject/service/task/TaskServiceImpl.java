package com.ta.managementproject.service.task;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.ta.managementproject.enums.Role;
import com.ta.managementproject.exception.BadRequestException;
import com.ta.managementproject.repository.*;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.Project;
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
            UtilService utilService
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
    }
    private static List<String> TASK_COLUMNS = List.of("taskName", "createdAt", "order", "priority", "dueDate");

    
    private String getUsernameFromToken() {

        return jwtUtils.getUserNameFromRequest(request);

    }

    @Override
    public ResponseEntity<?> getAllTask(
            int page,
            int size,
            String stageId,
            Instant startDate,
            Instant endDate,
            String sortingColumn,
            String orderDirection
    ) {
        Stage stage = authService.validateStage(stageId);
        authService.validateManagerAndMemberAccess(stage.getProject(), getUsernameFromToken());

        if (!TASK_COLUMNS.contains(sortingColumn)){
            throw new BadRequestException("Sorting column is not valid!");
        }

        Pageable pageable;
        if (orderDirection.equals("ascending")) {
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).ascending()
            );
        }else{
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).descending()
            );
        }

        Page<TaskResponseDTO> tasks;

        if (startDate != null && endDate != null) {
            tasks = taskDb.findTaskByStageIdAndDueDate(stageId, startDate, endDate, pageable);
        } else {
            tasks = taskDb.findTaskByStageId(stageId, pageable);
        }

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", tasks);
    }

    @Override
    public ResponseEntity<?> searchTask(
            int page,
            int size,
            String stageId,
            String query,
            String sortingColumn,
            String orderDirection
    ) {
        Stage stage = authService.validateStage(stageId);
        authService.validateManagerAndMemberAccess(stage.getProject(), getUsernameFromToken());

        if (!TASK_COLUMNS.contains(sortingColumn)){
            throw new BadRequestException("Sorting column is not valid!");
        }

        Pageable pageable;
        if (orderDirection.equals("ascending")) {
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).ascending()
            );
        }else{
            pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by(sortingColumn).descending()
            );
        }
        Page<TaskResponseDTO> tasks = taskDb.searchTaskByQuery(stageId, query, pageable);

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", tasks);
    }

    @Override
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) {
            Stage stage = authService.validateStage(stageId);
            authService.validateManagerAccess(stage.getProject(), getUsernameFromToken());

            Integer currentTotal = taskDb.getTotalTask(stageId);

            Task newTask = Task.builder()
                    .taskName(requestDTO.getTaskName())
                    .description(requestDTO.getDescription())
                    .priority(requestDTO.getPriority())
                    .dueDate(requestDTO.getDueDate())
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

        task.setTaskName(requestDTO.getTaskName());
        task.setDescription(requestDTO.getDescription());
        task.setPriority(requestDTO.getPriority());
        task.setDueDate(requestDTO.getDueDate());
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

            if (!task.getSubTaskList().isEmpty()){
                subTaskDb.deleteAll(task.getSubTaskList());
            }
            taskDb.deleteById(taskId);

            return utilService.buildResponse(HttpStatus.OK, "Tasks deleted successfully", null);
    }

    @Override
    @Transactional
    public ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO) {
        authService.validateStage(stageId);
        Task task = authService.validateTask(requestDTO.getId());
        authService.validateManagerAccess(task.getStage().getProject(), getUsernameFromToken());

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

        task.setStatus(requestDTO.getStatus());
        taskDb.save(task);

        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", new CrudResponseDTO(taskId, Instant.now().toString()));
    }
}
