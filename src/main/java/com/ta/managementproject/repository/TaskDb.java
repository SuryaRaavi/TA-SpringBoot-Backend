package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface TaskDb extends JpaRepository<Task, String> {

    @Query("SELECT t FROM Task t WHERE t.taskId = :taskId")
    Task findByTaskId(@Param("taskId") String taskId);

    @Query(value = "SELECT COUNT(t) FROM Task t WHERE t.stage.stageId = :stageId")
    Integer getTotalTask(@Param("stageId") String stageId);

    @Modifying
    @Query("""
            UPDATE 
             Task t 
              SET t.order = t.order + 1 
             WHERE t.stage.stageId = :stageId
             AND t.order >= :firstOrder
             AND t.order <= :secondOrder 
            """)
    int updateTaskOrderAbove(
            @Param("stageId") String stageId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying
    @Query("""
            UPDATE 
             Task t 
              SET t.order = t.order - 1 
             WHERE t.stage.stageId = :stageId
             AND t.order >= :firstOrder
             AND t.order <= :secondOrder  
            """)
    int updateTaskOrderBelow(
            @Param("stageId") String stageId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying
    @Query("""
            UPDATE 
             Task t 
              SET t.order = t.order - 1 
             WHERE t.stage.stageId = :stageId
             AND t.order > :order
            """)
    int updateTaskOrderAfterDelete(@Param("stageId") String stageId, @Param("order") Integer order);

    @Modifying
    @Query("""
            UPDATE 
                SubTask st  
                 SET st.isDeleted = true
                WHERE st.task.taskId = :taskId 
    """)
    int softDeleteSubTaskByTaskId(@Param("taskId") String taskId);

    // Query 1: ambil task yang PUNYA subtask
    // progress dihitung dari subtask
    @Query("""
                SELECT new com.ta.managementproject.dto.response.ProgressResponseDTO(
                    COUNT(st),
                      COALESCE(SUM(CASE WHEN st.status = 'FINISHED' THEN 1 ELSE 0 END), 0),
                      COALESCE(SUM(CASE WHEN st.status = 'TODO' THEN 1 ELSE 0 END), 0),
                      COALESCE(SUM(CASE WHEN st.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0),
                    CASE
                        WHEN COUNT(st) = 0 THEN 0.0
                        ELSE (COALESCE(SUM(CASE WHEN st.status = 'FINISHED' THEN 1 ELSE 0 END), 0) * 100.0 / COUNT(st))
                    END
                )
                FROM Task t
                INNER JOIN t.subTaskList st
                WHERE t.taskId = :taskId
            """)
    ProgressResponseDTO getSummaryFromSubTasks(@Param("taskId") String taskId);
}
