package com.project.Fashion.controller;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.dto.ChatMessageRequestDto; // Import new DTO for sending messages
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;
import com.project.Fashion.model.User;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.repository.UserRepository; // Keep for getAuthenticatedUserId
import com.project.Fashion.repository.ConversationRepository; // Keep for direct lookups if needed by auth logic
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.ConversationNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
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
            // This might happen if conversation entity is not fully loaded or is malformed
            throw new ConversationNotFoundException("Conversation participants not found for conversation ID: " + conversation.getId());
        }
        if (!conversation.getUser1().getId().equals(authenticatedUserId) &&
                !conversation.getUser2().getId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("User is not a participant in this conversation.");
        }
    }

    @PostMapping("/start")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can attempt to start
    public ResponseEntity<ConversationDto> startConversation(
            @RequestParam String user1Id, // ID of the first user (could be the authenticated user)
            @RequestParam String user2Id) { // ID of the second user

        String authenticatedUserId = getAuthenticatedUserId();

        // Authorization: Ensure the authenticated user is one of the participants
        if (!authenticatedUserId.equals(user1Id) && !authenticatedUserId.equals(user2Id)) {
            throw new AccessDeniedException("Authenticated user must be one of the participants to start this conversation.");
        }

        // ChatService.startOrGetConversation should return the rich ConversationDto
        ConversationDto conversationDto = chatService.startOrGetConversation(user1Id, user2Id, authenticatedUserId);
        return ResponseEntity.ok(conversationDto);
    }

    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageDto> sendMessage(@Valid @RequestBody ChatMessageRequestDto messageRequestDto) {
        String authenticatedSenderId = getAuthenticatedUserId();

        // Fetch the conversation to ensure the sender is a participant
        Conversation conversation = conversationRepository.findById(messageRequestDto.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + messageRequestDto.getConversationId()));
        authorizeConversationParticipant(conversation, authenticatedSenderId);

        // Proceed to send message using authenticatedSenderId, not one from DTO for security
        // ChatService.sendMessage now takes plain content
        MessageDto messageDto = chatService.sendMessage(
                messageRequestDto.getConversationId(),
                authenticatedSenderId, // Use verified sender ID
                messageRequestDto.getContent()
        );
        // The returned MessageDto should be the rich version with senderName etc.
        return ResponseEntity.ok(messageDto);
    }

    @GetMapping("/conversation/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long conversationId) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId);

        // ChatService.getMessages should return List<MessageDto> with senderName and plain content
        return ResponseEntity.ok(chatService.getMessages(conversationId, authenticatedUserId));
    }

    // Endpoint for the authenticated user to get their own conversations
    @GetMapping("/user/me/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getMyConversations() {
        String authenticatedUserId = getAuthenticatedUserId();
        // ChatService.getUserConversations should return List<ConversationDto>
        // with participant names, last message, and unread count for the authenticatedUserId
        return ResponseEntity.ok(chatService.getUserConversations(authenticatedUserId));
    }

    // This specific endpoint might be redundant if /user/me/conversations is used.
    // Kept for now if it serves a different purpose or if frontend uses it explicitly.
    @GetMapping("/user/{userId}/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getUserConversations(@PathVariable String userId) {
        String authenticatedUserId = getAuthenticatedUserId();
        if (!authenticatedUserId.equals(userId)) {
            // Allow admins to view other users' conversations if needed, otherwise restrict.
            // For now, restricting to self unless admin role is checked explicitly.
            // if (!isAdmin(authenticatedUser)) {
            throw new AccessDeniedException("User can only retrieve their own conversations.");
            // }
        }
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }


    @PostMapping("/conversation/{conversationId}/mark-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markMessagesAsRead(@PathVariable Long conversationId) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId);

        chatService.markMessagesAsRead(conversationId, authenticatedUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/me/unread-count")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can get their own count
    public ResponseEntity<Map<String, Integer>> getMyUnreadMessageCount() {
        String authenticatedUserId = getAuthenticatedUserId();
        int unreadCount = chatService.getUnreadMessageCountForUser(authenticatedUserId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}
