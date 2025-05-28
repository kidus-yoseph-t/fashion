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

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.sendMessage") // mapped to /app/chat.sendMessage
    @SendTo("/topic/messages")
    public ChatMessageDto sendMessage(ChatMessageDto messageDTO) {
        // Save the message
        chatService.sendMessage(
                messageDTO.getConversationId(),
                messageDTO.getSenderId(),
                messageDTO.getContent()
        );

        messageDTO.setTimestamp(LocalDateTime.now().toString());
        return messageDTO;
    }
}

