package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.service.task.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
        /**TODO:
         Implementasikan service taskService.getAllTask sama taskService.searchTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        /**TODO:
         Implementasikan service taskService.addNewTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId,
            @RequestBody CreateUpdateTaskRequestDTO requestDTO
    ){
        /**TODO:
         Implementasikan service taskService.updateTask
         */
        return null;
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getDetailTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @PathVariable String taskId
    ){
        /**TODO:
         Implementasikan service taskService.getDetailTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody List<ReorderRequestDTO> requestDTOs
    ){
        /**TODO:
         Implementasikan service taskService.reorderTask
         */
        return null;
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("")
    public ResponseEntity<?> deleteSelectedTask(
            @PathVariable String projectId,
            @PathVariable String stageId,
            @RequestBody DeleteRequestDTO requestDTO
    ){
        /**TODO:
         Implementasikan service taskService.deleteSelectedTask
         */
        return null;
    }
}