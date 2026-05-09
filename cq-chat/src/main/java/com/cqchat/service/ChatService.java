package com.cqchat.service;

import com.alibaba.fastjson.JSON;
import com.cqchat.entity.ChatMsg;
import com.cqchat.entity.Conversation;
import com.cqchat.mapper.ChatMsgMapper;
import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.protocol.ChatMessage;
import com.cqchat.protocol.MessageType;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ChatService {

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private OfflineMsgService offlineMsgService;

    @Autowired
    private UserChannelManager userChannelManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final String RECENT_MSG_KEY_PREFIX = "chat:recent:";
    private static final double JITTER_FACTOR = 0.1;
    private final Random random = new Random();

    private void expireWithJitter(String key, long baseTimeout, TimeUnit unit) {
        long jitter = (long) (baseTimeout * JITTER_FACTOR);
        long randomJitter = random.nextLong(jitter * 2 + 1) - jitter;
        long finalTimeout = Math.max(1, baseTimeout + randomJitter);
        redisTemplate.expire(key, finalTimeout, unit);
    }

    /**
     * 处理发送消息
     */
    public void handleSendMessage(ChatMessage msg) {
        // 1. 获取或创建会话
        Conversation conversation = conversationService.getOrCreate(msg.getSenderId(), msg.getReceiverId());

        // 2. 生成服务端 msgId 并持久化
        String msgId = UUID.randomUUID().toString().replace("-", "");
        ChatMsg chatMsg = buildChatMsg(msg, conversation.getId(), msgId);
        chatMsgMapper.insert(chatMsg);

        // 3. 更新会话最后消息
        conversationService.updateLastMsg(conversation.getId(), chatMsg.getId(), chatMsg.getCreatedAt());

        // 4. 缓存最近消息到 Redis
        cacheRecentMessage(conversation.getId(), chatMsg);

        // 5. 发送 SERVER_ACK 给发送方
        sendAck(msg.getSenderId(), msgId);

        // 6. 推送消息给接收方
        pushToReceiver(msg, chatMsg, conversation.getId());
    }

    /**
     * 处理已读回执
     */
    public void handleReadAck(ChatMessage msg, Long readerId) {
        Long senderId = msg.getSenderId();
        Long receiverId = msg.getReceiverId();

        // senderId 是读者，receiverId 是消息发送方
        Conversation conversation = conversationService.getOrCreate(readerId, receiverId);
        if (conversation == null) return;

        // 更新消息已读状态（使用 chatMsg.id 而不是 msgId）
        // 这里 content 存的是已读到的 chatMsg.id
        try {
            Long readToId = Long.parseLong(msg.getContent());
            chatMsgMapper.markAsRead(conversation.getId(), readToId);
        } catch (NumberFormatException e) {
            log.warn("已读回执 content 格式错误: {}", msg.getContent());
            return;
        }

        // 清零未读计数
        String unreadKey = UNREAD_KEY_PREFIX + readerId;
        redisTemplate.opsForHash().delete(unreadKey, conversation.getId().toString());

        // 通知对方已读
        Channel senderChannel = userChannelManager.getChannel(receiverId);
        if (senderChannel != null && senderChannel.isActive()) {
            ChatMessage ack = new ChatMessage();
            ack.setType(MessageType.READ_ACK.name());
            ack.setSenderId(readerId);
            ack.setReceiverId(receiverId);
            ack.setContent(msg.getContent());
            ack.setTimestamp(System.currentTimeMillis());
            senderChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
        }

        log.info("用户 {} 已读会话 {} 的消息到 {}", readerId, conversation.getId(), msg.getContent());
    }

    private ChatMsg buildChatMsg(ChatMessage msg, Long conversationId, String msgId) {
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setMsgId(msgId);
        chatMsg.setConversationId(conversationId);
        chatMsg.setSenderId(msg.getSenderId());
        chatMsg.setReceiverId(msg.getReceiverId());
        chatMsg.setMsgType(msg.getType());
        chatMsg.setContent(msg.getContent());
        chatMsg.setMediaUrl(msg.getMediaUrl());
        chatMsg.setVoiceDuration(msg.getVoiceDuration() != null ? msg.getVoiceDuration() : 0);
        chatMsg.setIsRead(0);
        chatMsg.setCreatedAt(LocalDateTime.now());
        return chatMsg;
    }

    private void cacheRecentMessage(Long conversationId, ChatMsg chatMsg) {
        try {
            String key = RECENT_MSG_KEY_PREFIX + conversationId;
            String json = JSON.toJSONString(chatMsg);
            redisTemplate.opsForZSet().add(key, json, System.currentTimeMillis());
            // 只保留最近50条
            redisTemplate.opsForZSet().removeRange(key, 0, -51);
            expireWithJitter(key, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("缓存最近消息失败: {}", e.getMessage());
        }
    }

    private void sendAck(Long userId, String msgId) {
        Channel channel = userChannelManager.getChannel(userId);
        if (channel != null && channel.isActive()) {
            ChatMessage ack = new ChatMessage();
            ack.setType(MessageType.SERVER_ACK.name());
            ack.setMsgId(msgId);
            ack.setTimestamp(System.currentTimeMillis());
            channel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(ack)));
        }
    }

    private void pushToReceiver(ChatMessage originalMsg, ChatMsg savedMsg, Long conversationId) {
        Long receiverId = originalMsg.getReceiverId();
        Channel receiverChannel = userChannelManager.getChannel(receiverId);

        // 构建推送给接收方的消息
        ChatMessage pushMsg = new ChatMessage();
        pushMsg.setType(originalMsg.getType());
        pushMsg.setMsgId(savedMsg.getMsgId());
        pushMsg.setSenderId(originalMsg.getSenderId());
        pushMsg.setReceiverId(receiverId);
        pushMsg.setContent(originalMsg.getContent());
        pushMsg.setMediaUrl(originalMsg.getMediaUrl());
        pushMsg.setVoiceDuration(originalMsg.getVoiceDuration());
        pushMsg.setTimestamp(savedMsg.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

        if (receiverChannel != null && receiverChannel.isActive()) {
            // 在线：直接推送
            receiverChannel.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(pushMsg)));
            log.info("消息已推送给在线用户 {}", receiverId);
        } else {
            // 离线：存离线消息 + 更新未读计数
            offlineMsgService.saveOfflineMsg(receiverId, savedMsg.getMsgId());
            incrementUnreadCount(receiverId, conversationId);
            log.info("用户 {} 离线，消息已暂存", receiverId);
        }
    }

    private void incrementUnreadCount(Long userId, Long conversationId) {
        String key = UNREAD_KEY_PREFIX + userId;
        redisTemplate.opsForHash().increment(key, conversationId.toString(), 1);
        expireWithJitter(key, 30, TimeUnit.DAYS);
    }
}
