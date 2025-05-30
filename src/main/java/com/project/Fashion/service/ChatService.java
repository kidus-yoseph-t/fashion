package com.project.Fashion.service;

import com.project.Fashion.model.Conversation;
import com.project.Fashion.model.Message;
import com.project.Fashion.model.User;
import com.project.Fashion.repository.ConversationRepository;
import com.project.Fashion.repository.MessageRepository;
import com.project.Fashion.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {

    @Autowired private UserRepository userRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;

    public Conversation startOrGetConversation(String user1Id, String user2Id) {
        return conversationRepository.findByUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    User user1 = userRepository.findById(user1Id)
                            .orElseThrow(() -> new IllegalArgumentException("User1 not found"));
                    User user2 = userRepository.findById(user2Id)
                            .orElseThrow(() -> new IllegalArgumentException("User2 not found"));
                    Conversation conversation = new Conversation();
                    conversation.setUser1(user1);
                    conversation.setUser2(user2);
                    return conversationRepository.save(conversation);
                });
    }

    public Message sendMessage(Long conversationId, String senderId, String encryptedContent) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setEncryptedContent(encryptedContent);

        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    public List<Conversation> getUserConversations(String userId) {
        return conversationRepository.findAllByUserId(userId);
    }

    public List<Message> getUnreadMessages(Long conversationId, String userId) {
        return messageRepository.findUnreadMessagesForUser(conversationId, userId);
    }

    public void markMessagesAsRead(Long conversationId, String userId) {
        List<Message> unreadMessages = getUnreadMessages(conversationId, userId);
        unreadMessages.forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(unreadMessages);
    }
}
