package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SubTaskService {
    ResponseEntity<?> getAllSubTask(String taskId,
                                    LocalDate dueDate,
                                    LocalDate createdAt,
                                    LocalDate updatedAt,
                                    Integer order,
                                    String keyword,
                                    Pageable pageable
    );

    ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailSubTask(String subTaskId);

    ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId);

    ResponseEntity<?> reorderSubTask(String taskId, ReorderRequestDTO requestDTO);

    ResponseEntity<?> updateSubTaskStatus(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO);
}
