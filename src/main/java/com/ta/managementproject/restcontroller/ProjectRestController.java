package com.ta.managementproject.restcontroller;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.service.project.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectRestController {

    @Autowired
    private ProjectService projectService;

    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'PROJECT_MEMBER')")
    @GetMapping("")
    public ResponseEntity<?> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "4") int size,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false) String search
    ){
        if (search == null){
            return projectService.getAllProject(page, size, startDate, endDate);
        }else{
            return projectService.searchProject(page, size, search);
        }
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
    @DeleteMapping("")
    public ResponseEntity<?> deleteSelectedProject(@RequestBody List<DeleteRequestDTO> requestDTO){
        return projectService.deleteProject(requestDTO);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @GetMapping("/{projectId}/join")
    public ResponseEntity<?> generateJoinCode(@PathVariable("projectId") String projectId){
        return projectService.generateJoinCode(projectId);
    }

    @PreAuthorize("hasRole('HSE')")
    @PostMapping("/join")
    public ResponseEntity<?> joinProject(@RequestParam String joinCode){
        return projectService.joinProject(joinCode);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @GetMapping("/{projectId}/users")
    public ResponseEntity<?> getUsersInProject(
            @PathVariable("projectId") String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "4") int size,
            @RequestParam(required = false) Integer role,
            @RequestParam(required = false) String searchQuery
    ){
        if (searchQuery != null){
            return projectService.searchUserInProject(projectId, searchQuery, page, size);
        }
        return projectService.getUsersInProject(projectId, page, size, role);
    }

    @PreAuthorize("hasRole('PROJECT_MANAGER')")
    @DeleteMapping("/{projectId}/users/{username}")
    public ResponseEntity<?> deleteProjectMemberFromProject(
            @PathVariable("projectId") String projectId,
            @PathVariable String username
    ){
        return projectService.deleteProjectMemberFromProject(projectId, username);
    }
}
