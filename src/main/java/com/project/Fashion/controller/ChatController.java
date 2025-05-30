package com.project.Fashion.controller;

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
    public ResponseEntity<Conversation> startConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id) {
        Conversation conversation = chatService.startOrGetConversation(user1Id, user2Id);
        return ResponseEntity.ok(conversation);
    }

    @PostMapping("/message")
    public ResponseEntity<Message> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam String senderId,
            @RequestBody String encryptedContent) {
        Message message = chatService.sendMessage(conversationId, senderId, encryptedContent);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/conversation/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<List<Conversation>> getUserConversations(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/conversation/{id}/unread/{userId}")
    public ResponseEntity<List<Message>> getUnreadMessages(
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
