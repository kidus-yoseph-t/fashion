package com.project.Fashion.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationDto {
    private Long id;

    private String user1Id;
    private String user2Id;

    // Fields to hold the names of the participants
    private String user1Name;
    private String user2Name;

    // Add fields for the last message preview
    private String lastMessageContent;
    private LocalDateTime lastMessageTimestamp;
    private String lastMessageSenderId;
    private int unreadMessageCount;

    private LocalDateTime startedAt;
}