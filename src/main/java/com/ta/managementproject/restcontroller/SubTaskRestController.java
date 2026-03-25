package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.*;
import com.ta.managementproject.service.task.SubTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
        /**TODO:
         Implementasikan service subTaskService.getAllSubTask sama subTaskService.searchSubTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateSubTaskRequestDTO requestDTO
    ){
        /**TODO:
         Implementasikan service subTaskService.addNewSubTask
         */
        return null;
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
        /**TODO:
         Implementasikan service subTaskService.updateSubTask
         */
        return null;
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("/{subTaskId}")
    public ResponseEntity<?> getDetailSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @PathVariable String subTaskId
    ){
        /**TODO:
         Implementasikan service subTaskService.getDetailSubTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody List<ReorderRequestDTO> requestDTOs
    ){
        /**TODO:
         Implementasikan service subTaskService.reorderSubTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("")
    public ResponseEntity<?> deleteSubTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody DeleteRequestDTO requestDTO
    ){
        /**TODO:
         Implementasikan service subTaskService.deleteSelectedSubTask
         */
        return null;
    }
}
