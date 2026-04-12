package com.ta.managementproject.repository;

import com.ta.managementproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDb extends JpaRepository<User, String> {
    User findByUsername(String username);

    @Query("SELECT u.role.name FROM User u WHERE u.username = :username")
    String getRoleByUsername(@Param("username") String username);
}
