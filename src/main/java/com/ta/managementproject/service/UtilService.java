package com.ta.managementproject.service;

import com.ta.managementproject.dto.BaseResponseDTO;
import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.entity.Project;
import com.ta.managementproject.entity.Stage;
import com.ta.managementproject.entity.Task;
import com.ta.managementproject.repository.ProjectDb;
import com.ta.managementproject.repository.StageDb;
import com.ta.managementproject.repository.TaskDb;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Date;

@Service
public class UtilService {

    private final ProjectDb projectDb;
    private final StageDb stageDb;
    private final TaskDb taskDb;

    public UtilService(ProjectDb projectDb, StageDb stageDb, TaskDb taskDb){
        this.projectDb = projectDb;
        this.stageDb = stageDb;
        this.taskDb = taskDb;
    }

    // CYC: 1, LOC: 9, COG: 0
    public <T> ResponseEntity<BaseResponseDTO<T>> buildResponse(
            HttpStatus status, String message, T data) {

        BaseResponseDTO<T> res = new BaseResponseDTO<>();
        res.setStatus(status.value());
        res.setTimestamp(new Date());
        res.setMessage(message);
        res.setData(data);

        return new ResponseEntity<>(res, status);
    }

    // CYC: 2, LOC: 18, COG: 1
    @Transactional
    public void updateProjectSummary(String projectId) {
        Project project = projectDb.findByProjectId(projectId);
        ProgressResponseDTO fromSubTasks = projectDb.getSummaryFromSubTasks(projectId);
        ProgressResponseDTO fromTasks = projectDb.getSummaryFromTasksWithoutSubTasks(projectId);

        long totalTask = fromSubTasks.getTotalTask() + fromTasks.getTotalTask();
        long totalFinishedTask = fromSubTasks.getFinishedTask() + fromTasks.getFinishedTask();
        long totalInProgressTask = fromSubTasks.getInProgressTask() + fromTasks.getInProgressTask();
        long totalToDoTask = fromSubTasks.getTodoTask() + fromTasks.getTodoTask();

        projectDb.save(project.toBuilder()
                .finishedTask(totalFinishedTask)
                .todoTask(totalToDoTask)
                .inProgressTask(totalInProgressTask)
                .totalTask(totalTask)
                .progress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100))
                .build()
        );
    }

    // CYC: 2, LOC: 18, COG: 1
    @Transactional
    public void updateStageSummary(String stageId) {
        Stage stage = stageDb.findByStageId(stageId);
        ProgressResponseDTO fromSubTasks = stageDb.getSummaryFromSubTasks(stageId);
        ProgressResponseDTO fromTasks = stageDb.getSummaryFromTasksWithoutSubTasks(stageId);

        long totalTask = fromSubTasks.getTotalTask() + fromTasks.getTotalTask();
        long totalFinishedTask = fromSubTasks.getFinishedTask() + fromTasks.getFinishedTask();
        long totalInProgressTask = fromSubTasks.getInProgressTask() + fromTasks.getInProgressTask();
        long totalToDoTask = fromSubTasks.getTodoTask() + fromTasks.getTodoTask();

        stageDb.save(stage.toBuilder()
                .finishedTask(totalFinishedTask)
                .todoTask(totalToDoTask)
                .inProgressTask(totalInProgressTask)
                .totalTask(totalTask)
                .progress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100))
                .build()
        );
    }

    // Total CYC: 4, LOC: 26, COG: 4
    @Transactional
    public void updateTaskStatusAndSummary(String taskId){
        Task task = taskDb.findByTaskId(taskId);
        String status = "";

        ProgressResponseDTO fromSubTasks = taskDb.getSummaryFromSubTasks(taskId);

        long totalTask = fromSubTasks.getTotalTask();
        long totalFinishedTask = fromSubTasks.getFinishedTask();
        long totalInProgressTask = fromSubTasks.getInProgressTask();
        long totalToDoTask = fromSubTasks.getTodoTask();

        if (totalTask == totalToDoTask){
            status = "TODO";
        }else if (totalTask == totalFinishedTask){
            status = "FINISHED";
        }else{
            status = "IN_PROGRESS";
        }

        taskDb.save(task.toBuilder()
                .finishedTask(totalFinishedTask)
                .todoTask(totalToDoTask)
                .inProgressTask(totalInProgressTask)
                .totalTask(totalTask)
                .progress(totalTask == 0 ? 0.00 : (totalFinishedTask * 1.0 / totalTask * 100))
                .status(status)
                .build()
        );
    }
}
