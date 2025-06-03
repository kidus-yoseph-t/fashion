package com.project.Fashion.controller;

import com.project.Fashion.dto.ChatMessageDto;
import com.project.Fashion.model.User; // To get User ID
import com.project.Fashion.repository.UserRepository; // To fetch User by email
import com.project.Fashion.service.ChatService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.MessageSendException; // For unauthorized send attempt

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser; // To send error to specific user
import org.springframework.stereotype.Controller;


import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map; // For error message payload

@Controller
public class WebSocketChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/messages")
    public ChatMessageDto handleMessage(ChatMessageDto chatMessageDto, Principal principal, SimpMessageHeaderAccessor headerAccessor) {


        if (principal == null || principal.getName() == null) {
            System.err.println("WebSocket: Principal is null in handleMessage. Session ID: " + headerAccessor.getSessionId());
            throw new MessageSendException("Cannot send message: User is not authenticated in WebSocket session.");
        }

        String authenticatedUserEmail = principal.getName();
        User authenticatedUser = userRepository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> {
                    System.err.println("WebSocket: Authenticated user email '" + authenticatedUserEmail + "' not found in DB. Session ID: " + headerAccessor.getSessionId());
                    return new UserNotFoundException("Authenticated user not found in database: " + authenticatedUserEmail);
                });

        String authenticatedUserId = authenticatedUser.getId();

        if (!authenticatedUserId.equals(chatMessageDto.getSenderId())) {
            System.err.println("Security Alert: Mismatched sender ID in WebSocket message. Authenticated User ID: " +
                    authenticatedUserId + ", DTO SenderId: " + chatMessageDto.getSenderId() +
                    ". Session ID: " + headerAccessor.getSessionId() + ". Principal: " + principal.getName());
            throw new MessageSendException("Sender ID in message does not match authenticated user.");
        }

        chatService.sendMessage(
                chatMessageDto.getConversationId(),
                chatMessageDto.getSenderId(),
                chatMessageDto.getContent()
        );

        chatMessageDto.setTimestamp(LocalDateTime.now().toString());
        return chatMessageDto;
    }

    /**
     * Handles exceptions specifically from @MessageMapping methods in this controller.
     * Sends an error message back to the user who caused the error on a private queue.
     * @param exception The exception that was thrown.
     * @param principal The authenticated user who caused the error.
     * @return A map containing the error message.
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors") // Sends to a user-specific queue like /user/{username}/queue/errors
    public Map<String, String> handleWebSocketException(Exception exception, Principal principal) {
        String errorMessage = "An error occurred processing your message: " + exception.getMessage();
        if (principal != null) {
            System.err.println("WebSocket Error for user " + principal.getName() + ": " + exception.getMessage());
        } else {
            System.err.println("WebSocket Error for unauthenticated user: " + exception.getMessage());
        }
        // You can customize the error payload
        return Map.of("error", errorMessage);
    }

    /**
     * More specific handler for MessageSendException
     */
    @MessageExceptionHandler(MessageSendException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleMessageSendException(MessageSendException exception, Principal principal) {
        String userDisplay = (principal != null && principal.getName() != null) ? principal.getName() : "unknown user";
        System.err.println("MessageSendException for user " + userDisplay + ": " + exception.getMessage());
        return Map.of("error", "Message sending failed: " + exception.getMessage());
    }

    /**
     * More specific handler for UserNotFoundException (if it can occur from WebSocket interactions)
     */
    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleUserNotFoundException(UserNotFoundException exception, Principal principal) {
        String userDisplay = (principal != null && principal.getName() != null) ? principal.getName() : "unknown user";
        System.err.println("UserNotFoundException during WebSocket interaction for " + userDisplay + ": " + exception.getMessage());
        return Map.of("error", "User context error: " + exception.getMessage());
    }
}
