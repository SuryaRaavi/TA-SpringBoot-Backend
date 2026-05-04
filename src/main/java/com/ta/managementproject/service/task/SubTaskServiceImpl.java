package com.ta.managementproject.service.task;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

import com.ta.managementproject.exception.ConflictException;
import com.ta.managementproject.repository.SubTaskDbWithDsl;
import com.ta.managementproject.service.UtilService;
import com.ta.managementproject.service.auth.AuthService;
import com.ta.managementproject.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.SubTask;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.repository.MemberInProjectDb;
import com.ta.managementproject.repository.SubTaskDb;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.security.util.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class SubTaskServiceImpl implements SubTaskService{

    private final SubTaskDb subTaskDb;
    private final TaskDb taskDb;
    private final MemberInProjectDb memberInProjectDb;
    private final HttpServletRequest request;
    private final UserService userService;
    private final AuthService authService;
    private final UtilService utilService;

    private final SubTaskDbWithDsl subTaskDbWithDsl;

    public SubTaskServiceImpl(
            SubTaskDb subTaskDb,
            TaskDb taskDb,
            MemberInProjectDb memberInProjectDb,
            HttpServletRequest request,
            UserService userService,
            AuthService authService,
            UtilService utilService,
            SubTaskDbWithDsl subTaskDbWithDsl
    ) {
        this.subTaskDb = subTaskDb;
        this.taskDb = taskDb;
        this.memberInProjectDb = memberInProjectDb;
        this.request = request;
        this.userService = userService;
        this.authService = authService;
        this.utilService = utilService;
        this.subTaskDbWithDsl = subTaskDbWithDsl;
    }

    @Override // Total CYC: 21, LOC: 145, COG: 15
    public ResponseEntity<?> getAllSubTask( // CYC: 1, LOC: 23, COG: 0
            String taskId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer order,
            String keyword,
            Pageable pageable
    ) {
        String username = JwtUtils.getCurrentUsername(); // CYC: 1, LOC: 3, COG: 0

        Page<SubTaskResponseDTO> subTasks = subTaskDbWithDsl.findAll( // CYC: 18, LOC: 110, COG: 15
                taskId,
                dueDate,
                createdAt,
                updatedAt,
                order,
                keyword,
                username,
                pageable
        );

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", subTasks);
    }

    @Override // Total CYC: 19, LOC: 134, COG: 10
    @Transactional
    public ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO) { // CYC: 1, LOC: 24, COG: 0
        Task task = authService.validateTask(taskId); // CYC: 2, LOC: 8, COG: 1
        authService.validateManagerAccess(task.getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(task.getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        Integer currentTotal = subTaskDb.getTotalSubTask(taskId);

        SubTask newSubTask = SubTask.builder()
                .subTaskName(requestDTO.getSubTaskName())
                .description(requestDTO.getDescription())
                .dueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                .status("TODO")
                .label(requestDTO.getLabel())
                .projectMember(requestDTO.getProjectMember())
                .task(task)
                .order(currentTotal + 1)
                .isDeleted(false)
                .build();

        SubTask createdSubTask = subTaskDb.save(newSubTask);
        utilService.updateTaskStatusAndSummary(taskId); // CYC: 4, LOC: 26, COG: 4
        utilService.updateStageSummary(task.getStage().getStageId()); // CYC: 2, LOC: 18, COG: 1
        utilService.updateProjectSummary(task.getStage().getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1

        // CYC: 1, LOC: 9, COG: 0
        // CYC: 2, LOC: 16, COG: 1
        return utilService.buildResponse(HttpStatus.CREATED, "Sub-task created successfully", assignToDto(createdSubTask));
    }

    @Override // Total CYC: 16, LOC: 61, COG: 9
    public ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) { // CYC: 6, LOC: 13, COG: 5
        SubTask subTask = authService.validateSubTask(subTaskId); // CYC: 2, LOC: 8, COG: 1

        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(subTask.getTask().getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        subTask.setSubTaskName(requestDTO.getSubTaskName() != null ? requestDTO.getSubTaskName() : subTask.getSubTaskName());
        subTask.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription() : subTask.getDescription());
        subTask.setDueDate(requestDTO.getDueDate() != null ? requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant() : subTask.getDueDate());
        subTask.setLabel(requestDTO.getLabel() != null ? requestDTO.getLabel() : subTask.getLabel());
        subTask.setProjectMember(requestDTO.getProjectMember() != null ? requestDTO.getProjectMember() : subTask.getProjectMember());

        SubTask updatedSubTask = subTaskDb.save(subTask);

        // CYC: 1, LOC: 9, COG: 0
        // CYC: 2, LOC: 16, COG: 1
        return utilService.buildResponse(HttpStatus.OK, "Sub-task updated successfully", assignToDto(updatedSubTask));
    }

    @Override // Total CYC: 10, LOC: 52, COG: 4
    public ResponseEntity<?> getDetailSubTask(String subTaskId) { // CYC: 1, LOC: 6, COG: 0
        SubTask subTask = authService.validateSubTask(subTaskId); // CYC: 2, LOC: 8, COG: 1
        authService.validateManagerAndMemberAccess(subTask.getTask().getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3, COG: 0
        // CYC: 3, LOC: 10, COG: 2


        // CYC: 1, LOC: 9, COG: 0
        // CYC: 2, LOC: 16, COG: 1
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", assignToDto(subTask));
    }

    @Override // Total CYC: 17, LOC: 108, COG: 9
    @Transactional
    public ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId) { // CYC: 1, LOC: 14, COG: 0
        SubTask subTask = authService.validateSubTask(subTaskId); // CYC: 2, LOC: 8, COG: 1
        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(subTask.getTask().getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        Integer order = subTask.getOrder();

        subTaskDb.delete(subTask);
        subTaskDb.updateSubTaskOrderAfterDelete(taskId, order);
        utilService.updateTaskStatusAndSummary(taskId); // CYC: 4, LOC: 26, COG: 4
        utilService.updateStageSummary(subTask.getTask().getStage().getStageId()); // CYC: 2, LOC: 18, COG: 1
        utilService.updateProjectSummary(subTask.getTask().getStage().getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1

        // CYC: 1, LOC: 9, COG: 0
        return utilService.buildResponse(HttpStatus.CREATED, "Sub-task deleted successfully", new CrudResponseDTO(subTaskId, "DELETED"));
    }

    @Override // Total CYC: 15, LOC: 77, COG: 8
    @Transactional
    public ResponseEntity<?> reorderSubTask(String taskId, ReorderRequestDTO requestDTO) { // CYC: 3, LOC: 21, COG: 3
        authService.validateTask(taskId); // CYC: 2, LOC: 8, COG: 1
        SubTask subTask = authService.validateSubTask(requestDTO.getId()); // CYC: 2, LOC: 8, COG: 1

        int totalSubTask = subTask.getTask().getSubTaskList().size();
        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 2, LOC: 6, COG: 1
        // CYC: 1, LOC: 3, COG: 0
        authService.validateProjectCancellation(subTask.getTask().getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        int boundedOrder = Math.max(1, Math.min(totalSubTask, requestDTO.getOrder()));

        if (Objects.equals(subTask.getOrder(), boundedOrder)) {
            throw new ConflictException("Task is already in the requested position!");
        }

        if (subTask.getOrder() > boundedOrder){
            subTaskDb.updateSubTaskOrderAbove(taskId, boundedOrder, subTask.getOrder() - 1);
        }else {
            subTaskDb.updateSubTaskOrderBelow(taskId, boundedOrder + 1, subTask.getOrder() );
        }

        subTask.setOrder(boundedOrder);
        SubTask reorderedSubTask = subTaskDb.save(subTask);

        // CYC: 1, LOC: 9, COG: 0
        // CYC: 2, LOC: 16, COG: 1
        return utilService.buildResponse(HttpStatus.OK, "Sub-tasks reordered successfully", assignToDto(reorderedSubTask));
    }

    @Override // Total CYC: 20, LOC: 127, COG: 12
    @Transactional
    public ResponseEntity<?> updateSubTaskStatus(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) { // CYC: 1, LOC: 13, COG: 0
        SubTask subTask = authService.validateSubTask(subTaskId); // CYC: 2, LOC: 8, COG: 1
        authService.validateManagerAndMemberAccess(subTask.getTask().getStage().getProject(), JwtUtils.getCurrentUsername());
        // CYC: 1, LOC: 3, COG: 0
        // CYC: 3, LOC: 10, COG: 2

        authService.validateProjectCancellation(subTask.getTask().getStage().getProject()); // CYC: 2, LOC: 6, COG: 1

        subTask.setStatus(requestDTO.getStatus());
        SubTask updatedSubTask = subTaskDb.save(subTask);
        utilService.updateTaskStatusAndSummary(subTask.getTask().getTaskId()); // CYC: 4, LOC: 26, COG: 4
        utilService.updateStageSummary(subTask.getTask().getStage().getStageId()); // CYC: 2, LOC: 18, COG: 1
        utilService.updateProjectSummary(subTask.getTask().getStage().getProject().getProjectId()); // CYC: 2, LOC: 18, COG: 1


        // CYC: 1, LOC: 9, COG: 1
        // CYC: 2, LOC: 16, COG: 1
        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", assignToDto(updatedSubTask));
    }

    // CYC: 2, LOC: 16, COG: 1
    private SubTaskResponseDTO assignToDto(SubTask subTask){
        return SubTaskResponseDTO.builder()
                .subTaskId(subTask.getSubTaskId())
                .subTaskName(subTask.getSubTaskName())
                .dueDate(subTask.getDueDate())
                .status(subTask.getStatus())
                .label(subTask.getLabel())
                .assigneeId(subTask.getProjectMember() != null ? subTask.getProjectMember().getUsername() : "Unassigned" )
                .createdAt(subTask.getCreatedAt())
                .updatedAt(subTask.getUpdatedAt())
                .order(subTask.getOrder())
                .taskId(subTask.getTask().getTaskId())
                .description(subTask.getDescription())
                .isDeleted(subTask.isDeleted())
                .build();
    }
}
