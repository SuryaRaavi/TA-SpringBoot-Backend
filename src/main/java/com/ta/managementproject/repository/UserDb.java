package com.ta.managementproject.repository;

import com.ta.managementproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDb extends JpaRepository<User, String> {
    User findByUsername(String username);
}
