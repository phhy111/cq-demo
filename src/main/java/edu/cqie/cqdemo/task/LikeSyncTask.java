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

@Component
@Slf4j
public class LikeSyncTask {

    @Autowired
    private LikesService likesService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 定时任务：将Redis中的点赞数据同步到MySQL
     * 每1分2秒执行一次
     */
    @Scheduled(fixedRate = 62 * 1000) // 1分2秒
    public void syncLikesToMySQL() {
        try {
            // 扫描Redis中所有以"likes:"开头的键
            Set<String> keys = redisTemplate.keys("likes:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    // 解析key，格式为"likes:targetType:targetId"
                    String[] parts = key.split(":");
                    if (parts.length == 3) {
                        int targetType = Integer.parseInt(parts[1]);
                        Long targetId = Long.parseLong(parts[2]);

                        // 获取该key对应的所有userId
                        Set<Object> userIds = redisTemplate.opsForSet().members(key);
                        if (userIds != null && !userIds.isEmpty()) {
                            for (Object obj : userIds) {
                                // 将Object转换为Long类型
                                Long userId = obj instanceof Integer ? Long.valueOf((Integer) obj) : Long.valueOf(obj.toString());
                                // 检查该点赞是否已经存在于MySQL中
                                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                                queryWrapper.eq("user_id", userId);
                                queryWrapper.eq("target_id", targetId);
                                queryWrapper.eq("target_type", targetType);

                                // 执行查询
                                Likes existingLike = likesService.getOne(queryWrapper);

                                // 如果不存在，则插入
                                if (existingLike == null) {
                                    Likes like = new Likes();
                                    like.setUserId(userId);
                                    like.setTargetId(targetId);
                                    like.setTargetType(targetType);
                                    like.setCreatedAt(new Date());

                                    // 插入到MySQL
                                    likesService.save(like);
                                    log.info("新增点赞记录：userId={}, targetId={}, targetType={}", userId, targetId, targetType);
                                } else {
                                    log.info("点赞记录已存在，跳过插入：userId={}, targetId={}, targetType={}", userId, targetId, targetType);
                                }
                            }
                        }
                    }
                }
            }

            // 处理取消点赞的情况：删除 MySQL 中存在但 Redis 中不存在的点赞记录
            // 只有在 Redis 中有数据时才执行此操作，防止 Redis 重启或数据过期时误删 MySQL 数据
            if (keys != null && !keys.isEmpty()) {
                // 获取 MySQL 中所有点赞记录
                List<Likes> allLikesInMySQL = likesService.list();
                for (Likes like : allLikesInMySQL) {
                    String redisKey = "likes:" + like.getTargetType() + ":" + like.getTargetId();
                    // 检查 Redis 中是否存在该点赞
                    Boolean existsInRedis = redisTemplate.opsForSet().isMember(redisKey, like.getUserId());
                    if (existsInRedis == null || !existsInRedis) {
                        // Redis 中不存在，从 MySQL 中删除
                        likesService.removeById(like.getId());
                        log.info("从 MySQL 删除点赞记录：userId={}, targetId={}, targetType={}", like.getUserId(), like.getTargetId(), like.getTargetType());
                    }
                }
            } else {
                log.info("Redis 中无点赞数据，跳过删除操作，保留 MySQL 中的数据");
            }

            log.info("Redis 点赞数据同步到 MySQL 成功");
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Redis点赞数据同步到MySQL失败：{}", e.getMessage());
        }
    }

    /**
     * 从MySQL同步数据到Redis（带同步锁机制）
     * 用于Redis数据失效后重新同步
     */
    public void syncDataFromMysqlToRedis() {
        // 使用双重检查锁模式确保只有一个线程执行同步操作
        synchronized (this) {
            try {
                // 从MySQL同步所有点赞数据到Redis
                List<Likes> allLikes = likesService.list();
                for (Likes like : allLikes) {
                    String redisKey = "likes:" + like.getTargetType() + ":" + like.getTargetId();
                    redisTemplate.opsForSet().add(redisKey, like.getUserId());
                    // 设置26天过期时间
                    redisTemplate.expire(redisKey, 26, java.util.concurrent.TimeUnit.DAYS);
                }
                log.info("从MySQL同步点赞数据到Redis成功");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("从MySQL同步点赞数据到Redis失败：{}", e.getMessage());
            }
        }
    }
}
