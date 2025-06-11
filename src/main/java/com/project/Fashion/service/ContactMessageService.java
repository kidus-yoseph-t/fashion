package com.project.Fashion.service;

import com.project.Fashion.dto.ContactMessageRequestDto;
import com.project.Fashion.model.ContactMessage;
import com.project.Fashion.repository.ContactMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
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
        ContactMessage message = new ContactMessage();
        message.setSenderName(dto.getSenderName());
        message.setSenderEmail(dto.getSenderEmail());
        message.setSubject(dto.getSubject());
        message.setMessage(dto.getMessage());

        ContactMessage savedMessage = contactMessageRepository.save(message);
        logger.info("Contact message ID {} saved from sender: {}", savedMessage.getId(), savedMessage.getSenderEmail());

        // This method will now be executed in a separate thread and will not affect this transaction
        sendAdminNotification(savedMessage);

        return savedMessage;
    }

    @Async // Mark this method to run asynchronously
    public void sendAdminNotification(ContactMessage savedMessage) {
        if (messagingTemplate != null) {
            try {
                logger.info("Sending async new contact message notification to topic: {}", ADMIN_NEW_CONTACT_MESSAGE_TOPIC);
                messagingTemplate.convertAndSend(ADMIN_NEW_CONTACT_MESSAGE_TOPIC, savedMessage);
            } catch (Exception e) {
                logger.error("Async notification failed for message ID {}. The message was saved successfully, but the WebSocket push failed.", savedMessage.getId(), e);
            }
        }
    }

    public List<ContactMessage> getAllMessages() {
        return contactMessageRepository.findAll();
    }

    public Optional<ContactMessage> getMessageById(Long id) {
        return contactMessageRepository.findById(id);
    }

    @Transactional
    public void deleteMessage(Long id) {
        if (contactMessageRepository.existsById(id)) {
            contactMessageRepository.deleteById(id);
        }
    }

    @Transactional
    public ContactMessage updateMessageStatus(Long id, String newStatus) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + id));

        message.setStatus(newStatus.toLowerCase());
        return contactMessageRepository.save(message);
    }
}