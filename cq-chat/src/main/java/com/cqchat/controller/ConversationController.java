package com.cqchat.controller;

import com.cqchat.entity.Conversation;
import com.cqchat.entity.OfflineMsg;
import com.cqchat.mapper.OfflineMsgMapper;
import com.cqchat.netty.session.UserChannelManager;
import com.cqchat.service.ConversationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserChannelManager userChannelManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OfflineMsgMapper offlineMsgMapper;

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";

    /**
     * 获取用户的会话列表（含未读数）
     */
    @GetMapping("/conversations")
    public Map<String, Object> getConversations(@RequestParam Long userId) {
        List<Conversation> conversations = conversationService.getUserConversations(userId);
        String unreadKey = UNREAD_KEY_PREFIX + userId;
        Map<Object, Object> unreadMap = redisTemplate.opsForHash().entries(unreadKey);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation conv : conversations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", conv.getId());
            item.put("otherUserId", conversationService.getOtherUserId(conv, userId));
            item.put("lastMsgId", conv.getLastMsgId());
            item.put("lastMsgTime", conv.getLastMsgTime());

            // 未读数
            Object unread = unreadMap.get(conv.getId().toString());
            item.put("unreadCount", unread != null ? Integer.parseInt(unread.toString()) : 0);

            // 对方是否在线
            Long otherUserId = conversationService.getOtherUserId(conv, userId);
            item.put("otherUserOnline", userChannelManager.isOnline(otherUserId));

            result.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result);
        return response;
    }

    /**
     * 获取指定会话的未读数
     */
    @GetMapping("/unread")
    public Map<String, Object> getUnreadCount(@RequestParam Long userId, @RequestParam Long conversationId) {
        String unreadKey = UNREAD_KEY_PREFIX + userId;
        Object unread = redisTemplate.opsForHash().get(unreadKey, conversationId.toString());
        int count = unread != null ? Integer.parseInt(unread.toString()) : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", count);
        return response;
    }

    /**
     * 获取总未读数
     */
    @GetMapping("/unread/total")
    public Map<String, Object> getTotalUnreadCount(@RequestParam Long userId) {
        String unreadKey = UNREAD_KEY_PREFIX + userId;
        Map<Object, Object> unreadMap = redisTemplate.opsForHash().entries(unreadKey);
        long total = unreadMap.values().stream()
                .mapToLong(v -> Long.parseLong(v.toString()))
                .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", total);
        return response;
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online")
    public Map<String, Object> isOnline(@RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", userChannelManager.isOnline(userId));
        return response;
    }
}
