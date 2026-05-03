package com.ta.managementproject.repository;

import java.time.Instant;

import com.ta.managementproject.dto.response.ProgressResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.entity.SubTask;

@Repository
public interface SubTaskDb extends JpaRepository<SubTask, String> {

    @Query("SELECT st FROM SubTask st WHERE st.subTaskId = :subTaskId")
    SubTask findSubTaskBySubTaskId (@Param("subTaskId") String subTaskId);

    @Query("SELECT COUNT(st) FROM SubTask st WHERE st.task.taskId = :taskId")
    Integer getTotalSubTask(@Param("taskId") String taskId);

    @Query("""
        SELECT new com.ta.managementproject.dto.response.ProgressResponseDTO(
            COUNT(st),
            SUM(CASE WHEN st.status = 'FINISHED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN st.status = 'TODO' THEN 1 ELSE 0 END),
            SUM(CASE WHEN st.status = 'IN_PROGRESS' THEN 1 ELSE 0 END),
            CASE 
                WHEN COUNT(st) = 0 THEN 0.0
                ELSE (SUM(CASE WHEN st.status = 'FINISHED' THEN 1 ELSE 0 END) * 100.0 / COUNT(st))
            END
        )
        FROM SubTask st
        WHERE st.task.taskId = :taskId
    """)
    ProgressResponseDTO getSubTaskSummary(@Param("taskId") String taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
             SubTask st 
              SET st.order = st.order + 1 
             WHERE st.task.taskId = :taskId
             AND st.order > :firstOrder
             AND st.order < :secondOrder 
            """)
    int updateSubTaskOrderAbove(
            @Param("taskId") String taskId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
             SubTask st 
              SET st.order = st.order - 1 
             WHERE st.task.taskId = :taskId
             AND st.order > :firstOrder
             AND st.order < :secondOrder 
            """)
    int updateSubTaskOrderBelow(
            @Param("taskId") String taskId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE 
             SubTask st 
              SET st.order = st.order - 1 
             WHERE st.task.taskId = :taskId
             AND st.order > :order
            """)
    int updateSubTaskOrderAfterDelete(@Param("taskId") String taskId, @Param("order") Integer order);
}
