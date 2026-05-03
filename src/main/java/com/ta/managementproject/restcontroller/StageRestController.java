package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.service.stage.StageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/stages")
public class StageRestController {
    @Autowired
    private StageService stageService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 11, LOC: 51
    @GetMapping("")
    public ResponseEntity<?> getAllStage(
            @PathVariable String projectId,
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedAt,

            @RequestParam(required = false) String keyword

    ){ // CYC: 1, LOC: 5
        return stageService.getAllStage(projectId, pageable, createdAt, updatedAt, keyword); // CYC: 10, LOC: 46
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 9, LOC: 52
    @GetMapping("/{stageId}")
    public ResponseEntity<?> getStage(@PathVariable String projectId, @PathVariable String stageId){ // CYC: 1, LOC: 5
        return stageService.getStage(stageId); // CYC: 8, LOC: 47
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 10, LOC: 54
    @PostMapping("")
    public ResponseEntity<?> addNewStage(@PathVariable String projectId, @RequestBody CreateUpdateStageRequestDTO requestDTO){ // CYC: 1, LOC: 5
        return stageService.addNewStage(projectId, requestDTO);  // CYC: 9, LOC: 49
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 12, LOC: 51
    @PatchMapping("/{stageId}")
    public ResponseEntity<?> updateStage(@PathVariable String projectId, @PathVariable String stageId, @RequestBody CreateUpdateStageRequestDTO requestDTO){ // CYC: 1, LOC: 5
        return stageService.editStage(stageId, requestDTO); // CYC: 11, LOC: 46
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 10, LOC: 59
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderStage(@PathVariable String projectId, @RequestBody ReorderRequestDTO requestDTO){ // CYC: 1, LOC: 5
        return stageService.reorderStage(projectId, requestDTO); // CYC: 9, LOC: 54
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 13, LOC: 71
    @DeleteMapping("/{stageId}")
    public ResponseEntity<?> deleteStage(@PathVariable String projectId, @PathVariable String stageId){ // CYC: 1, LOC: 5
        return stageService.deleteStageById(projectId, stageId); // CYC: 12, LOC: 66
    }
}

