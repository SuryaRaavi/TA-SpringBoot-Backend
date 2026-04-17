package com.ta.managementproject.restcontroller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
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
            @PageableDefault(size = 10, page = 0) Pageable pageable,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Integer priority,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Integer order,

            @RequestParam(required = false) String keyword
    ){
        return taskService.getAllTask(
                pageable,
                stageId,
                dueDate,
                createdAt,
                updatedAt,
                priority,
                order,
                keyword
        );
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
            @RequestBody ReorderRequestDTO requestDTO
    ){
        return taskService.reorderTask(stageId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.deleteTaskById(stageId, taskId);
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateTaskStatus(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTaskStatus(taskId, requestDTO);
    }
}