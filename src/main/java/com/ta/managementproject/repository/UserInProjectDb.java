package com.ta.managementproject.repository;

import com.ta.managementproject.dto.response.UsersInProjectResponseDTO;
import com.ta.managementproject.entity.UserInProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInProjectDb extends JpaRepository<UserInProject, Long> {

    @Query("""
                      SELECT new com.ta.managementproject.dto.response.UsersInProjectResponseDTO(
                           up.user.username,
                           up.user.fullName,
                           up.user.role.name
                      ) FROM UserInProject up
                            WHERE up.project.projectId = :projectId
                            AND (
                                   LOWER(up.user.username) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                                OR LOWER(up.user.fullName) LIKE LOWER(CONCAT('%', :searchQuery, '%'))
                            )
            """)
    Page<UsersInProjectResponseDTO> findUsersByProjectIdAndQuery(
            @Param("projectId") String projectId,
            @Param("searchQuery") String searchQuery,
            Pageable pageable
    );

    @Query("""
                      SELECT new com.ta.managementproject.dto.response.UsersInProjectResponseDTO(
                           up.user.username,
                           up.user.fullName,
                           up.user.role.name
                      ) FROM UserInProject up
                            WHERE up.project.projectId = :projectId
            """)
    Page<UsersInProjectResponseDTO> findUsersByProjectId(
            @Param("projectId") String projectId,
            Pageable pageable
    );

    @Query("""
            SELECT up
                FROM UserInProject up
                WHERE up.project.projectId = :projectId
                AND up.user.username = :username
    """)
    UserInProject findByProjectIdAndUsername(@Param("projectId") String projectId, @Param("username") String username);
}
