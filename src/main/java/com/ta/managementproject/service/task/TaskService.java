package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface TaskService {

    ResponseEntity<?> getAllTask(Pageable pageable,
                                 String stageId,
                                 LocalDate dueDate,
                                 LocalDate createdAt,
                                 LocalDate updatedAt,
                                 Integer priority,
                                 Integer order,
                                 String keyword);

    ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailTask(String taskId);

    ResponseEntity<?> deleteTaskById(String stageId, String taskId);

    ResponseEntity<?> reorderTask(String stageId, ReorderRequestDTO requestDTO);

    ResponseEntity<?> updateTaskStatus(String taskId, CreateUpdateTaskRequestDTO requestDTO);
}
