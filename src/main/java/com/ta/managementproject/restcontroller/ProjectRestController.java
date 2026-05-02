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

    // CYC: 29, LOC: 174
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getProjects( // CYC: 1, LOC: 20
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
        return projectService.getAllProject(pageable, startDate, endDate, createdAt, updatedAt, keyword); // CYC: 28, LOC: 154
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')") // Total CYC: 9, LOC: 52
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectDetail(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5
        return projectService.getProjectDetail(projectId); // CYC: 8, LOC: 47
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 5, LOC: 35
    @PostMapping("")
    public ResponseEntity<?> addNewProject(@RequestBody CreateUpdateProjectRequestDTO requestDTO){ // CYC: 1, LOC: 5
        return projectService.addNewProject(requestDTO); // CYC: 4, LOC: 30
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 15, LOC: 52
    @PatchMapping("/{projectId}")
    public ResponseEntity<?> updateProject(@PathVariable("projectId") String projectId, @RequestBody CreateUpdateProjectRequestDTO requestDTO){
        // CYC: 1, LOC: 5
        return projectService.updateProject(projectId, requestDTO); // CYC: 14, LOC: 47
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 9, LOC: 43
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable String projectId){ // CYC: 1, LOC: 5
        return projectService.deleteProjectById(projectId); // CYC: 8, LOC: 38
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 12, LOC: 60
    @GetMapping("/{projectId}/join")
    public ResponseEntity<?> generateJoinCode(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5
        return projectService.generateJoinCode(projectId); // CYC: 11, LOC: 55
    }

    @PreAuthorize("hasRole('PROJECT_MEMBER')") // Total CYC: 9, LOC: 44
    @PostMapping("/join")
    public ResponseEntity<?> joinProject(@RequestParam String joinCode){ // CYC: 1, LOC: 5
        return projectService.joinProject(joinCode); // CYC: 8, LOC: 39
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')") // Total CYC: 10, LOC: 39
    @PostMapping("/{projectId}/cancel-project")
    public ResponseEntity<?> cancelProject(@PathVariable("projectId") String projectId){ // CYC: 1, LOC: 5
        return projectService.cancelProject(projectId); // CYC: 9, LOC: 34
    }
}
