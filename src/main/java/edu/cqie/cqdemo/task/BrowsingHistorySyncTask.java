package edu.cqie.cqdemo.task;

import edu.cqie.cqdemo.service.BrowsingHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class BrowsingHistorySyncTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private BrowsingHistoryService browsingHistoryService;

    // 用于存储同步锁，防止并发情况下多个进程同时访问MySQL
    private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();

    /**
     * 同步Redis中的浏览历史数据到MySQL
     * 每1.5分钟执行一次
     */
    @Scheduled(fixedRate = 90 * 1000) // 90秒 = 1.5分钟
    public void syncBrowsingHistoryData() {
        try {
            log.info("开始同步Redis浏览历史数据到MySQL");

            // 扫描所有浏览历史相关的Redis Key
            List<String> historyKeys = scanKeys("browsing_history:*");
            if (historyKeys == null || historyKeys.isEmpty()) {
                log.info("没有发现浏览历史相关的Redis Key");
                return;
            }

            int syncCount = 0;
            // 收集所有唯一的用户ID
            ConcurrentHashMap<Long, Boolean> userIds = new ConcurrentHashMap<>();

            for (String key : historyKeys) {
                // 解析Key，获取用户ID
                // Key格式：browsing_history:{userId} 或 browsing_history:{userId}:{type}
                String[] parts = key.split(":");
                if (parts.length < 2) {
                    log.warn("无效的浏览历史Key格式：{}", key);
                    continue;
                }

                try {
                    Long userId = Long.parseLong(parts[1]);
                    userIds.put(userId, Boolean.TRUE);
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID格式：{}", parts[1]);
                    continue;
                }
            }

            // 对每个用户同步数据
            for (Long userId : userIds.keySet()) {
                // 为每个用户创建同步锁，确保只有一个进程处理该用户的浏览历史数据
                String lockKey = "sync:lock:history:" + userId;
                Object lock = syncLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    try {
                        browsingHistoryService.syncFromRedisToMysql(userId);
                        syncCount++;
                    } catch (Exception e) {
                        log.error("同步用户 {} 的浏览历史数据失败：", userId, e);
                    }
                }
            }

            log.info("Redis浏览历史数据同步完成，同步了 {} 个用户的数据", syncCount);
        } catch (Exception e) {
            log.error("同步Redis浏览历史数据到MySQL失败：", e);
        }
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = new ArrayList<>();
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            var cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(100).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            cursor.close();
            return null;
        });
        return keys;
    }
}
