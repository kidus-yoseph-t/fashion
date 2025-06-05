package com.project.Fashion.service;

import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ContactMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ContactMessageService.class);

    private final ContactMessageRepository contactMessageRepository;
    private final SimpMessagingTemplate messagingTemplate; // Inject SimpMessagingTemplate

    // WebSocket topic for new contact messages for admins
    private static final String ADMIN_NEW_CONTACT_MESSAGE_TOPIC = "/topic/admin/newContactMessage";

    @Autowired
    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                 SimpMessagingTemplate messagingTemplate) { // Add to constructor
        this.contactMessageRepository = contactMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ContactMessage saveMessage(ContactMessage message) {
        // Ensure createdAt is set if not handled automatically by @PrePersist in model
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(java.time.LocalDateTime.now());
        }
        // Ensure status is set if not handled by default in model
        if (message.getStatus() == null || message.getStatus().isEmpty()) {
            message.setStatus("unread");
        }

        ContactMessage savedMessage = contactMessageRepository.save(message);
        logger.info("Contact message ID {} saved from sender: {}", savedMessage.getId(), savedMessage.getSenderEmail());

        // After saving, send the message to the admin WebSocket topic
        if (savedMessage != null) {
            logger.info("Sending new contact message notification to WebSocket topic: {}", ADMIN_NEW_CONTACT_MESSAGE_TOPIC);
            messagingTemplate.convertAndSend(ADMIN_NEW_CONTACT_MESSAGE_TOPIC, savedMessage);
            // Frontend (admin dashboard) should subscribe to "/topic/admin/newContactMessage"
            // The payload will be the savedContactMessage object (JSON).
        }
        return savedMessage;
    }

    public List<ContactMessage> getAllMessages() {
        logger.debug("Fetching all contact messages for admin.");
        return contactMessageRepository.findAll();
    }

    public Optional<ContactMessage> getMessageById(Long id) {
        logger.debug("Fetching contact message by ID: {}", id);
        return contactMessageRepository.findById(id);
    }

    @Transactional
    public void deleteMessage(Long id) {
        if (contactMessageRepository.existsById(id)) {
            contactMessageRepository.deleteById(id);
            logger.info("Deleted contact message with ID: {}", id);
        } else {
            logger.warn("Attempted to delete non-existent contact message with ID: {}", id);
        }
    }

    @Transactional
    public ContactMessage updateMessageStatus(Long id, String newStatus) {
        Optional<ContactMessage> optionalMessage = contactMessageRepository.findById(id);
        if (optionalMessage.isPresent()) {
            ContactMessage message = optionalMessage.get();
            message.setStatus(newStatus.toLowerCase()); // Store status consistently (e.g., lowercase)
            ContactMessage updatedMessage = contactMessageRepository.save(message);
            logger.info("Updated status of contact message ID {} to '{}'", id, newStatus);
            // Optionally, you could also send an update to a WebSocket topic if admins need real-time status changes
            // messagingTemplate.convertAndSend("/topic/admin/contactMessageUpdated", updatedMessage);
            return updatedMessage;
        }
        logger.warn("Attempted to update status for non-existent contact message ID: {}", id);
        return null; // Or throw an exception
    }
}
