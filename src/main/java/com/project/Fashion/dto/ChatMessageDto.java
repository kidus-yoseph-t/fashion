package com.project.Fashion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatMessageDto {
    private Long conversationId;
    private String senderId;
    private String content;
    private String timestamp;
}
