package com.ta.managementproject.service.task;

import com.ta.managementproject.dto.request.CreateUpdateTaskRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

public interface TaskService {

    ResponseEntity<?> getAllTask(int page, int size, String stageId, LocalDate startDate, LocalDate endDate);

    ResponseEntity<?> searchTask(int page, int size, String stageId, String query);

    ResponseEntity<?> addNewTask(String stageId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> updateTask(String taskId, CreateUpdateTaskRequestDTO requestDTO);

    ResponseEntity<?> getDetailTask(String taskId);

    ResponseEntity<?> deleteSelectedTask(String stageId, List<DeleteRequestDTO> requestDTOList);

    ResponseEntity<?> reorderTask(String stageId, List<ReorderRequestDTO> requestDTOList);
}
