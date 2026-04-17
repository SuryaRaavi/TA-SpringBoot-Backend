package com.ta.managementproject.repository;

import com.ta.managementproject.entity.MemberInProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberInProjectDb extends JpaRepository<MemberInProject, Long> {

}
