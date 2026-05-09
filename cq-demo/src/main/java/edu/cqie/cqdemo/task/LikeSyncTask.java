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
import java.util.ArrayList;

@Component
@Slf4j
public class LikeSyncTask {

    @Autowired
    private LikesService likesService;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private edu.cqie.cqdemo.redis.util.RedisUtil redisUtil;

    /**
     * 定时任务：将Redis中的点赞数据同步到MySQL
     * 每1分2秒执行一次
     */
    @Scheduled(fixedRate = 62 * 1000) // 1分2秒
    public void syncLikesToMySQL() {
        try {
            // 使用SCAN替代keys命令，避免阻塞Redis
            List<String> keys = new ArrayList<>();
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                var cursor = connection.scan(
                        org.springframework.data.redis.core.ScanOptions.scanOptions().match("likes:*").count(100).build());
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next()));
                }
                cursor.close();
                return null;
            });
            if (!keys.isEmpty()) {
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

            // 【修复】处理取消点赞的情况：仅当用户手动取消点赞时才删除，不根据 Redis 状态删除
            // 原因：Redis 数据可能因为过期、重启等原因丢失，不能作为删除 MySQL 数据的依据
            // 取消点赞操作应该由用户主动发起，通过 removeLikeRoutes/removeLikeGuides 接口处理
            log.info("Redis 点赞数据同步到 MySQL 完成（仅新增，不删除）");
            log.info("Redis 点赞数据同步到 MySQL 成功");
        } catch (Exception e) {
            log.error("Redis点赞数据同步到MySQL失败", e);
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
                    redisUtil.expireWithJitter(redisKey, 26, java.util.concurrent.TimeUnit.DAYS);
                }
                log.info("从MySQL同步点赞数据到Redis成功");
            } catch (Exception e) {
                log.error("从MySQL同步点赞数据到Redis失败", e);
            }
        }
    }
}
