package com.ta.managementproject.service.stage;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.request.CreateUpdateStageRequestDTO;
import com.ta.managementproject.dto.request.DeleteRequestDTO;
import com.ta.managementproject.dto.request.ReorderRequestDTO;
import com.ta.managementproject.dto.response.CrudResponseDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.User;
import com.ta.managementproject.enums.Role;
import com.ta.managementproject.repository.ProjectDb;
import com.ta.managementproject.repository.StageDb;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.repository.UserDb;
import com.ta.managementproject.security.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@Transactional
public class StageServiceImpl implements StageService{
    @Autowired
    private StageDb stageDb;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDb userDb;

    @Autowired
    private ProjectDb projectDb;

    @Autowired
    private TaskDb taskDb;

    @Override
    public ResponseEntity<?> getAllStage(String projectId) {
        var baseResponseDTO = new BaseResponseDTO<List<StageResponseDTO>>();

        try{
            Role userRole = jwtUtils.getUserRoleFromJwtToken(request);

            String username = jwtUtils.getUserNameFromRequest(request);

            if (userRole == Role.PROJECT_MANAGER){
                baseResponseDTO.setData(stageDb.findAllByProjectIdAndUsernamePM(username, projectId));
            }else{
                baseResponseDTO.setData(stageDb.findAllByProjectIdAndUsernamePMB(username, projectId));
            }

            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> addNewStage(String projectId, CreateUpdateStageRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Project project = projectDb.findByProjectId(projectId);

            String username = jwtUtils.getUserNameFromRequest(request);

            if (!project.getProjectManager().getUsername().equals(username)){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            Stage newStage = Stage.builder()
                    .stageId(generateStageId())
                    .stageName(requestDTO.getStageName())
                    .description(requestDTO.getDescription())
                    .createdAt(LocalDateTime.now())
                    .order(stageDb.getTotalStageByProject(projectId))
                    .project(project)
                    .build();

            stageDb.save(newStage);

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "New stage has been added!"));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> editStage(String stageId, CreateUpdateStageRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Stage stage = stageDb.findByStageId(stageId);
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

            if (!stage.getProject().getProjectManager().getUsername().equals(user.getUsername())){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            stageDb.save(
                    stage.toBuilder()
                            .stageName(requestDTO.getStageName() == null ? stage.getStageName() : requestDTO.getStageName())
                            .description(requestDTO.getDescription() == null ? stage.getDescription() : requestDTO.getDescription())
                            .build()
            );

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> getStageStatistics(String stageId) {
        var baseResponseDTO = new BaseResponseDTO<ProgressResponseDTO>();

        try{

            Stage stage = stageDb.findByStageId(stageId);
            Project project = stage.getProject();
            String username = jwtUtils.getUserNameFromRequest(request);

            if (
                    !project.getProjectManager().getUsername().equals(username) &&
                            project.getMemberInProjectList().stream().noneMatch(
                                    p -> p.getProjectMember().getUsername().equals(username)

                            )){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            ProgressResponseDTO responseDTO = new ProgressResponseDTO();

            Integer totalFinishedTask = taskDb.getTotalFinishedTask(stageId);
            Integer totalTask = taskDb.getTotalTask(stageId);

            responseDTO.setTotalTask(totalTask);
            responseDTO.setFinishedTask(totalFinishedTask);
            responseDTO.setTodoTask(taskDb.getTotalToDoTask(stageId));
            responseDTO.setInProgressTask(taskDb.getTotalInProgressTask(stageId));

            responseDTO.setProgress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100));

            baseResponseDTO.setData(responseDTO);
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setStatus(HttpStatus.OK.value());

            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<?> reorderStage(String projectId, List<ReorderRequestDTO> requestDTOS) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Project project = projectDb.findByProjectId(projectId);
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

            if (!project.getProjectManager().getUsername().equals(user.getUsername())){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            List<Stage> stages = new ArrayList<>();

            for (ReorderRequestDTO requestDTO: requestDTOS){
                Stage stage = stageDb.findByStageId(requestDTO.getId());

                stage.setOrder(requestDTO.getOrder());
                stages.add(stage);
            }

            stageDb.saveAll(stages);

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "Stage has been updated!"));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteStage(String projectId, DeleteRequestDTO requestDTO) {
        var baseResponseDTO = new BaseResponseDTO<CrudResponseDTO>();

        try{
            Project project = projectDb.findByProjectId(projectId);
            User user = userDb.findByUsername(jwtUtils.getUserNameFromRequest(request));

            if (!project.getProjectManager().getUsername().equals(user.getUsername())){
                baseResponseDTO.setStatus(HttpStatus.FORBIDDEN.value());
                baseResponseDTO.setTimestamp(new Date());
                baseResponseDTO.setMessage("Access Not Allowed!");
                return new ResponseEntity<>(baseResponseDTO, HttpStatus.FORBIDDEN);
            }

            stageDb.delete(stageDb.findByStageId(requestDTO.getId()));

            baseResponseDTO.setStatus(HttpStatus.OK.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage("SUCCESS");
            baseResponseDTO.setData(new CrudResponseDTO("SUCCESS", "Stage has been deleted!"));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.OK);
        }catch(Exception e){
            baseResponseDTO.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            baseResponseDTO.setTimestamp(new Date());
            baseResponseDTO.setMessage(String.format(e.getMessage()));
            baseResponseDTO.setData(new CrudResponseDTO("FAILED", String.format(e.getMessage())));
            return new ResponseEntity<>(baseResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String generateStageId(){
        return "STG" + "-" + String.valueOf(System.currentTimeMillis());
    }
}
