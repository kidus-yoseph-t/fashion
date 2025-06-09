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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;

    private MessageDto toDto(Message message) {
        if (message == null) return null;
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        if (message.getConversation() != null) {
            dto.setConversationId(message.getConversation().getId());
        }
        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
        }
        // The content of a message is stored in the 'encryptedContent' field in the model
        dto.setContent(message.getEncryptedContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead());
        return dto;
    }

    private ConversationDto toDto(Conversation conversation, String currentUserId) {
        if (conversation == null) return null;
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setStartedAt(conversation.getStartedAt());

        if (conversation.getUser1() != null) {
            dto.setUser1Id(conversation.getUser1().getId());
            dto.setUser1Name(conversation.getUser1().getFirstName() + " " + conversation.getUser1().getLastName());
        }
        if (conversation.getUser2() != null) {
            dto.setUser2Id(conversation.getUser2().getId());
            dto.setUser2Name(conversation.getUser2().getFirstName() + " " + conversation.getUser2().getLastName());
        }

        // Find the last message to show a preview in the conversation list
        Message lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId()).orElse(null);
        if (lastMessage != null) {
            dto.setLastMessageContent(lastMessage.getEncryptedContent());
            dto.setLastMessageTimestamp(lastMessage.getSentAt());
            if(lastMessage.getSender() != null) {
                dto.setLastMessageSenderId(lastMessage.getSender().getId());
            }
        }

        // Calculate the number of unread messages for the current user
        if (currentUserId != null) {
            dto.setUnreadMessageCount(messageRepository.countByConversationIdAndSenderIdNotAndIsReadFalse(conversation.getId(), currentUserId));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(String userId) {
        logger.debug("Fetching conversations for user ID: {}", userId);
        if(!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }

        List<Conversation> conversations = conversationRepository.findAllByUserId(userId);

        // Map conversations to DTOs and sort them by the last message's timestamp
        return conversations.stream()
                .map(convo -> toDto(convo, userId))
                .sorted(Comparator.comparing(ConversationDto::getLastMessageTimestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public ConversationDto startOrGetConversation(String user1Id, String user2Id) {
        User user1 = userRepository.findById(user1Id).orElseThrow(() -> new UserNotFoundException("User1 not found: " + user1Id));
        User user2 = userRepository.findById(user2Id).orElseThrow(() -> new UserNotFoundException("User2 not found: " + user2Id));

        // Find an existing conversation or create a new one
        Conversation conversation = conversationRepository.findByUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    logger.info("No existing conversation found. Creating new one for users {} and {}", user1Id, user2Id);
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1(user1);
                    newConversation.setUser2(user2);
                    return conversationRepository.save(newConversation);
                });
        return toDto(conversation, user1Id);
    }

    public MessageDto sendMessage(Long conversationId, String senderId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found: " + conversationId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new MessageSendException("Sender not found: " + senderId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setEncryptedContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);
        return toDto(savedMessage);
    }

    public void markMessagesAsRead(Long conversationId, String userId) {
        logger.info("Marking messages as read for user {} in conversation {}", userId, conversationId);
        List<Message> unreadMessages = messageRepository.findUnreadMessagesForUser(conversationId, userId);
        if (!unreadMessages.isEmpty()) {
            unreadMessages.forEach(msg -> msg.setRead(true));
            messageRepository.saveAll(unreadMessages);
            logger.info("Marked {} messages as read for user {} in conversation {}", unreadMessages.size(), userId, conversationId);
        }
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getUnreadMessageCountForUser(String userId) {
        return messageRepository.countUnreadMessagesForRecipient(userId);
    }
}