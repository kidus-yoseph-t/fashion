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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;

    // Updated to return ConversationDto
    public ConversationDto startOrGetConversation(String user1Id, String user2Id) {
        Conversation conversation = conversationRepository.findByUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    User user1 = userRepository.findById(user1Id)
                            .orElseThrow(() -> new UserNotFoundException("User1 not found"));
                    User user2 = userRepository.findById(user2Id)
                            .orElseThrow(() -> new UserNotFoundException("User2 not found"));
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1(user1);
                    newConversation.setUser2(user2);
                    return conversationRepository.save(newConversation);
                });
        return toDto(conversation); // Convert to DTO before returning
    }

    // Updated to return MessageDto
    public MessageDto sendMessage(Long conversationId, String senderId, String encryptedContent) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException("Conversation not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new MessageSendException("Sender not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setEncryptedContent(encryptedContent);

        Message savedMessage = messageRepository.save(message); // Save and then convert
        return toDto(savedMessage); // Convert to DTO before returning
    }

    private ConversationDto toDto(Conversation conversation) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conversation.getId());
        dto.setUser1Id(conversation.getUser1().getId());
        dto.setUser2Id(conversation.getUser2().getId());
        dto.setStartedAt(conversation.getStartedAt());
        return dto;
    }

    private MessageDto toDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setEncryptedContent(message.getEncryptedContent());
        dto.setSentAt(message.getSentAt());
        dto.setRead(message.isRead());
        return dto;
    }

    // `getMessages` method signature is correct in ChatService, it already returns List<MessageDto>
    public List<MessageDto> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream().map(this::toDto).toList();
    }

    public List<ConversationDto> getUserConversations(String userId) {
        return conversationRepository.findAllByUserId(userId)
                .stream().map(this::toDto).toList();
    }

    public List<MessageDto> getUnreadMessages(Long conversationId, String userId) {
        return messageRepository.findUnreadMessagesForUser(conversationId, userId)
                .stream().map(this::toDto).toList();
    }

    public void markMessagesAsRead(Long conversationId, String userId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesForUser(conversationId, userId);
        unreadMessages.forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    @Transactional(readOnly = true)
    public int getUnreadMessageCountForUser(String userId) {
        return messageRepository.countUnreadMessagesForRecipient(userId);
    }
}