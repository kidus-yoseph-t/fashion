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

    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ConversationRepository convoRepo;
    @Autowired
    private MessageRepository messageRepo;

    public Conversation startOrGetConversation(String user1Id, String user2Id) {
        return convoRepo.findByUsers(user1Id, user2Id)
                .orElseGet(() -> {
                    User user1 = userRepo.findById(user1Id).orElseThrow();
                    User user2 = userRepo.findById(user2Id).orElseThrow();
                    Conversation conversation = new Conversation();
                    conversation.setUser1(user1);
                    conversation.setUser2(user2);
                    return convoRepo.save(conversation);
                });
    }

    public Message sendMessage(Long conversationId, String senderId, String encryptedContent) {
        Conversation conversation = convoRepo.findById(conversationId).orElseThrow();
        User sender = userRepo.findById(senderId).orElseThrow();

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setEncryptedContent(encryptedContent);
        return messageRepo.save(message);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepo.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    public List<Conversation> getUserConversations(String userId) {
        return convoRepo.findAllByUserId(userId);
    }

    public List<Message> getUnreadMessages(Long conversationId, String userId) {
        return messageRepo.findUnreadMessagesForUser(conversationId, userId);
    }

    public void markMessagesAsRead(Long conversationId, String userId) {
        List<Message> unread = getUnreadMessages(conversationId, userId);
        unread.forEach(m -> m.setRead(true));
        messageRepo.saveAll(unread);
    }
}

