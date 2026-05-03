package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.ProgressResponseDTO;
import com.ta.managementproject.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDb extends JpaRepository<Project, String>{
    Project findByProjectId(String projectId);

    Project findByJoinCode(String joinCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
                Stage s  
                 SET s.isDeleted = true
                WHERE s.project.projectId = :projectId 
    """)
    int softDeleteStageByProjectId(@Param("projectId") String projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
                Task t  
                 SET t.isDeleted = true
                WHERE t.stage.project.projectId = :projectId 
    """)
    int softDeleteTaskByProjectId(@Param("projectId") String projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
                SubTask st  
                 SET st.isDeleted = true
                WHERE st.task.stage.project.projectId = :projectId 
    """)
    int softDeleteSubTaskByProjectId(@Param("projectId") String projectId);

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
                FROM Project p
                INNER JOIN p.stageList s
                INNER JOIN s.taskList t
                INNER JOIN t.subTaskList st
                WHERE p.projectId = :projectId
            """)
    ProgressResponseDTO getSummaryFromSubTasks(@Param("projectId") String projectId);

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
                FROM Project p
                INNER JOIN p.stageList s
                INNER JOIN s.taskList t
                WHERE p.projectId = :projectId
                AND t.subTaskList IS EMPTY
            """)
    ProgressResponseDTO getSummaryFromTasksWithoutSubTasks(@Param("projectId") String projectId);

    @Query("""
           SELECT
            CASE
                WHEN p.isCancelled = true THEN 'CANCELLED'
                WHEN p.todoTask = p.totalTask THEN 'NOT_STARTED'
                WHEN p.finishedTask = p.totalTask THEN 'COMPLETED'
                ELSE 'IN_PROGRESS'
            END
           FROM Project p
           WHERE p.projectId = :projectId
    """)
    String getProjectStatus(@Param("projectId") String projectId);
}
