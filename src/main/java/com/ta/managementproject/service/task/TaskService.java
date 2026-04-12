package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

public interface TaskService {

    ResponseEntity<?> getAllTask(int page, int size, String stageId, Instant startDate, Instant endDate, String sortingColumn, String orderDirection);

    ResponseEntity<?> searchTask(int page, int size, String stageId, String query, String sortingColumn, String orderDirection);

    ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailTask(String taskId);

    ResponseEntity<?> deleteTaskById(String stageId, String taskId);

    ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO);

    ResponseEntity<?> updateTaskStatus(String taskId, CreateUpdateTaskRequestDTO requestDTO);
}
