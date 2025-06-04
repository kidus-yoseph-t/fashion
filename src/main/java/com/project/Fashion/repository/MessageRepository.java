package com.project.Fashion.repository;

import com.project.Fashion.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.isRead = false")
    List<Message> findUnreadMessagesForUser(@Param("conversationId") Long conversationId, @Param("userId") String userId);

    // In MessageRepository.java
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation IN (SELECT c FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId) AND m.sender.id <> :userId AND m.isRead = false")
    int countUnreadMessagesForRecipient(@Param("userId") String userId);
}


