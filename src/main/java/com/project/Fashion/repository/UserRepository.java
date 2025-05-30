package com.project.Fashion.repository;

import com.project.Fashion.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    List<User> findByRole(String role); // To get all users with that role
   // Optional<User> findByRole(String role);
}
