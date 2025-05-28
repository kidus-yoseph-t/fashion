package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Data
public class ChatMessageDto {
    private Long conversationId;
    private String senderId;
    private String content;
    private String timestamp;
}
