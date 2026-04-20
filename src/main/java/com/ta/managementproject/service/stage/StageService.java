package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface StageService {
    ResponseEntity<?> getAllStage(String projectId);
    ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO);
    ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO);
    ResponseEntity<BaseResponseDTO<ProgressResponseDTO>> getStageStatistics(String stageId);
    ResponseEntity<?> reorderStage(String projectId, ReorderRequestDTO requestDTO);

    ResponseEntity<?> deleteStageById(String projectId, String stageId);
}
