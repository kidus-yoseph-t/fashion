package com.project.Fashion.controller;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.dto.ChatMessageRequestDto; // Import new DTO for sending messages
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message; // Keep if needed for model access directly, though DTOs are preferred
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
            @RequestParam String user1Id,
            @RequestParam String user2Id) {

        String authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(user1Id) && !authenticatedUserId.equals(user2Id)) {
            throw new AccessDeniedException("Authenticated user must be one of the participants to start this conversation.");
        }

        // Corrected: Pass only 2 arguments to chatService.startOrGetConversation
        ConversationDto conversationDto = chatService.startOrGetConversation(user1Id, user2Id);
        return ResponseEntity.ok(conversationDto);
    }

    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageDto> sendMessage(@Valid @RequestBody ChatMessageRequestDto messageRequestDto) {
        String authenticatedSenderId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(messageRequestDto.getConversationId())
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + messageRequestDto.getConversationId()));
        authorizeConversationParticipant(conversation, authenticatedSenderId);

        // This call is now compatible as chatService.sendMessage returns MessageDto
        MessageDto messageDto = chatService.sendMessage(
                messageRequestDto.getConversationId(),
                authenticatedSenderId,
                messageRequestDto.getContent()
        );
        return ResponseEntity.ok(messageDto);
    }

    @GetMapping("/conversation/{conversationId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long conversationId) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId);

        // Corrected: Pass only 1 argument to chatService.getMessages
        return ResponseEntity.ok(chatService.getMessages(conversationId));
    }

    @GetMapping("/user/me/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getMyConversations() {
        String authenticatedUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(chatService.getUserConversations(authenticatedUserId));
    }

    @GetMapping("/user/{userId}/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationDto>> getUserConversations(@PathVariable String userId) {
        String authenticatedUserId = getAuthenticatedUserId();
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("User can only retrieve their own conversations.");
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Integer>> getMyUnreadMessageCount() {
        String authenticatedUserId = getAuthenticatedUserId();
        int unreadCount = chatService.getUnreadMessageCountForUser(authenticatedUserId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}