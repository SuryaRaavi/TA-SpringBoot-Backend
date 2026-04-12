package com.ta.managementproject.repository;


import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.dto.response.StageResponseDTO;
import com.ta.managementproject.entity.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StageDb extends JpaRepository<Stage, String> {

    Stage findByStageId(String stageId);
    @Query("SELECT COUNT(s) FROM Stage s WHERE s.project.projectId = :projectId")
    Integer getTotalStageByProject(@Param("projectId") String projectId);

    @Query("""
            SELECT new com.ta.managementproject.dto.response.StageResponseDTO(
                s.stageId,
                s.stageName,
                s.order
            )
            FROM Stage s
            WHERE s.project.projectManager.username = :username
              AND s.project.projectId = :projectId
            """
    )
    List<StageResponseDTO> findAllByProjectIdAndUsernamePM(
            @Param("username") String username,
            @Param("projectId") String projectId
    );

    @Query("""
            SELECT new com.ta.managementproject.dto.response.StageResponseDTO(
                s.stageId,
                s.stageName,
                s.order
            )
            FROM MemberInProject mp
                JOIN Stage s
                WHERE mp.projectMember.username = :username
                  AND mp.project.projectId = :projectId
                  AND s.project.projectId = mp.project.projectId
    """)
    List<StageResponseDTO> findAllByProjectIdAndUsernamePMB(
            @Param("username") String username,
            @Param("projectId") String projectId
    );

    @Modifying
    @Query("""
            UPDATE 
             Stage s 
              SET s.order = s.order + 1 
             WHERE s.project.projectId = :projectId
             AND s.order > :firstOrder
             AND s.order < :secondOrder 
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
             AND s.order > :firstOrder
             AND s.order < :secondOrder 
            """)
    int updateStageOrderBelow(
            @Param("projectId") String projectId,
            @Param("firstOrder") Integer firstOrder,
            @Param("secondOrder") Integer secondOrder
    );
}
