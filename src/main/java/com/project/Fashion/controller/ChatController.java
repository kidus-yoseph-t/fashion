package com.project.Fashion.controller;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.dto.ChatMessageRequestDto;
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.User;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.repository.ConversationRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.ConversationNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // For validating request DTOs
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Management", description = "APIs for managing user conversations and messages.")
@SecurityRequirement(name = "bearerAuth") // All chat operations require authentication
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository; // Used for auth checks

    @Autowired
    private UserRepository userRepository;

    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName();
        User authenticatedUser = userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
        return authenticatedUser.getId();
    }

    private void authorizeConversationParticipant(Conversation conversation, String authenticatedUserId) {
        if (conversation.getUser1() == null || conversation.getUser2() == null) {
            // This scenario should ideally not happen if conversation data is consistent.
            throw new ConversationNotFoundException("Conversation participants not found for conversation ID: " + conversation.getId() + ". Data integrity issue suspected.");
        }
        if (!conversation.getUser1().getId().equals(authenticatedUserId) &&
                !conversation.getUser2().getId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("User is not a participant in this conversation.");
        }
    }

    @Operation(summary = "Start or get an existing conversation between two users",
            description = "Initiates a new conversation between two specified user IDs or retrieves an existing one if it already exists. The authenticated user must be one of the participants.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation started or retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ConversationDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., user IDs are the same, or one of the user IDs is invalid/not found by service)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"message\":\"User IDs cannot be the same\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Authenticated user is not one of the specified participants)"),
            @ApiResponse(responseCode = "404", description = "One or both users not found (if service throws UserNotFoundException)")
    })
    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ConversationDto> startConversation(
            @Parameter(description = "ID of the first user in the conversation", required = true, example = "user-uuid-1") @RequestParam String user1Id,
            @Parameter(description = "ID of the second user in the conversation", required = true, example = "user-uuid-2") @RequestParam String user2Id) {

        String authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(user1Id) && !authenticatedUserId.equals(user2Id)) {
            throw new AccessDeniedException("Authenticated user must be one of the participants to start this conversation.");
        }
        if (user1Id.equals(user2Id)) {
            throw new IllegalArgumentException("User IDs cannot be the same for a conversation.");
        }

        ConversationDto conversationDto = chatService.startOrGetConversation(user1Id, user2Id);
        return ResponseEntity.ok(conversationDto);
    }

    @Operation(summary = "Send a message in a conversation",
            description = "Allows an authenticated user to send a message within an existing conversation. The sender must be a participant in the conversation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully", // Consider 201 Created
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MessageDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (e.g., empty message content if validated by DTO)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"content\":\"Message content cannot be empty\"}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a participant in the conversation)"),
            @ApiResponse(responseCode = "404", description = "Conversation or Sender not found")
    })
    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageDto> sendMessage(@Valid @RequestBody ChatMessageRequestDto messageRequestDto) {
        String authenticatedSenderId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(messageRequestDto.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + messageRequestDto.getConversationId()));
        authorizeConversationParticipant(conversation, authenticatedSenderId);

        MessageDto messageDto = chatService.sendMessage(
                messageRequestDto.getConversationId(),
                authenticatedSenderId, // Use authenticated user's ID as sender
                messageRequestDto.getContent()
        );
        // Consider HttpStatus.CREATED (201)
        return ResponseEntity.ok(messageDto);
    }

    @Operation(summary = "Get messages for a specific conversation",
            description = "Retrieves all messages for a given conversation ID. The authenticated user must be a participant in the conversation.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved messages",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MessageDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a participant in the conversation)"),
            @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @GetMapping("/conversation/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDto>> getMessages(
            @Parameter(description = "ID of the conversation to fetch messages for", required = true, example = "1")
            @PathVariable Long conversationId) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId);

        return ResponseEntity.ok(chatService.getMessages(conversationId));
    }

    @Operation(summary = "Get all conversations for the authenticated user",
            description = "Retrieves a list of all conversations the currently authenticated user is a participant in.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's conversations",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = ConversationDto.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user not found (should not happen if token is valid)")
    })
    @GetMapping("/user/me/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getMyConversations() {
        String authenticatedUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(chatService.getUserConversations(authenticatedUserId));
    }

    @Operation(summary = "Get all conversations for a specific user ID (self only)",
            description = "Retrieves a list of all conversations for the user whose ID is provided. The authenticated user must match the `userId` in the path.",
            deprecated = true, // Deprecated because /user/me/conversations is preferred
            hidden = true) // Hidden from Swagger UI as it's redundant with /user/me/conversations
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's conversations"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden (Trying to access another user's conversations)")
    })
    @GetMapping("/user/{userId}/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getUserConversations(
            @Parameter(description = "ID of the user whose conversations to retrieve", required = true, example = "user-uuid-1")
            @PathVariable String userId) {
        String authenticatedUserId = getAuthenticatedUserId();
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("User can only retrieve their own conversations.");
        }
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @Operation(summary = "Mark messages in a conversation as read",
            description = "Marks all unread messages sent by the other participant in a conversation as read for the authenticated user. The authenticated user must be a participant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages marked as read successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "403", description = "Forbidden (User is not a participant in the conversation)"),
            @ApiResponse(responseCode = "404", description = "Conversation not found")
    })
    @PostMapping("/conversation/{conversationId}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markMessagesAsRead(
            @Parameter(description = "ID of the conversation where messages should be marked as read", required = true, example = "1")
            @PathVariable Long conversationId) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId);

        chatService.markMessagesAsRead(conversationId, authenticatedUserId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get unread message count for the authenticated user",
            description = "Retrieves the total number of unread messages across all conversations for the currently authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved unread message count",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object", example = "{\"unreadCount\": 5}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized (Token missing or invalid)"),
            @ApiResponse(responseCode = "404", description = "Authenticated user not found (should not happen if token is valid)")
    })
    @GetMapping("/user/me/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getMyUnreadMessageCount() {
        String authenticatedUserId = getAuthenticatedUserId();
        int unreadCount = chatService.getUnreadMessageCountForUser(authenticatedUserId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}
