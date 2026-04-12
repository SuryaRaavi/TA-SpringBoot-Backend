package com.ta.managementproject.restcontroller;

import java.time.Instant;
import java.util.List;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getAllSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "4") int size,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Instant startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            Instant endDate,

            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "order") String sortingColumn,
            @RequestParam(required = false, defaultValue = "ascending") String orderDirection
    ){
        if (query != null && !query.isEmpty()) {
            return subTaskService.searchSubTask(page, size, taskId, query, sortingColumn, orderDirection);
        }
        return subTaskService.getAllSubTask(page, size, taskId, startDate, endDate, sortingColumn, orderDirection);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.addNewSubTask(taskId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{subTaskId}")
    public ResponseEntity<?> updateSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTask(subTaskId, requestDTO);
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("/{subTaskId}")
    public ResponseEntity<?> getDetailSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.getDetailSubTask(subTaskId);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody ReorderRequestDTO requestDTO
    ){
        return subTaskService.reorderSubTask(taskId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @DeleteMapping("/{subTaskId}")
    public ResponseEntity<?> deleteSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        return subTaskService.deleteSubTaskById(taskId, subTaskId);
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @PatchMapping("/update-status")
    public ResponseEntity<?> updateSubTaskStatus(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        return subTaskService.updateSubTaskStatus(subTaskId, requestDTO);
    }
}
