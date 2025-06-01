// src/main/java/com/yourcompany/yourapp/service/ContactMessageService.java
package com.project.Fashion.service;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContactMessageService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    // Method to save a new contact message (from the contact form)
    public ContactMessage saveMessage(ContactMessage message) {
        return contactMessageRepository.save(message);
    }

    // Method for admins to get all messages
    public List<ContactMessage> getAllMessages() {
        return contactMessageRepository.findAll();
    }

    // Method for admins to get a single message by ID
    public Optional<ContactMessage> getMessageById(Long id) {
        return contactMessageRepository.findById(id);
    }

    // Method for admins to delete a message
    public void deleteMessage(Long id) {
        contactMessageRepository.deleteById(id);
    }

    // Method for admins to update message status (read/unread)
    public ContactMessage updateMessageStatus(Long id, String newStatus) {
        Optional<ContactMessage> optionalMessage = contactMessageRepository.findById(id);
        if (optionalMessage.isPresent()) {
            ContactMessage message = optionalMessage.get();
            message.setStatus(newStatus);
            return contactMessageRepository.save(message);
        }
        // You might want to throw an exception here if message is not found
        return null;
    }
}