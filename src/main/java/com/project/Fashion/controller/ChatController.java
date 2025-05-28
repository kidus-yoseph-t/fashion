package com.project.Fashion.controller;

import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;

import com.project.Fashion.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired private ChatService chatService;

    @PostMapping("/start")
    public ResponseEntity<Conversation> startConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id
    ) {
        return ResponseEntity.ok(chatService.startOrGetConversation(user1Id, user2Id));
    }

    @PostMapping("/message")
    public ResponseEntity<Message> sendMessage(
            @RequestParam Long conversationId,
            @RequestParam String senderId,
            @RequestBody String encryptedContent
    ) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, senderId, encryptedContent));
    }

    @GetMapping("/conversation/{id}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(id));
    }

    @GetMapping("/user/{userId}/conversations")
    public ResponseEntity<List<Conversation>> getConversations(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/conversation/{id}/unread/{userId}")
    public ResponseEntity<List<Message>> getUnreadMessages(
            @PathVariable Long id,
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(chatService.getUnreadMessages(id, userId));
    }

    @PostMapping("/conversation/{id}/mark-read/{userId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @PathVariable String userId) {
        chatService.markMessagesAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
}

