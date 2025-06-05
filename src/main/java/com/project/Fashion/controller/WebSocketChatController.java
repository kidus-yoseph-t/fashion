package com.project.Fashion.controller;

import com.project.Fashion.dto.ChatMessageDto;
import com.project.Fashion.dto.MessageDto; // Added for receiving from service
import com.project.Fashion.model.User;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.MessageSendException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
@Tag(name = "WebSocket Chat", description =
        "Manages real-time chat messages over WebSockets. " +
                "Clients connect to the STOMP endpoint configured at '/ws' (see WebSocketConfig.java). " +
                "Messages are typically sent to '/app/chat.sendMessage' and broadcast to subscribers of '/topic/messages'. " +
                "User-specific errors are sent to '/user/{username}/queue/errors'."
)
public class WebSocketChatController {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatController.class); // Added logger
    private static final DateTimeFormatter DTO_TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME; // Formatter for ChatMessageDto

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Autowired
    public WebSocketChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Handle incoming chat messages via WebSocket",
            description = "Receives a chat message sent by a client to the '/app/chat.sendMessage' destination. " +
                    "The message is then processed, saved, and the persisted message details are broadcast to all subscribers of the '/topic/messages' destination. " +
                    "The sender must be authenticated via the WebSocket session (Principal). " +
                    "The 'senderId' in the ChatMessageDto must match the authenticated user's ID.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "ChatMessageDto containing conversationId, senderId (must match authenticated user), and content. Timestamp is ignored as it's set by server.",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.class))
            )
    )
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/messages")
    public ChatMessageDto handleMessage(ChatMessageDto incomingChatMessageDto, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null || principal.getName() == null) {
            logger.warn("WebSocket: Principal is null in handleMessage. Session ID: {}. Denying message send.", headerAccessor.getSessionId());
            throw new MessageSendException("Cannot send message: User is not authenticated in WebSocket session.");
        }

        String authenticatedUserEmail = principal.getName();
        User authenticatedUser = userRepository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> {
                    logger.error("WebSocket: Authenticated user email '{}' not found in DB. Session ID: {}. Principal: {}",
                            authenticatedUserEmail, headerAccessor.getSessionId(), principal.getName());
                    return new UserNotFoundException("Authenticated user not found in database: " + authenticatedUserEmail);
                });

        String authenticatedUserId = authenticatedUser.getId();

        if (!authenticatedUserId.equals(incomingChatMessageDto.getSenderId())) {
            logger.warn("Security Alert: Mismatched sender ID in WebSocket message. Authenticated User ID: {}, DTO SenderId: {}. Session ID: {}. Principal: {}",
                    authenticatedUserId, incomingChatMessageDto.getSenderId(), headerAccessor.getSessionId(), principal.getName());
            throw new MessageSendException("Sender ID in message does not match authenticated user.");
        }

        // Persist the message via ChatService, which returns the persisted MessageDto
        MessageDto persistedMessageDto = chatService.sendMessage(
                incomingChatMessageDto.getConversationId(),
                authenticatedUserId, // Use validated authenticatedUserId
                incomingChatMessageDto.getContent()
        );

        // Convert the persisted MessageDto (from service) to ChatMessageDto (for WebSocket clients)
        ChatMessageDto broadcastMessage = new ChatMessageDto(
                persistedMessageDto.getConversationId(),
                persistedMessageDto.getSenderId(),
                persistedMessageDto.getEncryptedContent(), // Assuming ChatMessageDto.content maps to MessageDto.encryptedContent
                persistedMessageDto.getSentAt() != null ? persistedMessageDto.getSentAt().format(DTO_TIMESTAMP_FORMATTER) : LocalDateTime.now().format(DTO_TIMESTAMP_FORMATTER) // Format timestamp
        );
        // Note: 'id' and 'isRead' from MessageDto are not part of ChatMessageDto for broadcasting general messages.
        // If they were needed, ChatMessageDto would need those fields.

        logger.info("Broadcasting message for conversationId: {}, senderId: {}", broadcastMessage.getConversationId(), broadcastMessage.getSenderId());
        return broadcastMessage;
    }

    @Operation(summary = "Handle general exceptions from WebSocket message handlers",
            description = "Catches general exceptions occurring during @MessageMapping processing and sends an error message " +
                    "to the originating user's private queue ('/user/{username}/queue/errors').")
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleWebSocketException(Exception exception, Principal principal) {
        String errorMessage = "An error occurred processing your message: " + exception.getMessage();
        String username = (principal != null && principal.getName() != null) ? principal.getName() : "unauthenticated user";
        logger.error("WebSocket Error for user {}: {}", username, exception.getMessage(), exception); // Log with stack trace
        return Map.of("error", errorMessage);
    }

    @Operation(summary = "Handle MessageSendException specifically",
            description = "Catches MessageSendException (e.g., authentication issues, sender ID mismatch) and sends " +
                    "a specific error message to the originating user's private queue ('/user/{username}/queue/errors').")
    @MessageExceptionHandler(MessageSendException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleMessageSendException(MessageSendException exception, Principal principal) {
        String userDisplay = (principal != null && principal.getName() != null) ? principal.getName() : "unknown user";
        logger.warn("MessageSendException for user {}: {}", userDisplay, exception.getMessage()); // Log as WARN as it's often client-side issue
        return Map.of("error", "Message sending failed: " + exception.getMessage());
    }

    @Operation(summary = "Handle UserNotFoundException specifically (if occurring in WebSocket context)",
            description = "Catches UserNotFoundException if an authenticated user from WebSocket principal is not found in DB " +
                    "and sends an error message to the originating user's private queue ('/user/{username}/queue/errors').")
    @MessageExceptionHandler(UserNotFoundException.class)
    @SendToUser("/queue/errors")
    public Map<String, String> handleUserNotFoundException(UserNotFoundException exception, Principal principal) {
        String userDisplay = (principal != null && principal.getName() != null) ? principal.getName() : "unknown user";
        // This is likely a server-side data consistency issue or an issue with token validation if user is gone
        logger.error("UserNotFoundException during WebSocket interaction for user {}: {}", userDisplay, exception.getMessage());
        return Map.of("error", "User context error: " + exception.getMessage());
    }
}