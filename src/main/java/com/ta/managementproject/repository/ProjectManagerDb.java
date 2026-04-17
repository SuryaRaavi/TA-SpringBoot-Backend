package com.ta.managementproject.repository;

import com.ta.managementproject.entity.ProjectManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectManagerDb extends JpaRepository<ProjectManager, String> {
    ProjectManager findByUsername(String userName);
}
