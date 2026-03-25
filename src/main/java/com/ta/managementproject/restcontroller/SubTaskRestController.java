package com.ta.managementproject.restcontroller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
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
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) String query
    ){
        if (query != null && !query.isEmpty()) {
            return subTaskService.searchSubTask(page, size, taskId, query);
        }
        return subTaskService.getAllSubTask(page, size, taskId, startDate, endDate);
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
            @RequestBody List<ReorderRequestDTO> requestDTOs
    ){
        return subTaskService.reorderSubTask(taskId, requestDTOs);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("")
    public ResponseEntity<?> deleteSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody List<DeleteRequestDTO> requestDTOList
    ){
        return subTaskService.deleteSelectedSubTask(taskId, requestDTOList);
    }
}
