package com.cqchat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cqchat.entity.ChatMsg;
import com.cqchat.mapper.ChatMsgMapper;
import com.cqchat.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatMsgMapper chatMsgMapper;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String RECENT_MSG_KEY_PREFIX = "chat:recent:";

    /**
     * 获取聊天历史记录（分页）
     */
    @GetMapping("/messages")
    public Map<String, Object> getMessages(
            @RequestParam Long conversationId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        Page<ChatMsg> pageParam = new Page<>(page, size);
        Page<ChatMsg> result = chatMsgMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ChatMsg>()
                        .eq(ChatMsg::getConversationId, conversationId)
                        .orderByDesc(ChatMsg::getCreatedAt));

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        return response;
    }

    /**
     * 获取两个用户之间的聊天记录
     */
    @GetMapping("/messages/between")
    public Map<String, Object> getMessagesBetween(
            @RequestParam Long userId,
            @RequestParam Long otherUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        // 先查找会话
        Long smaller = Math.min(userId, otherUserId);
        Long bigger = Math.max(userId, otherUserId);
        var conversation = conversationService.getOrCreate(smaller, bigger);

        return getMessages(conversation.getId(), page, size);
    }

    /**
     * 获取最近消息（从 Redis 缓存）
     */
    @GetMapping("/messages/recent")
    public Map<String, Object> getRecentMessages(
            @RequestParam Long conversationId,
            @RequestParam(defaultValue = "20") Integer count) {

        String key = RECENT_MSG_KEY_PREFIX + conversationId;
        Set<String> messages = redisTemplate.opsForZSet().reverseRange(key, 0, count - 1);

        List<Map<String, Object>> result = new ArrayList<>();
        if (messages != null) {
            for (String msg : messages) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = com.alibaba.fastjson.JSON.parseObject(msg, Map.class);
                result.add(parsed);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", result);
        return response;
    }
}
