package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

public interface ProjectService {
    ResponseEntity<?> getAllProject(int page, int size, Instant startDate, Instant endDate, String sortingColumn, String orderDirection);
    ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> getProjectDetail(String projectId);
    ResponseEntity<?> deleteProjectById(String projectId);
    ResponseEntity<?> searchProject(int page, int size, String parameter, String sortingColumn, String orderDirection);

    ResponseEntity<?> generateJoinCode(String kodeProyek);

    ResponseEntity<?> joinProject(String joinCode);

    ResponseEntity<?> getUsersInProject(String projectId, int page, int size, Integer role, String sortingColumn, String orderDirection);

    ResponseEntity<?> searchUserInProject(String projectId, String searchQuery, int page, int size, String sortingColumn, String orderDirection);

    ResponseEntity<?> deleteProjectMemberFromProject(String projectId, String username);

    ResponseEntity<?> getProjectStatistics(String projectId);
}
