package com.ironcore.ironcorebackend.repository;

import com.ironcore.ironcorebackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username); 
    // Count non-admin users
    long countByIsAdminFalseOrIsAdminIsNull();
    
    // Alternative method if the above doesn't work
    @Query("SELECT COUNT(u) FROM User u WHERE u.isAdmin = false OR u.isAdmin IS NULL")
    long findNonAdminUsersCount();
}