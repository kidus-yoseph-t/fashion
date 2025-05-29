package com.project.Fashion.controller;

import com.project.Fashion.dto.ChatMessageDto;
import com.project.Fashion.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketChatController {

    @Autowired private ChatService chatService;

    @MessageMapping("/chat.sendMessage") // from client: /app/chat.sendMessage
    @SendTo("/topic/messages")
    public ChatMessageDto handleMessage(ChatMessageDto chatMessageDto) {
        chatService.sendMessage(
                chatMessageDto.getConversationId(),
                chatMessageDto.getSenderId(),
                chatMessageDto.getContent()
        );
        chatMessageDto.setTimestamp(LocalDateTime.now().toString());
        return chatMessageDto;
    }
}
