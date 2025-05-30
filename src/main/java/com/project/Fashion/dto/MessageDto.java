package com.project.Fashion.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDto {
    private Long id;
    private Long conversationId;
    private String senderId;
    private String encryptedContent;
    private LocalDateTime sentAt;
    private boolean isRead;
}
