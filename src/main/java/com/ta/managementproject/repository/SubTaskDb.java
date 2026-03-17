package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.SubTaskResponseDTO;
import com.ta.managementproject.dto.response.TaskResponseDTO;
import com.ta.managementproject.entity.SubTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface SubTaskDb extends JpaRepository<SubTask, String> {

    @Query(value = """
                    SELECT new com.ta.managementproject.dto.response.SubTaskResponseDTO(
                      st.subTaskId,
                      st.subTaskName,
                      st.dueDate,
                      st.status,
                      st.label,
                      st.projectMember.fullName
                    )
                    FROM SubTask st
                        WHERE st.task.taskId = :taskId
            """)
    Page<SubTaskResponseDTO> findSubTaskByTaskId(@Param("taskId") String taskId, Pageable pageable);

    @Query(value = """
                    SELECT new com.ta.managementproject.dto.response.SubTaskResponseDTO(
                      st.subTaskId,
                      st.subTaskName,
                      st.dueDate,
                      st.status,
                      st.label,
                      st.projectMember.fullName
                    )
                    FROM SubTask st
                        WHERE st.task.taskId = :taskId
                        AND st.dueDate BETWEEN :startDate AND :endDate
            """)
    Page<SubTaskResponseDTO> findSubTaskByTaskIdAndDueDate(
            @Param("taskId") String taskId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query(value = """
            SELECT new com.ta.managementproject.dto.response.SubTaskResponseDTO(
                      st.subTaskId,
                      st.subTaskName,
                      st.dueDate,
                      st.status,
                      st.label,
                      st.projectMember.fullName
            )
            FROM SubTask st
                WHERE st.task.taskId = :taskId
                AND (
                       LOWER(st.subTaskId) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(st.subTaskName) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(st.projectMember.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                )
    """)
    Page<SubTaskResponseDTO> searchSubTaskByQuery(
            @Param("taskId") String taskId,
            @Param("query") String query,
            Pageable pageable
    );
}
