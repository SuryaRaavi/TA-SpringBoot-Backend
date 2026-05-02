package com.ta.managementproject.restcontroller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.service.task.SubTaskService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages/{stageId}/tasks/{taskId}/subtasks")
public class SubTaskRestController {
    @Autowired
    private SubTaskService subTaskService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 27, LOC: 183
    @GetMapping("")
    public ResponseEntity<?> getAllSubTask( // CYC: 1, LOC: 29
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
        return subTaskService.getAllSubTask( // CYC: 26, LOC: 154
                taskId,
                dueDate,
                createdAt,
                updatedAt,
                order,
                keyword,
                pageable
        );
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 19, LOC: 127
    @PostMapping("")
    public ResponseEntity<?> addNewSubTask( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.addNewSubTask(taskId, requestDTO); // CYC: 18, LOC: 117
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 15, LOC: 56
    @PatchMapping("/{subTaskId}")
    public ResponseEntity<?> updateSubTask( // CYC: 1, LOC: 11
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTask(subTaskId, requestDTO); // CYC: 14, LOC: 45
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 10, LOC: 56
    @GetMapping("/{subTaskId}")
    public ResponseEntity<?> getDetailSubTask( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.getDetailSubTask(subTaskId); // CYC: 9, LOC: 46
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 14, LOC: 71
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody ReorderRequestDTO requestDTO
    ){
        return subTaskService.reorderSubTask(taskId, requestDTO); // CYC: 13, LOC: 61
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 18, LOC: 117
    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<?> deleteSubTask( // CYC: 1, LOC: 10
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.deleteSubTaskById(taskId, subTaskId); // CYC: 17, LOC: 107
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 19, LOC: 121
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateSubTaskStatus( // CYC: 1, LOC: 11
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTaskStatus(subTaskId, requestDTO); // CYC: 18, LOC: 110
    }
}
