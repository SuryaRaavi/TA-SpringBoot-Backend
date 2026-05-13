package com.ta.managementproject.service.project;

import com.ta.managementproject.dto.request.CreateUpdateProjectRequestDTO;
import com.ta.managementproject.dto.request.JoinProjectRequestDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;

public interface ProjectService {
    ResponseEntity<?> getAllProject(Pageable pageable, LocalDate startDate, LocalDate endDate, LocalDate createdAt, LocalDate updatedAt, String keyword);
    ResponseEntity<?> addNewProject(CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> updateProject(String projectId, CreateUpdateProjectRequestDTO request);
    ResponseEntity<?> getProjectDetail(String projectId);
    ResponseEntity<?> deleteProjectById(String projectId);

    ResponseEntity<?> generateJoinCode(String kodeProyek);

    ResponseEntity<?> joinProject(JoinProjectRequestDTO joinProjectRequestDTO);

    ResponseEntity<?> cancelProject(String projectId);
}
