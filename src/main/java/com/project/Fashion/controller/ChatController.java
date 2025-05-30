package com.project.Fashion.controller;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;
import com.project.Fashion.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired private ChatService chatService;

    @PostMapping("/start")
    public ResponseEntity<ConversationDto> startConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id) {

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
            @RequestParam String senderId,
            @RequestBody String encryptedContent
    ) {
        Message message = chatService.sendMessage(conversationId, senderId, encryptedContent);

        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setEncryptedContent(message.getEncryptedContent());
        dto.setSenderId(message.getSender().getId());
        dto.setSentAt(message.getSentAt());

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/conversation/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<List<ConversationDto>> getUserConversations(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/conversation/{id}/unread/{userId}")
    public ResponseEntity<List<MessageDto>> getUnreadMessages(
            @PathVariable Long id,
            @PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUnreadMessages(id, userId));
    }

    @PostMapping("/conversation/{id}/mark-read/{userId}")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long id,
            @PathVariable String userId) {
        chatService.markMessagesAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
}
