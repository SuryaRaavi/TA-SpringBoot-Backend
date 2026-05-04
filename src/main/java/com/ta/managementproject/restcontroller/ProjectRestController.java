package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectRestController {

    @Autowired
    private ProjectService projectService;

    // CYC: 25, LOC: 149, COG: 17
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getProjects( // CYC: 1, LOC: 20, COG: 0
            @PageableDefault(size = 10, page = 0) Pageable pageable,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate createdAt,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate updatedAt,

            @RequestParam(required = false) String keyword
    ){
        return projectService.getAllProject(pageable, startDate, endDate, createdAt, updatedAt, keyword); // CYC: 24, LOC: 129, COG: 17
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 10, LOC: 62, COG: 3
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectDetail(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5, COG: 0
        return projectService.getProjectDetail(projectId); // CYC: 9, LOC: 57, COG: 3
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 6, LOC: 55, COG: 1
    @PostMapping("")
    public ResponseEntity<?> addNewProject(@RequestBody CreateUpdateProjectRequestDTO requestDTO){ // CYC: 1, LOC: 5, COG: 0
        return projectService.addNewProject(requestDTO); // CYC: 5, LOC: 50, COG: 1
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 15, LOC: 72, COG: 11
    @PatchMapping("/{projectId}")
    public ResponseEntity<?> updateProject(@PathVariable("projectId") String projectId, @RequestBody CreateUpdateProjectRequestDTO requestDTO){
        // CYC: 1, LOC: 5, COG: 0
        return projectService.updateProject(projectId, requestDTO); // CYC: 14, LOC: 67, COG: 11
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 9, LOC: 43, COG: 2
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable String projectId){ // CYC: 1, LOC: 5, COG: 0
        return projectService.deleteProjectById(projectId); // CYC: 8, LOC: 38, COG: 2
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 14, LOC: 68, COG: 6
    @GetMapping("/{projectId}/join")
    public ResponseEntity<?> generateJoinCode(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5, COG: 0
        return projectService.generateJoinCode(projectId); // CYC: 13, LOC: 63, COG: 6
    }

    @PreAuthorize("hasRole('PROJECT_MEMBER')") // Total CYC: 11, LOC: 53, COG: 5
    @PostMapping("/join")
    public ResponseEntity<?> joinProject(@RequestParam String joinCode){ // CYC: 1, LOC: 5, COG: 0
        return projectService.joinProject(joinCode); // CYC: 10, LOC: 48, COG: 5
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 10, LOC: 59, COG: 2
    @PostMapping("/{projectId}/cancel-project")
    public ResponseEntity<?> cancelProject(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5, COG: 0
        return projectService.cancelProject(projectId); // CYC: 9, LOC: 54, COG: 2
    }
}
