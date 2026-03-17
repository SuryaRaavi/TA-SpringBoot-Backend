package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.ProjectResponseDTO;
import com.ta.managementproject.dto.response.UsersInProjectResponseDTO;
import com.ta.managementproject.entity.MemberInProject;
import com.ta.managementproject.entity.ProjectMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface MemberInProjectDb extends JpaRepository<MemberInProject, Long> {

    @Query("""
           SELECT mp
            FROM MemberInProject mp
            WHERE mp.projectMember.username = :username
            AND mp.project.projectId = :projectId
    """)
    MemberInProject findByProjectIdAndUsername(@Param("projectId") String projectId, @Param("username") String username);

    @Query("""
            SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                mp.project.projectId,
                mp.project.projectName,
                mp.project.status
            )
            FROM MemberInProject mp
            WHERE mp.projectMember.username = :username
              AND mp.projectMember.isDeleted = false
        """)
    Page<ProjectResponseDTO> findByProjectMember(@Param("username") String username, Pageable pageable);

    @Query("""
                SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                    p.projectId,
                    p.projectName,
                    p.status
                )
                FROM MemberInProject mp
                JOIN mp.project p
                JOIN mp.projectMember pmb
                WHERE pmb.username = :username
                   AND (:startDate IS NULL OR p.startDate >= :startDate)
                   AND (:endDate IS NULL OR p.endDate <= :endDate)
                  AND p.isDeleted = false
            """)
    Page<ProjectResponseDTO> findAllByProjectMemberAndStartEndProyek(
            @Param("username") String username,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );


    @Query("""
                SELECT new com.ta.managementproject.dto.response.ProjectResponseDTO(
                    p.projectId,
                    p.projectName,
                    p.status
                )
                FROM MemberInProject mp
                JOIN mp.project p
                JOIN mp.projectMember h
                WHERE h.username = :username
                  AND (LOWER(p.projectName) LIKE LOWER(CONCAT('%', :parameter, '%'))
                  OR LOWER(p.projectId) LIKE LOWER(CONCAT('%', :parameter, '%')))
                  AND p.isDeleted = false
            """)
    Page<ProjectResponseDTO> findPMBProjectByProjectNameAndProjectId(
            @Param("username") String username,
            @Param("parameter") String parameter,
            Pageable pageable
    );

    @Query("""
            SELECT new com.ta.managementproject.dto.response.UsersInProjectResponseDTO(
                   mp.projectMember.username,
                   mp.projectMember.fullName,
                   mp.projectMember.role.name         
              ) FROM MemberInProject mp
                WHERE mp.project.projectId = :projectId
    """)
    Page<UsersInProjectResponseDTO> findProjectPMB(
            @Param("projectId") String projectId,
            Pageable pageable
    );
}
