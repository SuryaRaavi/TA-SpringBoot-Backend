package com.ta.managementproject.repository;

import com.ta.managementproject.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDb extends JpaRepository<Project, String>{
    Project findByProjectId(String projectId);

    Project findByJoinCode(String joinCode);
}
