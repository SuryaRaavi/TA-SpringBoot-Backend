package com.ta.managementproject.restcontroller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.service.task.TaskService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages/{stageId}/tasks")
public class TaskRestController{
    @Autowired
    private TaskService taskService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getAllTask(
            @PathVariable String projectId,
            @PathVariable String stageId,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "4") int size,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) String query
    ){
        if (query != null && !query.isEmpty()) {
            return taskService.searchTask(page, size, stageId, query);
        }
        return taskService.getAllTask(page, size, stageId, startDate, endDate);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.addNewTask(stageId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTask(taskId, requestDTO);
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getDetailTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.getDetailTask(taskId);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody List<ReorderRequestDTO> requestDTOs
    ){
        return taskService.reorderTask(stageId, requestDTOs);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.deleteTaskById(stageId, taskId);
    }
}