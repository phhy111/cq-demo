package com.cqchat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqchat.entity.Conversation;
import com.cqchat.mapper.ConversationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    public Conversation getOrCreate(Long userA, Long userB) {
        // 保证顺序一致，避免重复会话
        Long smaller = Math.min(userA, userB);
        Long bigger = Math.max(userA, userB);

        Conversation conversation = conversationMapper.findByTwoUsers(smaller, bigger);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserAId(smaller);
            conversation.setUserBId(bigger);
            conversation.setCreatedAt(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    public void updateLastMsg(Long conversationId, Long msgId, LocalDateTime msgTime) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setLastMsgId(msgId);
        conversation.setLastMsgTime(msgTime);
        conversationMapper.updateById(conversation);
    }

    public List<Conversation> getUserConversations(Long userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserAId, userId)
                        .or()
                        .eq(Conversation::getUserBId, userId)
                        .orderByDesc(Conversation::getLastMsgTime)
        );
    }

    public Long getOtherUserId(Conversation conversation, Long currentUserId) {
        return conversation.getUserAId().equals(currentUserId)
                ? conversation.getUserBId()
                : conversation.getUserAId();
    }
}
