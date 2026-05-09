package com.ta.managementproject.restcontroller;

import java.time.LocalDate;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ta.managementproject.dto.request.*;
import com.ta.managementproject.service.task.TaskService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages/{stageId}/tasks")
public class TaskRestController{
    @Autowired
    private TaskService taskService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 24, LOC: 188, COG: 16
    @GetMapping("")
    public ResponseEntity<?> getAllTask( // CYC: 1, LOC: 34, COG: 0
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
        return taskService.getAllTask( // CYC: 23, LOC: 154, COG: 16
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

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 16, LOC: 122, COG: 6
    @PostMapping("")
    public ResponseEntity<?> addNewTask( // CYC: 1, LOC: 9, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @Valid @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.addNewTask(stageId, requestDTO); // CYC: 15, LOC: 113, COG: 6
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 12, LOC: 77, COG: 4
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @Valid @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTask(taskId, requestDTO); // CYC: 11, LOC: 67, COG: 4
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 11, LOC: 68, COG: 4
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getDetailTask( // CYC: 1, LOC: 9, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.getDetailTask(taskId); // CYC: 10, LOC: 59, COG: 4
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 16, LOC: 92, COG: 8
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask( // CYC: 1, LOC: 9, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @Valid @RequestBody ReorderRequestDTO requestDTO
    ){
        return taskService.reorderTask(stageId, requestDTO); // CYC: 15, LOC: 83, COG: 8
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 14, LOC: 91, COG: 5
    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask( // CYC: 1, LOC: 9, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        return taskService.deleteTaskById(stageId, taskId); // CYC: 13, LOC: 82, COG: 5
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 18, LOC: 119, COG: 8
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateTaskStatus( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @Valid @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        return taskService.updateTaskStatus(taskId, requestDTO); // CYC: 17, LOC: 109, COG: 8
    }
}