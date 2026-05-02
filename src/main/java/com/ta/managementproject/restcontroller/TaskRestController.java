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

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 27, LOC: 174
    @GetMapping("")
    public ResponseEntity<?> getAllTask( // CYC: 1, LOC: 34
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
        return taskService.getAllTask( // CYC: 26, LOC:  140
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

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 14, LOC: 97
    @PostMapping("")
    public ResponseEntity<?> addNewTask( // CYC: 1, LOC: 9
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.addNewTask(stageId, requestDTO); // CYC: 13, LOC: 88
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 9, LOC: 55
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTask(taskId, requestDTO); // CYC: 8, LOC: 45
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 10, LOC: 62
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getDetailTask( // CYC: 1, LOC: 9
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.getDetailTask(taskId); // CYC: 9, LOC: 53
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 14, LOC: 70
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask( // CYC: 1, LOC: 9
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody ReorderRequestDTO requestDTO
    ){
        return taskService.reorderTask(stageId, requestDTO); // CYC: 13, LOC: 61
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 15, LOC: 91
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask( // CYC: 1, LOC: 9
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.deleteTaskById(stageId, taskId); // CYC: 14, LOC: 82
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 14, LOC: 100
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateTaskStatus( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTaskStatus(taskId, requestDTO); // CYC: 13, LOC: 90
    }
}