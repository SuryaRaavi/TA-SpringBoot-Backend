package com.ta.managementproject.repository;

import java.time.Instant;

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

    @Query(value = """
                    SELECT COUNT(st)
                      FROM SubTask st
                     WHERE
                     st.task.taskId = :taskId
                     AND st.status = 'FINISHED'                           
    """)
    Integer getTotalFinishedSubTask(@Param("taskId") String taskId);

    @Query(value = """
                    SELECT COUNT(st)
                      FROM SubTask st
                     WHERE
                     st.task.taskId = :taskId
                     AND st.status = 'TODO'                            
    """)
    Integer getTotalToDoSubTask(@Param("taskId") String taskId);

    @Query(value = """
                    SELECT COUNT(st)
                      FROM SubTask st
                     WHERE
                     st.task.taskId = :taskId
                     AND st.status = 'IN_PROGRESS'                            
    """)
    Integer getTotalInProgressSubTask(@Param("taskId") String taskId);

    @Modifying
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

    @Modifying
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

    @Modifying
    @Query("""
            UPDATE 
             SubTask st 
              SET st.order = st.order - 1 
             WHERE st.task.taskId = :taskId
             AND st.order > :order
            """)
    int updateSubTaskOrderAfterDelete(@Param("taskId") String taskId, @Param("order") Integer order);
}
