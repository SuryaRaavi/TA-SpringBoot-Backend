package com.ta.managementproject.repository;

import com.ta.managementproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDb extends JpaRepository<User, String> {
    User findByEmail(String email);

    @Query("SELECT u.role.name FROM User u WHERE u.email = :email")
    String getRoleByEmail(@Param("email") String email);
}
