package com.cqchat.netty.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserChannelManager {

    public static final AttributeKey<Long> ATTR_USER_ID = AttributeKey.valueOf("userId");

    // userId -> Channel
    private final Map<Long, Channel> onlineUsers = new ConcurrentHashMap<>();
    // channelId -> userId
    private final Map<ChannelId, Long> channelUsers = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_USERS_KEY = "chat:online_users";

    public UserChannelManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void bindUser(Long userId, Channel channel) {
        onlineUsers.put(userId, channel);
        channelUsers.put(channel.id(), userId);
        channel.attr(ATTR_USER_ID).set(userId);
        // 同步到 Redis
        redisTemplate.opsForHash().put(ONLINE_USERS_KEY, userId.toString(), channel.id().asLongText());
        log.info("用户 {} 上线，channelId={}", userId, channel.id().asShortText());
    }

    public void unbindUser(Channel channel) {
        Long userId = channelUsers.remove(channel.id());
        if (userId != null) {
            onlineUsers.remove(userId);
            redisTemplate.opsForHash().delete(ONLINE_USERS_KEY, userId.toString());
            log.info("用户 {} 下线", userId);
        }
    }

    public Channel getChannel(Long userId) {
        return onlineUsers.get(userId);
    }

    public Long getUserId(Channel channel) {
        return channelUsers.get(channel.id());
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.containsKey(userId);
    }

    public int getOnlineCount() {
        return onlineUsers.size();
    }
}
