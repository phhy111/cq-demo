package edu.cqie.cqdemo.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.ScenicsService;
import edu.cqie.cqdemo.redis.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 定时全量同步任务：MySQL → Redis
 * 作为延迟双删的兜底保障，确保 Redis 缓存与 MySQL 数据最终一致
 *
 * 同步内容：
 * 1. 点赞计数：每 5 分钟同步一次
 * 2. 收藏计数：每 5 分钟同步一次
 * 3. 浏览量：每 10 分钟同步一次
 */
@Slf4j
@Component
public class DataSyncTask {

    @Autowired
    private LikesService likesService;

    @Autowired
    private CollectionsService collectionsService;

    @Autowired
    private ScenicsService scenicsService;

    @Autowired
    private GuidesService guidesService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisUtil redisUtil;

    // 点赞/收藏缓存基础TTL：15分钟（同步间隔5分钟的3倍）
    private static final long COUNT_BASE_TTL_MINUTES = 15;
    // 浏览量缓存基础TTL：30分钟（同步间隔10分钟的3倍）
    private static final long VIEW_BASE_TTL_MINUTES = 30;

    private static final String LIKE_COUNT_KEY_PREFIX = "like_count:";
    private static final String COLLECT_COUNT_KEY_PREFIX = "collect_count:";
    private static final String VIEW_COUNT_KEY_PREFIX = "view_count:";

    // ==================== 点赞计数同步（每5分钟）====================

    /**
     * 从 MySQL 同步点赞计数到 Redis
     * 按 targetType + targetId 分组统计，与 Redis 中的计数对比，有差异才更新
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 30 * 1000)
    public void syncLikeCountToRedis() {
        try {
            log.info("开始同步点赞计数：MySQL → Redis");

            // 查询所有点赞记录，按 targetId + targetType 分组计数
            QueryWrapper<Likes> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("target_id", "target_type", "count(*) as cnt");
            queryWrapper.groupBy("target_id", "target_type");
            List<Map<String, Object>> likeCounts = likesService.listMaps(queryWrapper);

            int updatedCount = 0;
            for (Map<String, Object> row : likeCounts) {
                Long targetId = ((Number) row.get("target_id")).longValue();
                Integer targetType = ((Number) row.get("target_type")).intValue();
                Long count = ((Number) row.get("cnt")).longValue();

                String redisKey = LIKE_COUNT_KEY_PREFIX + targetType + ":" + targetId;
                Object cached = redisTemplate.opsForValue().get(redisKey);
                long cachedCount = cached != null ? Long.parseLong(cached.toString()) : 0;

                if (cachedCount != count) {
                    redisUtil.setObjWithJitter(redisKey, String.valueOf(count), COUNT_BASE_TTL_MINUTES, TimeUnit.MINUTES);
                    updatedCount++;
                }
            }

            log.info("点赞计数同步完成，共同步 {} 条记录，更新 {} 条", likeCounts.size(), updatedCount);
        } catch (Exception e) {
            log.error("点赞计数同步失败", e);
        }
    }

    // ==================== 收藏计数同步（每5分钟）====================

    /**
     * 从 MySQL 同步收藏计数到 Redis
     * 按 targetType + targetId 分组统计，与 Redis 中的计数对比，有差异才更新
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncCollectCountToRedis() {
        try {
            log.info("开始同步收藏计数：MySQL → Redis");

            QueryWrapper<Collections> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("target_id", "target_type", "count(*) as cnt");
            queryWrapper.groupBy("target_id", "target_type");
            List<Map<String, Object>> collectCounts = collectionsService.listMaps(queryWrapper);

            int updatedCount = 0;
            for (Map<String, Object> row : collectCounts) {
                Long targetId = ((Number) row.get("target_id")).longValue();
                Integer targetType = ((Number) row.get("target_type")).intValue();
                Long count = ((Number) row.get("cnt")).longValue();

                String redisKey = COLLECT_COUNT_KEY_PREFIX + targetType + ":" + targetId;
                Object cached = redisTemplate.opsForValue().get(redisKey);
                long cachedCount = cached != null ? Long.parseLong(cached.toString()) : 0;

                if (cachedCount != count) {
                    redisUtil.setObjWithJitter(redisKey, String.valueOf(count), COUNT_BASE_TTL_MINUTES, TimeUnit.MINUTES);
                    updatedCount++;
                }
            }

            log.info("收藏计数同步完成，共同步 {} 条记录，更新 {} 条", collectCounts.size(), updatedCount);
        } catch (Exception e) {
            log.error("收藏计数同步失败", e);
        }
    }

    // ==================== 浏览量同步（每10分钟）====================

    /**
     * 从 MySQL 同步景点/攻略的浏览量到 Redis
     * 只同步有变化的记录（Redis 缓存值与 MySQL 值不同）
     */
    @Scheduled(fixedRate = 10 * 60 * 1000, initialDelay = 90 * 1000)
    public void syncViewCountToRedis() {
        try {
            log.info("开始同步浏览量：MySQL → Redis");

            int updatedScenics = syncScenicsViewCount();
            int updatedGuides = syncGuidesViewCount();

            log.info("浏览量同步完成，景点更新 {} 条，攻略更新 {} 条", updatedScenics, updatedGuides);
        } catch (Exception e) {
            log.error("浏览量同步失败", e);
        }
    }

    private int syncScenicsViewCount() {
        List<Scenics> scenicsList = scenicsService.list();
        int updatedCount = 0;
        for (Scenics scenic : scenicsList) {
            if (scenic.getViewCount() == null) continue;

            String redisKey = VIEW_COUNT_KEY_PREFIX + "scenics:" + scenic.getId();
            Object cached = redisTemplate.opsForValue().get(redisKey);
            long cachedCount = cached != null ? Long.parseLong(cached.toString()) : 0;

            if (cachedCount != scenic.getViewCount()) {
                redisUtil.setObjWithJitter(redisKey, String.valueOf(scenic.getViewCount()), VIEW_BASE_TTL_MINUTES, TimeUnit.MINUTES);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    private int syncGuidesViewCount() {
        List<Guides> guidesList = guidesService.list();
        int updatedCount = 0;
        for (Guides guide : guidesList) {
            if (guide.getViewCount() == null) continue;

            String redisKey = VIEW_COUNT_KEY_PREFIX + "guides:" + guide.getId();
            Object cached = redisTemplate.opsForValue().get(redisKey);
            long cachedCount = cached != null ? Long.parseLong(cached.toString()) : 0;

            if (cachedCount != guide.getViewCount()) {
                redisUtil.setObjWithJitter(redisKey, String.valueOf(guide.getViewCount()), VIEW_BASE_TTL_MINUTES, TimeUnit.MINUTES);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    // ==================== 手动触发全量同步 ====================

    /**
     * 手动触发全量同步（供管理接口调用）
     */
    public void manualSyncAll() {
        log.info("手动触发全量同步");
        syncLikeCountToRedis();
        syncCollectCountToRedis();
        syncViewCountToRedis();
    }
}
