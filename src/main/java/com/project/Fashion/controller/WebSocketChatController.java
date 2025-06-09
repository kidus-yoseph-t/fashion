package com.project.Fashion.controller;

import com.project.Fashion.dto.ChatMessageDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.MessageSendException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Handle incoming chat messages via WebSocket",
            description = "Receives a chat message sent to '/app/chat.sendMessage' and broadcasts the persisted message to '/topic/messages'."
    )
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/messages")
    public MessageDto handleMessage(@Payload ChatMessageDto incomingChatMessage, Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new MessageSendException("Cannot send message: User is not authenticated in WebSocket session.");
        }

        String authenticatedUserEmail = principal.getName();
        User authenticatedUser = userRepository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + authenticatedUserEmail));

        String authenticatedUserId = authenticatedUser.getId();

        if (!authenticatedUserId.equals(incomingChatMessage.getSenderId())) {
            logger.warn("Security Alert: Mismatched sender ID. Authenticated: {}, Payload: {}.",
                    authenticatedUserId, incomingChatMessage.getSenderId());
            throw new MessageSendException("Sender ID in message does not match authenticated user.");
        }

        MessageDto persistedMessageDto = chatService.sendMessage(
                incomingChatMessage.getConversationId(),
                authenticatedUserId,
                incomingChatMessage.getContent()
        );

        logger.info("Broadcasting message ID {} for conversationId: {}", persistedMessageDto.getId(), persistedMessageDto.getConversationId());
        return persistedMessageDto;
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketException(Exception exception, Principal principal) {
        String username = (principal != null) ? principal.getName() : "unauthenticated user";
        logger.error("WebSocket Error for user {}: {}", username, exception.getMessage(), exception);
        return Map.of("error", "An error occurred: " + exception.getMessage());
    }
}