package edu.cqie.cqdemo.redis.config;

import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.LikesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis数据初始化器，在项目启动时从MySQL同步点赞和收藏数据到Redis
 */
@Configuration
public class RedisDataInitializer {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private LikesService likesService;

    @Autowired
    private CollectionsService collectionsService;

    @Bean
    public ApplicationRunner initRedisData() {
        return args -> {
            System.out.println("开始从MySQL同步数据到Redis...");
            syncDataFromMysqlToRedis();
            System.out.println("从MySQL同步数据到Redis完成");
        };
    }

    /**
     * 从MySQL同步数据到Redis
     */
    private void syncDataFromMysqlToRedis() {
        try {
            // 1. 同步点赞数据
            List<Likes> allLikes = likesService.list();
            for (Likes like : allLikes) {
                String redisKey = "likes:" + like.getTargetType() + ":" + like.getTargetId();
                redisTemplate.opsForSet().add(redisKey, like.getUserId());
                // 设置7天过期时间
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            }
            System.out.println("同步点赞数据到Redis成功，共" + allLikes.size() + "条记录");

            // 2. 同步收藏数据
            List<Collections> allCollections = collectionsService.list();
            for (Collections collection : allCollections) {
                String redisKey = "collections:" + collection.getTargetType() + ":" + collection.getTargetId();
                redisTemplate.opsForSet().add(redisKey, collection.getUserId());
                // 设置7天过期时间
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            }
            System.out.println("同步收藏数据到Redis成功，共" + allCollections.size() + "条记录");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("从MySQL同步数据到Redis失败：" + e.getMessage());
        }
    }
}
