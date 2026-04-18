package com.ta.managementproject.repository;


import com.ta.managementproject.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StageDb extends JpaRepository<Stage, String> {

    Stage findByStageId(String stageId);

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
}
