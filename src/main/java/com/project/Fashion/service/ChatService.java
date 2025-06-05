package com.project.Fashion.service;

import com.project.Fashion.dto.ConversationDto;
import com.project.Fashion.dto.MessageDto;
import com.project.Fashion.exception.exceptions.ConversationNotFoundException;
import com.project.Fashion.exception.exceptions.MessageSendException;
import com.project.Fashion.exception.exceptions.UserNotFoundException;
import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ConversationRepository;
import com.project.Fashion.repository.MessageRepository;
import com.project.Fashion.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // For MessageDto's sentAt
import java.util.List;
import java.util.stream.Collectors; // For mapping lists

@Service
@Transactional // class-level transactional for default behavior
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;

    public ConversationDto startOrGetConversation(String user1Id, String user2Id) {
        logger.info("Attempting to start or get conversation between user {} and user {}", user1Id, user2Id);
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> {
                    logger.warn("User1 not found with ID: {}", user1Id);
                    return new UserNotFoundException("User1 not found with ID: " + user1Id);
                });
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> {
                    logger.warn("User2 not found with ID: {}", user2Id);
                    return new UserNotFoundException("User2 not found with ID: " + user2Id);
                });

        Conversation conversation = conversationRepository.findByUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    logger.info("No existing conversation found. Creating new one for users {} and {}", user1Id, user2Id);
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1(user1);
                    newConversation.setUser2(user2);
                    // startedAt is set by default in Conversation entity
                    return conversationRepository.save(newConversation);
                });
        logger.info("Conversation ID {} obtained for users {} and {}", conversation.getId(), user1Id, user2Id);
        return toDto(conversation);
    }

    // Changed to return MessageDto
    public MessageDto sendMessage(Long conversationId, String senderId, String encryptedContent) {
        logger.info("Attempting to send message from sender {} to conversation {}", senderId, conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> {
                    logger.warn("Conversation not found with ID: {} while sending message", conversationId);
                    return new ConversationNotFoundException("Conversation not found with ID: " + conversationId);
                });
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> {
                    logger.warn("Sender not found with ID: {} while sending message", senderId);
                    // Using MessageSendException as per original controller logic for user not found in send context
                    return new MessageSendException("Sender not found with ID: " + senderId);
                });

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setEncryptedContent(encryptedContent);
        // sentAt is set by default in Message entity, isRead defaults to false

        Message savedMessage = messageRepository.save(message);
        logger.info("Message ID {} saved successfully from sender {} to conversation {}", savedMessage.getId(), senderId, conversationId);
        return toDto(savedMessage); // Convert and return the persisted MessageDto
    }

    // Helper to convert Conversation entity to ConversationDto
    private ConversationDto toDto(Conversation conversation) {
        if (conversation == null) return null;
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        if (conversation.getUser1() != null) dto.setUser1Id(conversation.getUser1().getId());
        if (conversation.getUser2() != null) dto.setUser2Id(conversation.getUser2().getId());
        dto.setStartedAt(conversation.getStartedAt());
        return dto;
    }

    // Helper to convert Message entity to MessageDto
    // This DTO is used for REST API responses and now as service layer return.
    private MessageDto toDto(Message message) {
        if (message == null) return null;
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        if (message.getConversation() != null) dto.setConversationId(message.getConversation().getId());
        if (message.getSender() != null) dto.setSenderId(message.getSender().getId());
        dto.setEncryptedContent(message.getEncryptedContent()); // Field name in MessageDto
        dto.setSentAt(message.getSentAt());                     // LocalDateTime
        dto.setRead(message.isRead());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(Long conversationId) {
        logger.debug("Fetching messages for conversation ID: {}", conversationId);
        if (!conversationRepository.existsById(conversationId)) {
            logger.warn("Attempted to fetch messages for non-existent conversation ID: {}", conversationId);
            throw new ConversationNotFoundException("Conversation not found with ID: " + conversationId);
        }
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String userId) {
        logger.debug("Fetching conversations for user ID: {}", userId);
        if(!userRepository.existsById(userId)) {
            logger.warn("Attempted to fetch conversations for non-existent user ID: {}", userId);
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        return conversationRepository.findAllByUserId(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // This method might not be directly called by ChatController anymore if combined with markMessagesAsRead
    @Transactional(readOnly = true)
    public List<MessageDto> getUnreadMessages(Long conversationId, String userId) {
        logger.debug("Fetching unread messages for user {} in conversation {}", userId, conversationId);
        return messageRepository.findUnreadMessagesForUser(conversationId, userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void markMessagesAsRead(Long conversationId, String userId) {
        logger.info("Marking messages as read for user {} in conversation {}", userId, conversationId);
        List<Message> unreadMessages = messageRepository.findUnreadMessagesForUser(conversationId, userId);
        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(msg -> msg.setRead(true));
            messageRepository.saveAll(unreadMessages);
            logger.info("Marked {} messages as read for user {} in conversation {}", unreadMessages.size(), userId, conversationId);
        } else {
            logger.info("No unread messages to mark as read for user {} in conversation {}", userId, conversationId);
        }
    }

    @Transactional(readOnly = true)
    public int getUnreadMessageCountForUser(String userId) {
        logger.debug("Counting unread messages for user ID: {}", userId);
        if(!userRepository.existsById(userId)) {
            logger.warn("Attempted to count unread messages for non-existent user ID: {}", userId);
            // Depending on requirements, could return 0 or throw UserNotFoundException
            return 0;
        }
        return messageRepository.countUnreadMessagesForRecipient(userId);
    }
}