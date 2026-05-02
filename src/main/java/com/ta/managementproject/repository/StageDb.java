package com.ta.managementproject.repository;


import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StageDb extends JpaRepository<Stage, String> {

    Stage findByStageId(String stageId);

    @Query("SELECT COUNT(s) FROM Stage s WHERE s.project.projectId = :projectId")
    int getTotalStage(@Param("projectId") String projectId);

    @Modifying
    @Query("""
            UPDATE 
             Stage s 
              SET s.order = s.order + 1 
             WHERE s.project.projectId = :projectId
             AND s.order >= :firstOrder
             AND s.order <= :secondOrder 
            """)
    int updateStageOrderAbove(
            @Param("projectId") String projectId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying
    @Query("""
            UPDATE 
             Stage s 
              SET s.order = s.order - 1 
             WHERE s.project.projectId = :projectId
             AND s.order >= :firstOrder
             AND s.order <= :secondOrder 
            """)
    int updateStageOrderBelow(
            @Param("projectId") String projectId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying
    @Query(
            """
            UPDATE 
             Stage s
              SET s.order = s.order - 1
             WHERE s.project.projectId = :projectId
             AND s.order > :order
    """)
    int updateStageOrderAfterDelete(@Param("projectId") String projectId, @Param("order") Integer order);

    @Modifying
    @Query("""
            UPDATE 
                Task t  
                 SET t.isDeleted = true
                WHERE t.stage.stageId = :stageId 
    """)
    int softDeleteTaskByStageId(@Param("stageId") String stageId);

    @Modifying
    @Query("""
            UPDATE 
                SubTask st  
                 SET st.isDeleted = true
                WHERE st.task.stage.stageId = :stageId 
    """)
    int softDeleteSubTaskByStageId(@Param("stageId") String stageId);

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
                FROM Stage s
                INNER JOIN s.taskList t
                INNER JOIN t.subTaskList st
                WHERE s.stageId = :stageId
            """)
    ProgressResponseDTO getSummaryFromSubTasks(@Param("stageId") String stageId);

    // Query 2: ambil task yang TIDAK PUNYA subtask
    // progress dihitung dari status task langsung
    @Query("""
                SELECT new com.ta.managementproject.dto.response.ProgressResponseDTO(
                    COUNT(t),
                      COALESCE(SUM(CASE WHEN t.status = 'FINISHED' THEN 1 ELSE 0 END), 0),
                      COALESCE(SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END), 0),
                      COALESCE(SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END), 0),
                    CASE
                        WHEN COUNT(t) = 0 THEN 0.0
                        ELSE (COALESCE(SUM(CASE WHEN t.status = 'FINISHED' THEN 1 ELSE 0 END), 0) * 100.0 / COUNT(t))
                    END
                )
                FROM Stage s
                INNER JOIN s.taskList t
                WHERE s.stageId = :stageId
                AND t.subTaskList IS EMPTY
            """)
    ProgressResponseDTO getSummaryFromTasksWithoutSubTasks(@Param("stageId") String stageId);
}
