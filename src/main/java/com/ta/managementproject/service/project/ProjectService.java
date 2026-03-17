package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface ProjectService {
    ResponseEntity<?> getAllProject(int page, int size, LocalDate startDate, LocalDate endDate);
    ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> getProjectDetail(String projectId);
    ResponseEntity<?> deleteProject(List<DeleteRequestDTO> request);
    ResponseEntity<?> searchProject(int page, int size, String parameter);

    ResponseEntity<?> generateJoinCode(String kodeProyek);

    ResponseEntity<?> joinProject(String joinCode);

    ResponseEntity<?> getUsersInProject(String projectId, int page, int size, Integer role);

    ResponseEntity<?> searchUserInProject(String projectId, String searchQuery, int page, int size);

    ResponseEntity<?> deleteProjectMemberFromProject(String projectId, String username);

    ResponseEntity<?> getProjectStatistic(String projectId);
}
