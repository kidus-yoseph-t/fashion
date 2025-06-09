package com.project.Fashion.service;

import com.project.Fashion.dto.ContactMessageRequestDto;
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
    private final SimpMessagingTemplate messagingTemplate;

    private static final String ADMIN_NEW_CONTACT_MESSAGE_TOPIC = "/topic/admin/newContactMessage";

    // Use nullable annotation to indicate messagingTemplate might not be present in all environments (e.g., certain tests)
    @Autowired
    public ContactMessageService(ContactMessageRepository contactMessageRepository,
                                 Optional<SimpMessagingTemplate> messagingTemplate) {
        this.contactMessageRepository = contactMessageRepository;
        this.messagingTemplate = messagingTemplate.orElse(null);
        if (this.messagingTemplate == null) {
            logger.warn("SimpMessagingTemplate is not available. Real-time notifications for contact messages will be disabled.");
        }
    }

    @Transactional
    public ContactMessage saveMessageFromDto(ContactMessageRequestDto dto) {
        // Map DTO to entity within the service
        ContactMessage message = new ContactMessage();
        message.setSenderName(dto.getSenderName());
        message.setSenderEmail(dto.getSenderEmail());
        message.setSubject(dto.getSubject());
        message.setMessage(dto.getMessage());
        // createdAt and status are handled by the entity's defaults

        // Save the message to the database first
        ContactMessage savedMessage = contactMessageRepository.save(message);
        logger.info("Contact message ID {} saved from sender: {}", savedMessage.getId(), savedMessage.getSenderEmail());

        // --- REFACTORED: Defensive check for real-time notification ---
        // Now, even if the messaging template fails, the message is still saved.
        if (messagingTemplate != null) {
            try {
                logger.info("Sending new contact message notification to WebSocket topic: {}", ADMIN_NEW_CONTACT_MESSAGE_TOPIC);
                messagingTemplate.convertAndSend(ADMIN_NEW_CONTACT_MESSAGE_TOPIC, savedMessage);
            } catch (Exception e) {
                // Log the error but don't fail the entire transaction
                logger.error("Failed to send real-time contact message notification. The message was saved successfully, but the WebSocket push failed.", e);
            }
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
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id)); // Or a custom exception

        message.setStatus(newStatus.toLowerCase());
        ContactMessage updatedMessage = contactMessageRepository.save(message);
        logger.info("Updated status of contact message ID {} to '{}'", id, newStatus);

        return updatedMessage;
    }
}