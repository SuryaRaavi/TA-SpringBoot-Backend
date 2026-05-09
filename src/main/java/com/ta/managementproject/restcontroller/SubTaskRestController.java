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
import com.ta.managementproject.service.task.SubTaskService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages/{stageId}/tasks/{taskId}/subtasks")
public class SubTaskRestController {
    @Autowired
    private SubTaskService subTaskService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 22, LOC: 174, COG: 15
    @GetMapping("")
    public ResponseEntity<?> getAllSubTask( // CYC: 1, LOC: 29, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
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

            @RequestParam(required = false) Integer order,

            @RequestParam(required = false) String keyword
    ){
        return subTaskService.getAllSubTask( // CYC: 21, LOC: 145, COG: 15
                taskId,
                dueDate,
                createdAt,
                updatedAt,
                order,
                keyword,
                pageable
        );
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 20, LOC: 144, COG: 10
    @PostMapping("")
    public ResponseEntity<?> addNewSubTask( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @Valid @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.addNewSubTask(taskId, requestDTO); // CYC: 19, LOC: 134, COG: 10
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 17, LOC: 73, COG: 9
    @PatchMapping("/{subTaskId}")
    public ResponseEntity<?> updateSubTask( // CYC: 1, LOC: 11, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @Valid @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTask(subTaskId, requestDTO); // CYC: 16, LOC: 61, COG: 9
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 11, LOC: 62, COG: 4
    @GetMapping("/{subTaskId}")
    public ResponseEntity<?> getDetailSubTask( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.getDetailSubTask(subTaskId); // CYC: 10, LOC: 52, COG: 4
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 16, LOC: 87, COG: 8
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @Valid @RequestBody ReorderRequestDTO requestDTO
    ){
        return subTaskService.reorderSubTask(taskId, requestDTO); // CYC: 15, LOC: 77, COG: 8
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 18, LOC: 118, COG: 9
    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<?> deleteSubTask( // CYC: 1, LOC: 10, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.deleteSubTaskById(taskId, subTaskId); // CYC: 17, LOC: 108, COG: 9
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 21, LOC: 138, COG: 12
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateSubTaskStatus( // CYC: 1, LOC: 11, COG: 0
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @Valid @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTaskStatus(subTaskId, requestDTO); // CYC: 20, LOC: 127, COG: 12
    }
}
