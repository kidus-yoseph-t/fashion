package com.project.Fashion.repository;

import com.project.Fashion.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    // You can add custom query methods here if needed, e.g., findByStatus("unread")
}