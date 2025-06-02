package com.project.Fashion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversation_user1_id", columnList = "user1_id"),
        @Index(name = "idx_conversation_user2_id", columnList = "user2_id"),
        // This composite index helps the findByUsers query specifically.
        @Index(name = "idx_conversation_users_pair", columnList = "user1_id, user2_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    private LocalDateTime startedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "conversation")
    private List<Message> messages;
}
