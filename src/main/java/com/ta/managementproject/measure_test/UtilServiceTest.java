package com.ta.managementproject.measure_test;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.repository.ProjectDb;
import com.ta.managementproject.repository.StageDb;
import com.ta.managementproject.repository.TaskDb;
import com.ta.managementproject.service.UtilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilServiceTest {

    @Mock
    private ProjectDb projectDb;

    @Mock
    private StageDb stageDb;

    @Mock
    private TaskDb taskDb;

    @InjectMocks
    private UtilService utilService;

    private Project mockProject;
    private Stage mockStage;
    private Task mockTask;

    @BeforeEach
    void setUp() {
        mockProject = Project.builder()
                .projectId("project-1")
                .build();

        mockStage = Stage.builder()
                .stageId("stage-1")
                .build();

        mockTask = Task.builder()
                .taskId("task-1")
                .build();
    }

    // ===================== buildResponse =====================

    @Test
    void buildResponse_ShouldReturnCorrectStatus() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", "data");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void buildResponse_ShouldReturnCorrectMessage() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", "data");

        assertEquals("Success", response.getBody().getMessage());
    }

    @Test
    void buildResponse_ShouldReturnCorrectData() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", "data");

        assertEquals("data", response.getBody().getData());
    }

    @Test
    void buildResponse_ShouldReturnCorrectStatusCode() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", "data");

        assertEquals(200, response.getBody().getStatus());
    }

    @Test
    void buildResponse_ShouldReturnTimestamp() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", "data");

        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void buildResponse_ShouldReturnNullData_WhenDataIsNull() {
        ResponseEntity<BaseResponseDTO<Object>> response =
                utilService.buildResponse(HttpStatus.OK, "Success", null);

        assertNull(response.getBody().getData());
    }

    @Test
    void buildResponse_ShouldReturn404Status_WhenNotFound() {
        ResponseEntity<BaseResponseDTO<String>> response =
                utilService.buildResponse(HttpStatus.NOT_FOUND, "Not Found", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
    }

    // ===================== updateProjectSummary =====================

    @Test
    void updateProjectSummary_ShouldCallFindByProjectId() {
        ProgressResponseDTO fromSubTasks = buildProgress(5, 2, 1, 2);
        ProgressResponseDTO fromTasks = buildProgress(3, 1, 1, 1);

        when(projectDb.findByProjectId("project-1")).thenReturn(mockProject);
        when(projectDb.getSummaryFromSubTasks("project-1")).thenReturn(fromSubTasks);
        when(projectDb.getSummaryFromTasksWithoutSubTasks("project-1")).thenReturn(fromTasks);
        when(projectDb.save(any())).thenReturn(mockProject);

        utilService.updateProjectSummary("project-1");

        verify(projectDb, times(1)).findByProjectId("project-1");
    }

    @Test
    void updateProjectSummary_ShouldSaveProjectWithCorrectProgress() {
        ProgressResponseDTO fromSubTasks = buildProgress(4, 2, 1, 1);
        ProgressResponseDTO fromTasks = buildProgress(4, 2, 1, 1);

        when(projectDb.findByProjectId("project-1")).thenReturn(mockProject);
        when(projectDb.getSummaryFromSubTasks("project-1")).thenReturn(fromSubTasks);
        when(projectDb.getSummaryFromTasksWithoutSubTasks("project-1")).thenReturn(fromTasks);
        when(projectDb.save(any())).thenReturn(mockProject);

        utilService.updateProjectSummary("project-1");

        // totalTask = 8, totalFinished = 4, progress = 4/8 * 100 = 50.0
        verify(projectDb).save(argThat(p ->
                p.getTotalTask() == 8 &&
                        p.getFinishedTask() == 4 &&
                        p.getInProgressTask() == 2 &&
                        p.getTodoTask() == 2 &&
                        p.getProgress() == 50.0
        ));
    }

    @Test
    void updateProjectSummary_ShouldSetProgressZero_WhenNoTasks() {
        ProgressResponseDTO empty = buildProgress(0, 0, 0, 0);

        when(projectDb.findByProjectId("project-1")).thenReturn(mockProject);
        when(projectDb.getSummaryFromSubTasks("project-1")).thenReturn(empty);
        when(projectDb.getSummaryFromTasksWithoutSubTasks("project-1")).thenReturn(empty);
        when(projectDb.save(any())).thenReturn(mockProject);

        utilService.updateProjectSummary("project-1");

        verify(projectDb).save(argThat(p -> p.getProgress() == 0.00));
    }

    @Test
    void updateProjectSummary_ShouldSetProgressHundred_WhenAllFinished() {
        ProgressResponseDTO allDone = buildProgress(5, 5, 0, 0);
        ProgressResponseDTO empty = buildProgress(0, 0, 0, 0);

        when(projectDb.findByProjectId("project-1")).thenReturn(mockProject);
        when(projectDb.getSummaryFromSubTasks("project-1")).thenReturn(allDone);
        when(projectDb.getSummaryFromTasksWithoutSubTasks("project-1")).thenReturn(empty);
        when(projectDb.save(any())).thenReturn(mockProject);

        utilService.updateProjectSummary("project-1");

        verify(projectDb).save(argThat(p -> p.getProgress() == 100.0));
    }

    // ===================== updateStageSummary =====================

    @Test
    void updateStageSummary_ShouldCallFindByStageId() {
        ProgressResponseDTO fromSubTasks = buildProgress(3, 1, 1, 1);
        ProgressResponseDTO fromTasks = buildProgress(2, 0, 1, 1);

        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        when(stageDb.getSummaryFromSubTasks("stage-1")).thenReturn(fromSubTasks);
        when(stageDb.getSummaryFromTasksWithoutSubTasks("stage-1")).thenReturn(fromTasks);
        when(stageDb.save(any())).thenReturn(mockStage);

        utilService.updateStageSummary("stage-1");

        verify(stageDb, times(1)).findByStageId("stage-1");
    }

    @Test
    void updateStageSummary_ShouldSaveStageWithCorrectTotals() {
        ProgressResponseDTO fromSubTasks = buildProgress(3, 1, 1, 1);
        ProgressResponseDTO fromTasks = buildProgress(2, 0, 1, 1);

        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        when(stageDb.getSummaryFromSubTasks("stage-1")).thenReturn(fromSubTasks);
        when(stageDb.getSummaryFromTasksWithoutSubTasks("stage-1")).thenReturn(fromTasks);
        when(stageDb.save(any())).thenReturn(mockStage);

        utilService.updateStageSummary("stage-1");

        verify(stageDb).save(argThat(s ->
                s.getTotalTask() == 5 &&
                        s.getFinishedTask() == 1 &&
                        s.getInProgressTask() == 2 &&
                        s.getTodoTask() == 2
        ));
    }

    @Test
    void updateStageSummary_ShouldSetProgressZero_WhenNoTasks() {
        ProgressResponseDTO empty = buildProgress(0, 0, 0, 0);

        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        when(stageDb.getSummaryFromSubTasks("stage-1")).thenReturn(empty);
        when(stageDb.getSummaryFromTasksWithoutSubTasks("stage-1")).thenReturn(empty);
        when(stageDb.save(any())).thenReturn(mockStage);

        utilService.updateStageSummary("stage-1");

        verify(stageDb).save(argThat(s -> s.getProgress() == 0.00));
    }

    @Test
    void updateStageSummary_ShouldSetProgressHundred_WhenAllFinished() {
        ProgressResponseDTO allDone = buildProgress(4, 4, 0, 0);
        ProgressResponseDTO empty = buildProgress(0, 0, 0, 0);

        when(stageDb.findByStageId("stage-1")).thenReturn(mockStage);
        when(stageDb.getSummaryFromSubTasks("stage-1")).thenReturn(allDone);
        when(stageDb.getSummaryFromTasksWithoutSubTasks("stage-1")).thenReturn(empty);
        when(stageDb.save(any())).thenReturn(mockStage);

        utilService.updateStageSummary("stage-1");

        verify(stageDb).save(argThat(s -> s.getProgress() == 100.0));
    }

    // ===================== updateTaskStatusAndSummary =====================

    @Test
    void updateTaskStatusAndSummary_ShouldSetStatusTodo_WhenAllTasksAreTodo() {
        ProgressResponseDTO allTodo = buildProgress(3, 0, 0, 3);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(allTodo);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        verify(taskDb).save(argThat(t -> "TODO".equals(t.getStatus())));
    }

    @Test
    void updateTaskStatusAndSummary_ShouldSetStatusFinished_WhenAllTasksAreFinished() {
        ProgressResponseDTO allFinished = buildProgress(3, 3, 0, 0);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(allFinished);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        verify(taskDb).save(argThat(t -> "FINISHED".equals(t.getStatus())));
    }

    @Test
    void updateTaskStatusAndSummary_ShouldSetStatusInProgress_WhenMixed() {
        ProgressResponseDTO mixed = buildProgress(3, 1, 1, 1);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(mixed);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        verify(taskDb).save(argThat(t -> "IN_PROGRESS".equals(t.getStatus())));
    }

    @Test
    void updateTaskStatusAndSummary_ShouldSetProgressZero_WhenNoSubTasks() {
        ProgressResponseDTO empty = buildProgress(0, 0, 0, 0);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(empty);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        verify(taskDb).save(argThat(t -> t.getProgress() == 0.00));
    }

    @Test
    void updateTaskStatusAndSummary_ShouldSaveCorrectSummary() {
        ProgressResponseDTO data = buildProgress(6, 3, 2, 1);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(data);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        // progress = 3/6 * 100 = 50.0, status = IN_PROGRESS
        verify(taskDb).save(argThat(t ->
                t.getTotalTask() == 6 &&
                        t.getFinishedTask() == 3 &&
                        t.getInProgressTask() == 2 &&
                        t.getTodoTask() == 1 &&
                        t.getProgress() == 50.0 &&
                        "IN_PROGRESS".equals(t.getStatus())
        ));
    }

    @Test
    void updateTaskStatusAndSummary_ShouldCallFindByTaskId() {
        ProgressResponseDTO data = buildProgress(2, 2, 0, 0);

        when(taskDb.findByTaskId("task-1")).thenReturn(mockTask);
        when(taskDb.getSummaryFromSubTasks("task-1")).thenReturn(data);
        when(taskDb.save(any())).thenReturn(mockTask);

        utilService.updateTaskStatusAndSummary("task-1");

        verify(taskDb, times(1)).findByTaskId("task-1");
    }

    // ===================== Helper =====================

    private ProgressResponseDTO buildProgress(long total, long finished, long inProgress, long todo) {
        ProgressResponseDTO dto = new ProgressResponseDTO();
        dto.setTotalTask(total);
        dto.setFinishedTask(finished);
        dto.setInProgressTask(inProgress);
        dto.setTodoTask(todo);
        return dto;
    }
}