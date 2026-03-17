package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StageService {
    ResponseEntity<?> getAllStage(String projectId);
    ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO);
    ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO);
    ResponseEntity<?> getStageStatistics(String stageId);
    ResponseEntity<?> reorderStage(String projectId, List<ReorderRequestDTO> requestDTOS);

    ResponseEntity<?> deleteStage(String projectId, DeleteRequestDTO requestDTO);
}
