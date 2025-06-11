package com.project.Fashion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project.Fashion.dto.ChatMessageDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.MessageSendException;

import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class);

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Use a Jackson ObjectMapper to manually deserialize later
    private final ObjectMapper objectMapper;

    @Autowired
    public WebSocketChatController(ChatService chatService,
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;

        // Initialize ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @MessageMapping("/chat.sendMessage")
    public void handleMessage(@Payload String rawPayload, Principal principal) {
        // --- DIAGNOSTIC LOG ---
        logger.info("DIAGNOSTIC: Received raw payload in handleMessage: {}", rawPayload);

        try {
            if (principal == null || principal.getName() == null) {
                throw new MessageSendException("Cannot send message: User is not authenticated in WebSocket session.");
            }

            // Manually deserialize the payload
            ChatMessageDto incomingChatMessage = objectMapper.readValue(rawPayload, ChatMessageDto.class);

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

            String destination = "/topic/conversation/" + persistedMessageDto.getConversationId();

            logger.info("Sending message ID {} to destination: {}", persistedMessageDto.getId(), destination);

            messagingTemplate.convertAndSend(destination, persistedMessageDto);

        } catch (Exception e) {
            logger.error("Error processing WebSocket message payload.", e);
            // Optionally, send an error back to the user
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors",
                        Map.of("error", "Failed to process your message: " + e.getMessage()));
            }
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketException(Exception exception, Principal principal) {
        String username = (principal != null) ? principal.getName() : "unauthenticated user";
        logger.error("WebSocket Error for user {}: {}", username, exception.getMessage(), exception);
        return Map.of("error", "An error occurred: " + exception.getMessage());
    }
}