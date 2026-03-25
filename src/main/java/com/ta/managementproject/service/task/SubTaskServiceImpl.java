package com.ta.managementproject.service.task;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

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
import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.SubTaskDetailResponseDTO;
import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.Project;
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
   
    @Autowired
    private SubTaskDb subTaskDb;

    @Autowired
    private TaskDb taskDb;

    @Autowired
    private MemberInProjectDb memberInProjectDb;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private HttpServletRequest request;

    private String getUsernameFromRequest() {
        return jwtUtils.getUserNameFromRequest(request);
    }

    private boolean isAuthorizedForTask(String taskId) {
        String username = getUsernameFromRequest();
        Optional<Task> taskOpt = taskDb.findById(taskId);
        if (taskOpt.isEmpty()) return false;

        Project project = taskOpt.get().getStage().getProject();
        if (project.getProjectManager().getUsername().equals(username)) return true;

        return memberInProjectDb.findByProjectIdAndUsername(project.getProjectId(), username) != null;
    }

    private boolean isProjectManagerForTask(String taskId) {
        String username = getUsernameFromRequest();
        Optional<Task> taskOpt = taskDb.findById(taskId);
        if (taskOpt.isEmpty()) return false;
        return taskOpt.get().getStage().getProject().getProjectManager().getUsername().equals(username);
    }

    @Override
    public ResponseEntity<?> getAllSubTask(int page, int size, String taskId, LocalDate startDate, LocalDate endDate) {
        
        var baseResponseDTO = new BaseResponseDTO<Page<SubTaskResponseDTO>>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isAuthorizedForTask(taskId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("order").ascending());
            Page<SubTaskResponseDTO> subTasks;

            if (startDate != null && endDate != null) {
                subTasks = subTaskDb.findSubTaskByTaskIdAndDueDate(taskId, startDate, endDate, pageable);
            } else {
                subTasks = subTaskDb.findSubTaskByTaskId(taskId, pageable);
            }

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(subTasks);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> searchSubTask(int page, int size, String taskId, String query) {

        var baseResponseDTO = new BaseResponseDTO<Page<SubTaskResponseDTO>>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isAuthorizedForTask(taskId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("order").ascending());
            Page<SubTaskResponseDTO> subTasks = subTaskDb.searchSubTaskByQuery(taskId, query, pageable);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(subTasks);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManagerForTask(taskId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Only Project Managers can add sub-tasks");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Task task = taskDb.findById(taskId).orElseThrow();
            Integer currentTotal = subTaskDb.getTotalSubTask(taskId);

            SubTask newSubTask = SubTask.builder()
                    .subTaskName(requestDTO.getSubTaskName())
                    .description(requestDTO.getDescription())
                    .dueDate(requestDTO.getDueDate())
                    .status(requestDTO.getStatus())
                    .label(requestDTO.getLabel())
                    .projectMember(requestDTO.getProjectMember())
                    .task(task)
                    .order(currentTotal != null ? currentTotal + 1 : 1)
                    .isDeleted(false)
                    .build();

            subTaskDb.save(newSubTask);

            baseResponseDTO.setStatus(201);
            baseResponseDTO.setMessage("Sub-task created successfully");
            baseResponseDTO.setData(new CrudResponseDTO(newSubTask.getSubTaskId(), "SUCCESS"));
            return ResponseEntity.status(HttpStatus.CREATED).body(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            Optional<SubTask> subTaskOpt = subTaskDb.findById(subTaskId);
            if (subTaskOpt.isEmpty()) {
                baseResponseDTO.setStatus(404);
                baseResponseDTO.setMessage("Sub-task not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(baseResponseDTO);
            }

            SubTask subTask = subTaskOpt.get();
            String username = getUsernameFromRequest();
            Project project = subTask.getTask().getStage().getProject();

            boolean isPM = project.getProjectManager().getUsername().equals(username);
            boolean isAssignedMember = subTask.getProjectMember() != null &&
                    subTask.getProjectMember().getUsername().equals(username);

            if (!isPM && !isAssignedMember) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Not authorized to update this sub-task");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            subTask.setSubTaskName(requestDTO.getSubTaskName() != null ? requestDTO.getSubTaskName() : subTask.getSubTaskName());
            subTask.setDescription(requestDTO.getDescription() != null ? requestDTO.getDescription() : subTask.getDescription());
            subTask.setDueDate(requestDTO.getDueDate() != null ? requestDTO.getDueDate() : subTask.getDueDate());
            subTask.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : subTask.getStatus());
            subTask.setLabel(requestDTO.getLabel() != null ? requestDTO.getLabel() : subTask.getLabel());

            if (isPM) {
                subTask.setProjectMember(requestDTO.getProjectMember());
            }

            subTaskDb.save(subTask);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Sub-task updated successfully");
            baseResponseDTO.setData(new CrudResponseDTO(subTaskId, "SUCCESS"));
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> getDetailSubTask(String subTaskId) {

        var baseResponseDTO = new BaseResponseDTO<SubTaskDetailResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            Optional<SubTask> subTaskOpt = subTaskDb.findById(subTaskId);
            if (subTaskOpt.isEmpty()) {
                baseResponseDTO.setStatus(404);
                baseResponseDTO.setMessage("Sub-task not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(baseResponseDTO);
            }

            SubTask subTask = subTaskOpt.get();
            if (!isAuthorizedForTask(subTask.getTask().getTaskId())) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            SubTaskDetailResponseDTO detail = new SubTaskDetailResponseDTO(
                    subTask.getSubTaskId(),
                    subTask.getSubTaskName(),
                    subTask.getDescription(),
                    subTask.getDueDate(),
                    subTask.getStatus(),
                    subTask.getLabel(),
                    subTask.getProjectMember() != null ? subTask.getProjectMember().getFullName() : "Unassigned"
            );

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(detail);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManagerForTask(taskId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            subTaskDb.deleteById(subTaskId);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Sub-tasks deleted successfully");
            baseResponseDTO.setData(new CrudResponseDTO(taskId, "DELETED"));
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> reorderSubTask(String taskId, List<ReorderRequestDTO> requestDTOList) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManagerForTask(taskId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            for (ReorderRequestDTO req : requestDTOList) {
                Optional<SubTask> subTaskOpt = subTaskDb.findById(req.getId());
                if (subTaskOpt.isPresent()) {
                    SubTask subTask = subTaskOpt.get();
                    subTask.setOrder(req.getOrder());
                    subTaskDb.save(subTask);
                }
            }

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Sub-tasks reordered successfully");
            baseResponseDTO.setData(new CrudResponseDTO(taskId, "SUCCESS"));
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }
}
