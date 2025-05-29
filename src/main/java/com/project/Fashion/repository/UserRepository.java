package com.project.Fashion.repository;

import com.project.Fashion.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    Optional<User> findByRole(String role);
}
