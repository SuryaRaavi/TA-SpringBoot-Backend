package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateSubTaskRequestDTO;
import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface SubTaskService {
    ResponseEntity<?> getAllSubTask(int page, int size, String taskId, LocalDate startDate, LocalDate endDate);

    ResponseEntity<?> searchSubTask(int page, int size, String taskId, String query);

    ResponseEntity<?> addNewSubTask(String taskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> updateSubTask(String subTaskId, CreateUpdateSubTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailSubTask(String subTaskId);

    ResponseEntity<?> deleteSubTaskById(String taskId, String subTaskId);

    ResponseEntity<?> reorderSubTask(String taskId, List<ReorderRequestDTO> requestDTOList);
}
