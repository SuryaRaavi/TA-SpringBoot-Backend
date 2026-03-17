package com.ta.managementproject.repository;

import com.ta.managementproject.entity.ProjectManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectManagerDb extends JpaRepository<ProjectManager, String> {
    @Query(value = "SELECT pm.full_name FROM project_manager pm WHERE pm.username = :username", nativeQuery = true)
    String getFullNameByUserName(@Param("username") String username);

    ProjectManager findByUsername(String userName);
}
