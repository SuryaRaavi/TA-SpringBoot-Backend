package com.ta.managementproject.service.task;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
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
    private final JwtUtils jwtUtils;
    private final HttpServletRequest request;
    private final UserService userService;
    private final AuthService authService;
    private final UtilService utilService;

    private final SubTaskDbWithDsl subTaskDbWithDsl;

    public SubTaskServiceImpl(
            SubTaskDb subTaskDb,
            TaskDb taskDb,
            MemberInProjectDb memberInProjectDb,
            JwtUtils jwtUtils,
            HttpServletRequest request,
            UserService userService,
            AuthService authService,
            UtilService utilService,
            SubTaskDbWithDsl subTaskDbWithDsl
    ) {
        this.subTaskDb = subTaskDb;
        this.taskDb = taskDb;
        this.memberInProjectDb = memberInProjectDb;
        this.jwtUtils = jwtUtils;
        this.request = request;
        this.userService = userService;
        this.authService = authService;
        this.utilService = utilService;
        this.subTaskDbWithDsl = subTaskDbWithDsl;
    }

    private String getUsernameFromRequest() {
        return jwtUtils.getUserNameFromRequest(request);
    }

    @Override
    public ResponseEntity<?> getAllSubTask(
            String taskId,
            LocalDate dueDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer order,
            String keyword,
            Pageable pageable
    ) {
        Task task = authService.validateTask(taskId);
        authService.validateManagerAndMemberAccess(task.getStage().getProject(), getUsernameFromRequest());

        Page<SubTaskResponseDTO> subTasks = subTaskDbWithDsl.findAll(
                taskId,
                dueDate,
                createdAt,
                updatedAt,
                order,
                keyword,
                pageable
        );

        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", subTasks);
    }

    @Override
    public ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO) {
        Task task = authService.validateTask(taskId);
        authService.validateManagerAccess(task.getStage().getProject(), getUsernameFromRequest());

        Integer currentTotal = subTaskDb.getTotalSubTask(taskId);

        SubTask newSubTask = SubTask.builder()
                .subTaskName(requestDTO.getSubTaskName())
                .description(requestDTO.getDescription())
                .dueDate(requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant())
                .status("TODO")
                .label(requestDTO.getLabel())
                .projectMember(requestDTO.getProjectMember())
                .task(task)
                .order(currentTotal != null ? currentTotal + 1 : 1)
                .isDeleted(false)
                .build();

        subTaskDb.save(newSubTask);
        return utilService.buildResponse(HttpStatus.CREATED, "Sub-task created successfully", new CrudResponseDTO(newSubTask.getSubTaskId(), "SUCCESS"));
    }

    @Override
    public ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) {
        SubTask subTask = authService.validateSubTask(subTaskId);

        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), getUsernameFromRequest());

        subTask.setSubTaskName(requestDTO.getSubTaskName() != null ? requestDTO.getSubTaskName() : subTask.getSubTaskName());
        subTask.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription() : subTask.getDescription());
        subTask.setDueDate(requestDTO.getDueDate() != null ? requestDTO.getDueDate().atStartOfDay(ZoneOffset.UTC).toInstant() : subTask.getDueDate());
        subTask.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : subTask.getStatus());
        subTask.setLabel(requestDTO.getLabel() != null ? requestDTO.getLabel() : subTask.getLabel());
        subTask.setProjectMember(requestDTO.getProjectMember() != null ? requestDTO.getProjectMember() : subTask.getProjectMember());

        subTaskDb.save(subTask);
        return utilService.buildResponse(HttpStatus.OK, "Sub-task updated successfully", new CrudResponseDTO(subTaskId, "SUCCESS"));
    }

    @Override
    public ResponseEntity<?> getDetailSubTask(String subTaskId) {
        SubTask subTask = authService.validateSubTask(subTaskId);
        authService.validateManagerAndMemberAccess(subTask.getTask().getStage().getProject(), getUsernameFromRequest());

        SubTaskDetailResponseDTO detail = new SubTaskDetailResponseDTO(
                subTask.getSubTaskId(),
                subTask.getSubTaskName(),
                subTask.getDescription(),
                subTask.getDueDate(),
                subTask.getStatus(),
                subTask.getLabel(),
                subTask.getProjectMember() != null ? subTask.getProjectMember().getFullName() : "Unassigned"
        );
        return utilService.buildResponse(HttpStatus.OK, "SUCCESS", detail);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId) {
        SubTask subTask = authService.validateSubTask(subTaskId);
        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), getUsernameFromRequest());
        Integer order = subTask.getOrder();

        subTaskDb.delete(subTask);
        subTaskDb.updateSubTaskOrderAfterDelete(taskId, order);
        return utilService.buildResponse(HttpStatus.CREATED, "Sub-task deleted successfully", new CrudResponseDTO(subTaskId, "DELETED"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> reorderSubTask(String taskId, ReorderRequestDTO requestDTO) {
        authService.validateTask(taskId);
        SubTask subTask = authService.validateSubTask(requestDTO.getId());

        authService.validateManagerAccess(subTask.getTask().getStage().getProject(), getUsernameFromRequest());

        if (subTask.getOrder() > requestDTO.getOrder()){
            subTaskDb.updateSubTaskOrderAbove(taskId, requestDTO.getOrder() - 1, subTask.getOrder());
        }else if (subTask.getOrder() < requestDTO.getOrder()){
            subTaskDb.updateSubTaskOrderBelow(taskId, requestDTO.getOrder(), subTask.getOrder() + 1);
        }

        subTask.setOrder(requestDTO.getOrder());
        subTaskDb.save(subTask);

        return utilService.buildResponse(HttpStatus.OK, "Sub-tasks reordered successfully", null);
    }

    @Override
    public ResponseEntity<?> updateSubTaskStatus(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) {
        SubTask subTask = authService.validateSubTask(subTaskId);
        authService.validateManagerAndMemberAccess(subTask.getTask().getStage().getProject(), getUsernameFromRequest());

        subTask.setStatus(requestDTO.getStatus());
        subTaskDb.save(subTask);

        return utilService.buildResponse(HttpStatus.OK, "Task status updated successfully", new CrudResponseDTO(subTaskId, Instant.now().toString()));
    }
}
