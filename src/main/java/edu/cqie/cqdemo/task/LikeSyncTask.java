package edu.cqie.cqdemo.task;

import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.service.LikesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LikeSyncTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LikesService likesService;

    // 用于存储同步锁，防止并发情况下多个进程同时访问MySQL
    private final ConcurrentHashMap<String, Object> syncLocks = new ConcurrentHashMap<>();

    /**
     * 同步Redis中的点赞数据到MySQL
     * 每1分钟执行一次
     */
    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void syncLikeData() {
        try {
            log.info("开始同步Redis点赞数据到MySQL");

            // 扫描所有点赞相关的Redis Key
            Set<String> likeKeys = redisTemplate.keys("likes:*");
            if (likeKeys == null || likeKeys.isEmpty()) {
                log.info("没有发现点赞相关的Redis Key");
                return;
            }

            int syncCount = 0;
            for (String key : likeKeys) {
                // 解析Key，获取targetType和targetId
                // Key格式：likes:{targetType}:{targetId}
                String[] parts = key.split(":");
                if (parts.length != 3) {
                    log.warn("无效的点赞Key格式：{}", key);
                    continue;
                }

                Integer targetType = Integer.parseInt(parts[1]);
                Long targetId = Long.parseLong(parts[2]);

                // 获取Redis中该目标的所有点赞用户ID
                Set<Object> userIds = redisTemplate.opsForSet().members(key);
                if (userIds == null || userIds.isEmpty()) {
                    continue;
                }

                // 为每个目标创建同步锁，确保只有一个进程处理该目标的点赞数据
                String lockKey = "sync:lock:like:" + targetType + ":" + targetId;
                Object lock = syncLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    // 同步到MySQL
                    for (Object userIdObj : userIds) {
                        Long userId = Long.parseLong(userIdObj.toString());

                        // 检查MySQL中是否已存在该点赞记录
                        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                        queryWrapper.eq("user_id", userId);
                        queryWrapper.eq("target_id", targetId);
                        queryWrapper.eq("target_type", targetType);

                        Likes existingLike = likesService.getOne(queryWrapper);
                        if (existingLike == null) {
                            // 不存在，创建新记录
                            Likes newLike = new Likes();
                            newLike.setUserId(userId);
                            newLike.setTargetId(targetId);
                            newLike.setTargetType(targetType);
                            newLike.setCreatedAt(new Date());
                            likesService.save(newLike);
                            syncCount++;
                        }
                    }
                }

                // 重置Redis Key的过期时间
                redisTemplate.expire(key, 7, TimeUnit.DAYS);
            }

            log.info("Redis点赞数据同步完成，同步了 {} 条记录", syncCount);
        } catch (Exception e) {
            log.error("同步Redis点赞数据到MySQL失败：", e);
        }
    }

    /**
     * 清理MySQL中不存在于Redis的点赞记录
     * 每1分钟执行一次
     */
    @Scheduled(fixedRate = 1 * 60 * 1000)
    public void cleanInvalidLikeData() {
        try {
            log.info("开始清理MySQL中无效的点赞记录");

            // 为清理操作创建全局锁，确保只有一个进程执行清理
            Object cleanLock = syncLocks.computeIfAbsent("sync:lock:clean:like", k -> new Object());

            synchronized (cleanLock) {
                // 查询MySQL中所有点赞记录（分批处理）
                List<Likes> allLikes = likesService.list();
                int cleanCount = 0;

                for (Likes like : allLikes) {
                    // 检查Redis中是否存在对应的记录
                    String redisKey = "likes:" + like.getTargetType() + ":" + like.getTargetId();
                    Boolean existsInRedis = redisTemplate.opsForSet().isMember(redisKey, like.getUserId());

                    if (existsInRedis == null || !existsInRedis) {
                        // Redis中不存在，从MySQL中删除
                        likesService.removeById(like.getId());
                        cleanCount++;
                    }
                }

                log.info("MySQL无效点赞记录清理完成，删除了 {} 条记录", cleanCount);
            }
        } catch (Exception e) {
            log.error("清理MySQL无效点赞记录失败：", e);
        }
    }
}
