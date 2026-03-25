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
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.TaskDetailResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.repository.MemberInProjectDb;
import com.ta.managementproject.repository.ProjectDb;
import com.ta.managementproject.repository.ProjectManagerDb;
import com.ta.managementproject.repository.ProjectMemberDb;
import com.ta.managementproject.repository.StageDb;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.security.util.JwtUtils;

import jakarta.servlet.http.HttpServletRequest;


@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    @Autowired
    private ProjectManagerDb projectManagerDb;

    @Autowired
    private ProjectMemberDb projectMemberDb;

    @Autowired
    private TaskDb taskDb;

    @Autowired
    private ProjectDb projectDb;

    @Autowired
    private StageDb stageDb;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private MemberInProjectDb memberInProjectDb;

    
    private String getUsernameFromToken() {

        return jwtUtils.getUserNameFromRequest(request);

    }

    private boolean isAuthorizedForStage(String stageId) {

        String username = getUsernameFromToken();
        Optional<Stage> stageOpt = stageDb.findById(stageId);
        if (stageOpt.isEmpty()) return false;

        Project project = stageOpt.get().getProject();
        
        if (project.getProjectManager().getUsername().equals(username)) return true;

        return memberInProjectDb.findByProjectIdAndUsername(project.getProjectId(), username) != null;

    }

    private boolean isProjectManager(String stageId) {

        String username = getUsernameFromToken();
        Optional<Stage> stageOpt = stageDb.findById(stageId);

        if (stageOpt.isEmpty()) return false;
        return stageOpt.get().getProject().getProjectManager().getUsername().equals(username);

    }

    @Override
    public ResponseEntity<?> getAllTask(int page, int size, String stageId, LocalDate startDate, LocalDate endDate) {

        var baseResponseDTO = new BaseResponseDTO<Page<TaskResponseDTO>>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isAuthorizedForStage(stageId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<TaskResponseDTO> tasks;

            if (startDate != null && endDate != null) {
                tasks = taskDb.findTaskByStageIdAndDueDate(stageId, startDate, endDate, pageable);
            } else {
                tasks = taskDb.findTaskByStageId(stageId, pageable);
            }

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Success");
            baseResponseDTO.setData(tasks);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> searchTask(int page, int size, String stageId, String query) {

        var baseResponseDTO = new BaseResponseDTO<Page<TaskResponseDTO>>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isAuthorizedForStage(stageId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<TaskResponseDTO> tasks = taskDb.searchTaskByQuery(stageId, query, pageable);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Success");
            baseResponseDTO.setData(tasks);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManager(stageId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Only Project Managers can add tasks");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            Stage stage = stageDb.findById(stageId).orElseThrow();
            Integer currentTotal = taskDb.getTotalTask(stageId);

            Task newTask = Task.builder()
                    .taskName(requestDTO.getTaskName())
                    .description(requestDTO.getDescription())
                    .priority(requestDTO.getPriority())
                    .dueDate(requestDTO.getDueDate())
                    .status(requestDTO.getStatus())
                    .projectMember(requestDTO.getProjectMember())
                    .hasSubTask(requestDTO.isHasSubTask())
                    .stage(stage)
                    .order(currentTotal + 1)
                    .isDeleted(false)
                    .build();

            taskDb.save(newTask);

            baseResponseDTO.setStatus(201);
            baseResponseDTO.setMessage("Task created successfully");
            baseResponseDTO.setData(new CrudResponseDTO(newTask.getTaskId(), java.time.LocalDateTime.now().toString()));
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO) {

        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            Task task = taskDb.findById(taskId).orElse(null);
            if (task == null) {
                baseResponseDTO.setStatus(404);
                baseResponseDTO.setMessage("Task not found");
                return ResponseEntity.status(404).body(baseResponseDTO);
            }

            if (!isAuthorizedForStage(task.getStage().getStageId())) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            task.setTaskName(requestDTO.getTaskName());
            task.setDescription(requestDTO.getDescription());
            task.setPriority(requestDTO.getPriority());
            task.setDueDate(requestDTO.getDueDate());
            task.setStatus(requestDTO.getStatus());
            task.setProjectMember(requestDTO.getProjectMember());
            task.setHasSubTask(requestDTO.isHasSubTask());

            taskDb.save(task);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Task updated successfully");
            baseResponseDTO.setData(new CrudResponseDTO(taskId, java.time.LocalDateTime.now().toString()));
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
      }


    @Override
    public ResponseEntity<?> getDetailTask(String taskId) {

        var baseResponseDTO = new BaseResponseDTO<TaskDetailResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            Task task = taskDb.findById(taskId).orElse(null);
            if (task == null) {
                baseResponseDTO.setStatus(404);
                baseResponseDTO.setMessage("Task not found");
                return ResponseEntity.status(404).body(baseResponseDTO);
            }

            if (!isAuthorizedForStage(task.getStage().getStageId())) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

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

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Success");
            baseResponseDTO.setData(response);
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> deleteTaskById(String stageId, String taskId) {

        var baseResponseDTO = new BaseResponseDTO<Object>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManager(stageId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            taskDb.deleteById(taskId);

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Tasks deleted successfully");
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }

    @Override
    public ResponseEntity<?> reorderTask(String stageId, List<ReorderRequestDTO> requestDTOList) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();
        baseResponseDTO.setTimestamp(new Date());

        try {
            if (!isProjectManager(stageId)) {
                baseResponseDTO.setStatus(403);
                baseResponseDTO.setMessage("Access Denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(baseResponseDTO);
            }

            for (ReorderRequestDTO req : requestDTOList) {
                Optional<Task> taskOpt = taskDb.findById(req.getId());
                if (taskOpt.isPresent()) {
                    Task task = taskOpt.get();
                    task.setOrder(req.getOrder());
                    taskDb.save(task);
                }
            }

            baseResponseDTO.setStatus(200);
            baseResponseDTO.setMessage("Tasks reordered successfully");
            return ResponseEntity.ok(baseResponseDTO);
        }catch(Exception e){
            baseResponseDTO.setStatus(500);
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return ResponseEntity.internalServerError().body(baseResponseDTO);
        }
    }
}
