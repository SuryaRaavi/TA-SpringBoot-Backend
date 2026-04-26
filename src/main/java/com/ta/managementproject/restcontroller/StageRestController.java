package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.service.stage.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages")
public class StageRestController {
    @Autowired
    private StageService stageService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getAllStage(@PathVariable String projectId){
        return stageService.getAllStage(projectId);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewStage(@PathVariable String projectId, @RequestBody CreateUpdateStageRequestDTO requestDTO){
        return stageService.addNewStage(projectId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{stageId}")
    public ResponseEntity<?> updateStage(@PathVariable String projectId, @PathVariable String stageId, @RequestBody CreateUpdateStageRequestDTO requestDTO){
        return stageService.editStage(stageId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderStage(@PathVariable String projectId, @RequestBody ReorderRequestDTO requestDTO){
        return stageService.reorderStage(projectId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @DeleteMapping("/{stageId}")
    public ResponseEntity<?> deleteStage(@PathVariable String projectId, @PathVariable String stageId){
        return stageService.deleteStageById(projectId, stageId);
    }
}

