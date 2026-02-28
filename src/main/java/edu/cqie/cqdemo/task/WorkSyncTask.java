package edu.cqie.cqdemo.task;

import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.RoutesService;
import edu.cqie.cqdemo.service.ScenicsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WorkSyncTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RoutesService routesService;

    @Autowired
    private GuidesService guidesService;

    @Autowired
    private ScenicsService scenicsService;

    // 用于存储同步锁，防止并发情况下多个进程同时访问MySQL
    private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();

    /**
     * 同步Redis中的作品数据到MySQL
     * 每1分35秒执行一次
     */
    @Scheduled(fixedRate = 95 * 1000) // 95秒 = 1分35秒
    public void syncWorkData() {
        try {
            log.info("开始同步Redis作品数据到MySQL");

            // 扫描所有作品相关的Redis Key
            Set<String> workKeys = redisTemplate.keys("user_works:*");
            if (workKeys == null || workKeys.isEmpty()) {
                log.info("没有发现作品相关的Redis Key");
                return;
            }

            int syncCount = 0;

            for (String key : workKeys) {
                // 解析Key，获取用户ID、状态和类型
                // Key格式：user_works:{userId}:{status} 或 user_works:{userId}:{status}:{type}
                String[] parts = key.split(":");
                if (parts.length < 3) {
                    log.warn("无效的作品Key格式：{}", key);
                    continue;
                }

                try {
                    Long userId = Long.parseLong(parts[2]);
                    Integer status = Integer.parseInt(parts[3]);
                    String type = parts.length > 4 ? parts[4] : null;

                    // 为每个用户创建同步锁，确保只有一个进程处理该用户的作品数据
                    String lockKey = "sync:lock:works:" + userId + ":" + status;
                    if (type != null) {
                        lockKey += ":" + type;
                    }
                    Object lock = syncLocks.computeIfAbsent(lockKey, k -> new Object());

                    synchronized (lock) {
                        // 从Redis获取数据
                        List<Object> works = (List<Object>) redisTemplate.opsForValue().get(key);
                        if (works == null || works.isEmpty()) {
                            continue;
                        }

                        // 同步到MySQL
                        for (Object work : works) {
                            if (work instanceof Routes) {
                                Routes route = (Routes) work;
                                if (routesService.getById(route.getId()) == null) {
                                    // Redis有MySQL没有，增加到MySQL
                                    routesService.save(route);
                                }
                            } else if (work instanceof Guides) {
                                Guides guide = (Guides) work;
                                if (guidesService.getById(guide.getId()) == null) {
                                    // Redis有MySQL没有，增加到MySQL
                                    guidesService.save(guide);
                                }
                            } else if (work instanceof Scenics) {
                                Scenics scenic = (Scenics) work;
                                if (scenicsService.getById(scenic.getId()) == null) {
                                    // Redis有MySQL没有，增加到MySQL
                                    scenicsService.save(scenic);
                                }
                            }
                        }

                        syncCount++;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无效的用户ID或状态格式：{}", key);
                    continue;
                }
            }

            log.info("Redis作品数据同步完成，同步了 {} 个用户的数据", syncCount);
        } catch (Exception e) {
            log.error("同步Redis作品数据到MySQL失败：", e);
        }
    }

    /**
     * 清理Redis中不存在但MySQL中存在的数据
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10分钟
    public void cleanUpWorkData() {
        try {
            log.info("开始清理Redis中不存在但MySQL中存在的作品数据");

            // 这里需要实现清理逻辑
            // 1. 从MySQL获取所有用户的作品
            // 2. 检查Redis中是否存在
            // 3. 如果Redis中不存在，从MySQL删除

            log.info("作品数据清理完成");
        } catch (Exception e) {
            log.error("清理作品数据失败：", e);
        }
    }
}
