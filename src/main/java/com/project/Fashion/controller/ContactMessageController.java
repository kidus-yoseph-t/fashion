package com.project.Fashion.controller;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.service.ContactMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact") // This is the endpoint your Contact.jsx will hit
@CrossOrigin(origins = "http://localhost:5173") // Adjust if your frontend is on a different port
public class ContactMessageController {

    @Autowired
    private ContactMessageService contactMessageService;

    @PostMapping
    public ResponseEntity<String> submitContactMessage(@RequestBody ContactMessage message) {
        try {
            contactMessageService.saveMessage(message);
            return ResponseEntity.status(HttpStatus.CREATED).body("Message sent successfully!");
        } catch (Exception e) {
            System.err.println("Error saving contact message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message.");
        }
    }
}