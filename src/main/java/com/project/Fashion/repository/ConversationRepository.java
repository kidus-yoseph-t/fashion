package com.project.Fashion.repository;

import com.project.Fashion.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.user1.id = :user1Id AND c.user2.id = :user2Id) OR " +
            "(c.user1.id = :user2Id AND c.user2.id = :user1Id)")
    Optional<Conversation> findByUsers(@Param("user1Id") String user1Id, @Param("user2Id") String user2Id);

    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId")
    List<Conversation> findAllByUserId(@Param("userId") String userId);
}

