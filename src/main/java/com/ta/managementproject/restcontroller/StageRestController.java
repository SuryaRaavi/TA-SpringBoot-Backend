package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.*;
import com.ta.managementproject.service.stage.StageService;
import jakarta.validation.Valid;
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

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 19, LOC: 114, COG: 13
    @GetMapping("")
    public ResponseEntity<?> getAllStage( // CYC: 1, LOC: 15, COG: 0
            @PathVariable String projectId,
            @PageableDefault(size = 20, page = 0) Pageable pageable,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedAt,

            @RequestParam(required = false) String keyword
    ){
        return stageService.getAllStage(projectId, pageable, createdAt, updatedAt, keyword); // CYC: 18, LOC: 99, COG: 13
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 10, LOC: 58, COG: 3
    @GetMapping("/{stageId}")
    public ResponseEntity<?> getStage(@PathVariable String projectId, @PathVariable String stageId){ // CYC: 1, LOC: 5, COG: 0
        return stageService.getStage(stageId); // CYC: 9, LOC: 53, COG: 3
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 11, LOC: 70, COG: 3
    @PostMapping("")
    public ResponseEntity<?> addNewStage(@PathVariable String projectId, @Valid @RequestBody CreateUpdateStageRequestDTO requestDTO){ // CYC: 1, LOC: 5, COG: 0
        return stageService.addNewStage(projectId, requestDTO);  // CYC: 10, LOC: 65, COG: 3
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 13, LOC: 67, COG: 5
    @PatchMapping("/{stageId}")
    public ResponseEntity<?> updateStage(@PathVariable String projectId, @PathVariable String stageId, @Valid @RequestBody CreateUpdateStageRequestDTO requestDTO){ // CYC: 1, LOC: 5, COG: 0
        return stageService.editStage(stageId, requestDTO); // CYC: 12, LOC: 62, COG: 5
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 13, LOC: 75, COG: 6
    @PatchMapping("/reorder")
    public ResponseEntity<?> reorderStage(@PathVariable String projectId, @Valid @RequestBody ReorderRequestDTO requestDTO){ // CYC: 1, LOC: 5, COG: 0
        return stageService.reorderStage(projectId, requestDTO); // CYC: 12, LOC: 70, COG: 6
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 12, LOC: 71, COG: 4
    @DeleteMapping("/{stageId}")
    public ResponseEntity<?> deleteStage(@PathVariable String projectId, @PathVariable String stageId){ // CYC: 1, LOC: 5, COG: 0
        return stageService.deleteStageById(projectId, stageId); //  CYC: 11, LOC: 66, COG: 4
    }
}

