package com.project.Fashion.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDto {
    private Long id;
    private String user1Id;
    private String user2Id;
    private LocalDateTime startedAt;
}
