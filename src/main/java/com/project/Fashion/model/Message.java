package com.project.Fashion.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Entity
@Table(name = "messages", indexes = {
        // For findByConversationIdOrderBySentAtAsc
        @Index(name = "idx_message_conv_sent", columnList = "conversation_id, sentAt"),
        // For findUnreadMessagesForUser (covers conversation_id, sender_id, isRead)
        @Index(name = "idx_message_conv_sender_read", columnList = "conversation_id, sender_id, isRead"),
        @Index(name = "idx_message_sender_id", columnList = "sender_id") // If querying by sender alone often
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Conversation conversation;

    @ManyToOne
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String encryptedContent;

    private LocalDateTime sentAt = LocalDateTime.now();
    private boolean isRead = false;
}

