package com.project.Fashion.dto;
import lombok.Data;
@Data
public class ChatMessageRequestDto {
    private Long conversationId;
    // senderId will be taken from authenticated user on backend for security
    private String content;
}