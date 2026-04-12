package com.ta.managementproject.repository;

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

    @Query(value = """
                    SELECT COUNT(t)
                      FROM Task t
                     WHERE
                     t.stage.stageId = :stageId
                     AND t.status = 'FINISHED'                           
    """)
    Integer getTotalFinishedTask(@Param("stageId") String stageId);

    @Query(value = """
                    SELECT COUNT(t)
                      FROM Task t
                     WHERE
                     t.stage.stageId = :stageId
                     AND t.status = 'TODO'                            
    """)
    Integer getTotalToDoTask(@Param("stageId") String stageId);

    @Query(value = """
                    SELECT COUNT(t)
                      FROM Task t
                     WHERE
                     t.stage.stageId = :stageId
                     AND t.status = 'IN_PROGRESS'                            
    """)
    Integer getTotalInProgressTask(@Param("stageId") String stageId);

    @Query(value = """
            SELECT new com.ta.managementproject.dto.response.TaskResponseDTO(
              t.taskId,               
              t.taskName,
              t.priority,
              t.dueDate,
              t.status,
              t.projectMember.fullName,
              t.label,
              t.createdAt,
              t.order
            )
            FROM Task t
                WHERE t.stage.stageId = :stageId
    """)
    Page<TaskResponseDTO> findTaskByStageId(@Param("stageId") String stageId, Pageable pageable);

    @Query(value = """
            SELECT new com.ta.managementproject.dto.response.TaskResponseDTO(
              t.taskId,              
              t.taskName,
              t.priority,
              t.dueDate,
              t.status,
              t.projectMember.fullName,
              t.label,
              t.createdAt,
              t.order
            )
            FROM Task t
                WHERE t.stage.stageId = :stageId
                AND t.dueDate BETWEEN :startDate AND :endDate
            """)
    Page<TaskResponseDTO> findTaskByStageIdAndDueDate(
            @Param("stageId") String stageId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );

    @Query(value = """
            SELECT new com.ta.managementproject.dto.response.TaskResponseDTO(
              t.taskId,           
              t.taskName,
              t.priority,
              t.dueDate,
              t.status,
              t.projectMember.fullName,
              t.label,
              t.createdAt,
              t.order
            )
            FROM Task t
                WHERE t.stage.stageId = :stageId
                AND (
                       LOWER(t.taskId) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(t.taskName) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(t.projectMember.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                )
    """)
    Page<TaskResponseDTO> searchTaskByQuery(
            @Param("stageId") String stageId,
            @Param("query") String query,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE 
             Task t 
              SET t.order = t.order + 1 
             WHERE t.stage.stageId = :stageId
             AND t.order > :firstOrder
             AND t.order < :secondOrder 
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
             AND t.order > :firstOrder
             AND t.order < :secondOrder  
            """)
    int updateTaskOrderBelow(
            @Param("taskId") String taskId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );
}
