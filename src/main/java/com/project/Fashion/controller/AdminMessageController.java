// src/main/java/com/yourcompany/yourapp/controller/AdminMessageController.java
package com.project.Fashion.controller;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.service.ContactMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/AdminMessages") // Matches your frontend adminService.js
@CrossOrigin(origins = "http://localhost:5173" , originPatterns = "http://localhost:5174") // Adjust if your frontend is on a different port
@PreAuthorize("hasRole('ADMIN')") // Ensure only ADMINs can access these endpoints
public class AdminMessageController {

    @Autowired
    private ContactMessageService contactMessageService;

    // GET all messages (for the admin messages list)
    @GetMapping
    public ResponseEntity<List<ContactMessage>> getAllAdminMessages() {
        List<ContactMessage> messages = contactMessageService.getAllMessages();
        return ResponseEntity.ok(messages);
    }

    // DELETE a message
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdminMessage(@PathVariable Long id) {
        contactMessageService.deleteMessage(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // PATCH to update message status (read/unread)
    @PatchMapping("/{id}/status")
    public ResponseEntity<ContactMessage> updateMessageStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdate) {
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || (!newStatus.equals("read") && !newStatus.equals("unread"))) {
            return ResponseEntity.badRequest().build(); // Invalid status provided
        }

        ContactMessage updatedMessage = contactMessageService.updateMessageStatus(id, newStatus);
        if (updatedMessage != null) {
            return ResponseEntity.ok(updatedMessage);
        } else {
            return ResponseEntity.notFound().build(); // Message not found
        }
    }

    // Optional: GET a single message detail (if needed)
    @GetMapping("/{id}")
    public ResponseEntity<ContactMessage> getMessageById(@PathVariable Long id) {
        return contactMessageService.getMessageById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}