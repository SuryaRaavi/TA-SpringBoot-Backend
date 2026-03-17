package com.ta.managementproject.repository;

import com.ta.managementproject.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberDb extends JpaRepository<ProjectMember, String> {
    ProjectMember findByUsername(String username);
}
