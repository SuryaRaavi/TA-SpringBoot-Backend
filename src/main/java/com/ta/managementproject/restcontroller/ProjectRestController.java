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

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getProjects(
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
        return projectService.getAllProject(pageable, startDate, endDate, createdAt, updatedAt, keyword);
    }

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("/{projectId}")
    public ResponseEntity<?> getProjectDetail(@PathVariable("projectId") String projectId){
        return projectService.getProjectDetail(projectId);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PostMapping("")
    public ResponseEntity<?> addNewProject(@RequestBody CreateUpdateProjectRequestDTO requestDTO){
        return projectService.addNewProject(requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @PatchMapping("/{projectId}")
    public ResponseEntity<?> updateProject(@PathVariable("projectId") String projectId, @RequestBody CreateUpdateProjectRequestDTO requestDTO){
        return projectService.updateProject(projectId, requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable String projectId){
        return projectService.deleteProjectById(projectId);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @GetMapping("/{projectId}/join")
    public ResponseEntity<?> generateJoinCode(@PathVariable("projectId") String projectId){
        return projectService.generateJoinCode(projectId);
    }

    @PreAuthorize("hasRole('PROJECT_MEMBER')")
    @PostMapping("/join")
    public ResponseEntity<?> joinProject(@RequestParam String joinCode){
        return projectService.joinProject(joinCode);
    }
}
