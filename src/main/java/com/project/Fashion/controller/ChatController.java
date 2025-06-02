package com.project.Fashion.controller;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;
import com.project.Fashion.model.User;
import com.project.Fashion.service.ChatService;
import com.project.Fashion.repository.ConversationRepository;
import com.project.Fashion.repository.UserRepository;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.exception.exceptions.ConversationNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// Consider using UserDetails if your principal is always UserDetails
// import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Helper method to get the authenticated user's ID.
     * Throws UserNotFoundException if the authenticated user cannot be found in the repository.
     * @return The ID of the authenticated user.
     */
    private String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("User is not authenticated.");
        }
        String currentPrincipalName = authentication.getName(); // This is usually the email

        User authenticatedUser = userRepository.findByEmail(currentPrincipalName)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found with email: " + currentPrincipalName));
        return authenticatedUser.getId();
    }

    /**
     * Helper method to check if the authenticated user is a participant in the given conversation.
     * Throws AccessDeniedException if the user is not a participant.
     * @param conversation The conversation to check.
     * @param authenticatedUserId The ID of the authenticated user.
     */
    private void authorizeConversationParticipant(Conversation conversation, String authenticatedUserId) {
        if (!conversation.getUser1().getId().equals(authenticatedUserId) &&
                !conversation.getUser2().getId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("User is not a participant in this conversation.");
        }
    }

    @PostMapping("/start")
    public ResponseEntity<ConversationDto> startConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id) {

        String authenticatedUserId = getAuthenticatedUserId(); // Use the helper method

        // AUTHORIZATION CHECK: Ensure the authenticated user is one of the participants
        if (!authenticatedUserId.equals(user1Id) && !authenticatedUserId.equals(user2Id)) {
            throw new AccessDeniedException("Authenticated user must be one of the participants to start this conversation.");
        }

        Conversation conversation = chatService.startOrGetConversation(user1Id, user2Id);

        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setUser1Id(conversation.getUser1().getId());
        dto.setUser2Id(conversation.getUser2().getId());
        dto.setStartedAt(conversation.getStartedAt());

        return ResponseEntity.ok(dto);
    }


    @PostMapping("/message")
    public ResponseEntity<MessageDto> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam String senderId, // This is the ID of the user who is sending the message
            @RequestBody String encryptedContent // Assuming content is passed in the request body
    ) {
        String authenticatedUserId = getAuthenticatedUserId();

        // Authorization: Ensure the senderId matches the authenticated user
        if (!authenticatedUserId.equals(senderId)) {
            throw new AccessDeniedException("Authenticated user does not match the sender ID.");
        }

        // Fetch the conversation to ensure the sender is a participant
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + conversationId));
        authorizeConversationParticipant(conversation, authenticatedUserId); // Check if sender is participant

        // Proceed to send message
        Message message = chatService.sendMessage(conversationId, senderId, encryptedContent);

        // Convert to DTO
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setEncryptedContent(message.getEncryptedContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead()); // Include read status

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/conversation/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long id) {
        String authenticatedUserId = getAuthenticatedUserId();

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + id));

        authorizeConversationParticipant(conversation, authenticatedUserId);

        return ResponseEntity.ok(chatService.getMessages(id));
    }

    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<List<ConversationDto>> getUserConversations(@PathVariable String userId) {
        String authenticatedUserId = getAuthenticatedUserId();

        // Authorization: Ensure the authenticated user is requesting their own conversations
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("User can only retrieve their own conversations.");
        }

        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/conversation/{id}/unread/{userId}")
    public ResponseEntity<List<MessageDto>> getUnreadMessages(
            @PathVariable Long id, // Conversation ID
            @PathVariable String userId) { // The user FOR WHOM we are checking unread messages

        String authenticatedUserId = getAuthenticatedUserId();

        // Authorization Check 1: The authenticated user must be the same as the userId in the path.
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("Authenticated user does not match user specified in path for unread messages.");
        }

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + id));

        // Authorization Check 2: The authenticated user (who is also 'userId' from path) must be a participant.
        authorizeConversationParticipant(conversation, authenticatedUserId);

        return ResponseEntity.ok(chatService.getUnreadMessages(id, userId));
    }

    @PostMapping("/conversation/{id}/mark-read/{userId}")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long id, // Conversation ID
            @PathVariable String userId) { // The user FOR WHOM we are marking messages as read

        String authenticatedUserId = getAuthenticatedUserId();

        // Authorization Check 1: Authenticated user must be the 'userId' from path.
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("Authenticated user does not match user specified in path for marking messages as read.");
        }

        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found with id: " + id));

        // Authorization Check 2: The authenticated user (who is also 'userId' from path) must be a participant.
        authorizeConversationParticipant(conversation, authenticatedUserId);

        chatService.markMessagesAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
}
