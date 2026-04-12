package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

public interface SubTaskService {
    ResponseEntity<?> getAllSubTask(int page, int size, String taskId, Instant startDate, Instant endDate, String sortingColumn, String orderDirection);

    ResponseEntity<?> searchSubTask(int page, int size, String taskId, String query, String sortingColumn, String orderDirection);

    ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailSubTask(String subTaskId);

    ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId);

    ResponseEntity<?> reorderSubTask(String taskId, ReorderRequestDTO requestDTO);

    ResponseEntity<?> updateSubTaskStatus(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO);
}
