package edu.cqie.cqdemo.task;

import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.RoutesService;
import edu.cqie.cqdemo.service.ScenicsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WorkSyncTask implements ApplicationRunner {

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
     * 项目启动时同步MySQL的所有数据到Redis
     * 确保Redis中包含所有MySQL数据
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("项目启动时开始同步MySQL数据到Redis");

            // 同步Routes数据
            syncRoutesToRedis();

            // 同步Guides数据
            syncGuidesToRedis();

            // 同步Scenics数据
            syncScenicsToRedis();

            log.info("项目启动时MySQL数据同步到Redis完成");
        } catch (Exception e) {
            log.error("项目启动时同步MySQL数据到Redis失败：", e);
        }
    }

    /**
     * 同步Routes数据到Redis
     */
    private void syncRoutesToRedis() {
        try {
            List<Routes> routesList = routesService.list();
            for (Routes route : routesList) {
                String redisKey = "user_works:" + route.getUserId() + ":" + route.getStatus();
                // 从Redis获取当前数据
                List<Object> works = (List<Object>) redisTemplate.opsForValue().get(redisKey);
                if (works == null) {
                    works = new ArrayList<>();
                }
                // 检查是否已存在
                boolean exists = false;
                for (Object work : works) {
                    if (work instanceof Routes && ((Routes) work).getId().equals(route.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    works.add(route);
                    redisTemplate.opsForValue().set(redisKey, works);
                }
            }
            log.info("同步Routes数据到Redis完成，共同步 {} 条记录", routesList.size());
        } catch (Exception e) {
            log.error("同步Routes数据到Redis失败：", e);
        }
    }

    /**
     * 同步Guides数据到Redis
     */
    private void syncGuidesToRedis() {
        try {
            List<Guides> guidesList = guidesService.list();
            for (Guides guide : guidesList) {
                String redisKey = "user_works:" + guide.getUserId() + ":" + guide.getStatus();
                // 从Redis获取当前数据
                List<Object> works = (List<Object>) redisTemplate.opsForValue().get(redisKey);
                if (works == null) {
                    works = new ArrayList<>();
                }
                // 检查是否已存在
                boolean exists = false;
                for (Object work : works) {
                    if (work instanceof Guides && ((Guides) work).getId().equals(guide.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    works.add(guide);
                    redisTemplate.opsForValue().set(redisKey, works);
                }
            }
            log.info("同步Guides数据到Redis完成，共同步 {} 条记录", guidesList.size());
        } catch (Exception e) {
            log.error("同步Guides数据到Redis失败：", e);
        }
    }

    /**
     * 同步Scenics数据到Redis
     */
    private void syncScenicsToRedis() {
        try {
            List<Scenics> scenicsList = scenicsService.list();
            for (Scenics scenic : scenicsList) {
                // Scenics没有userId字段，使用默认值0
                String redisKey = "user_works:0:1";
                // 从Redis获取当前数据
                List<Object> works = (List<Object>) redisTemplate.opsForValue().get(redisKey);
                if (works == null) {
                    works = new ArrayList<>();
                }
                // 检查是否已存在
                boolean exists = false;
                for (Object work : works) {
                    if (work instanceof Scenics && ((Scenics) work).getId().equals(scenic.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    works.add(scenic);
                    redisTemplate.opsForValue().set(redisKey, works);
                }
            }
            log.info("同步Scenics数据到Redis完成，共同步 {} 条记录", scenicsList.size());
        } catch (Exception e) {
            log.error("同步Scenics数据到Redis失败：", e);
        }
    }

    /**
     * 同步 Redis 中的作品数据到 MySQL
     * 每 1 分 35 秒执行一次
     * 【修复】增加删除标记检查，防止已删除数据被重新同步
     */
    @Scheduled(fixedRate = 95 * 1000) // 95 秒 = 1 分 35 秒
    public void syncWorkData() {
        try {
            log.info("开始同步 Redis 作品数据到 MySQL");

            // 扫描所有作品相关的 Redis Key
            Set<String> workKeys = redisTemplate.keys("user_works:*");
            if (workKeys == null || workKeys.isEmpty()) {
                log.info("没有发现作品相关的 Redis Key");
                return;
            }

            int syncCount = 0;

            for (String key : workKeys) {
                // 解析 Key，获取用户 ID、状态和类型
                // Key 格式：user_works:{userId}:{status}
                String[] parts = key.split(":");
                if (parts.length < 3) {
                    log.warn("无效的作品 Key 格式：{}", key);
                    continue;
                }

                try {
                    // parts[0]="user_works", parts[1]=userId, parts[2]=status
                    Long userId = Long.parseLong(parts[1]);
                    Integer status = Integer.parseInt(parts[2]);
                    String type = null;
                    
                    // 检查是否有 type 信息（扩展用途）
                    if (parts.length > 3) {
                        type = parts[3];
                    }

                    // 为每个用户创建同步锁，确保只有一个进程处理该用户的作品数据
                    String lockKey = "sync:lock:works:" + userId + ":" + status;
                    if (type != null) {
                        lockKey += ":" + type;
                    }
                    Object lock = syncLocks.computeIfAbsent(lockKey, k -> new Object());

                    synchronized (lock) {
                        // 从 Redis 获取数据 - 增加异常处理
                        List<Object> works = null;
                        try {
                            works = (List<Object>) redisTemplate.opsForValue().get(key);
                        } catch (Exception e) {
                            log.error("从 Redis 读取数据失败，JSON 解析错误：key={}, error={}", key, e.getMessage());
                            // 数据损坏，删除该 key
                            try {
                                redisTemplate.delete(key);
                                log.info("已删除损坏的 Redis 数据：{}", key);
                            } catch (Exception deleteEx) {
                                log.warn("删除损坏的 Redis 数据失败：{}", key);
                            }
                            continue;
                        }
                        
                        if (works == null || works.isEmpty()) {
                            continue;
                        }

                        // 同步到 MySQL
                        for (Object work : works) {
                            try {
                                if (work instanceof Routes) {
                                    Routes route = (Routes) work;
                                    
                                    // 【关键】检查是否有删除标记
                                    String deleteMarkKey = "deleted:route:" + route.getId();
                                    if (redisTemplate.hasKey(deleteMarkKey)) {
                                        log.info("路线 ID={} 有删除标记，跳过同步", route.getId());
                                        continue;
                                    }
                                    
                                    // 检查 MySQL 中是否存在
                                    if (routesService.getById(route.getId()) == null) {
                                        // Redis 有 MySQL 没有，增加到 MySQL
                                        routesService.save(route);
                                        log.info("同步路线到 MySQL: routeId={}", route.getId());
                                    }
                                } else if (work instanceof Guides) {
                                    Guides guide = (Guides) work;
                                    
                                    // 【关键】检查是否有删除标记
                                    String deleteMarkKey = "deleted:guide:" + guide.getId();
                                    if (redisTemplate.hasKey(deleteMarkKey)) {
                                        log.info("攻略 ID={} 有删除标记，跳过同步", guide.getId());
                                        continue;
                                    }
                                    
                                    // 检查 MySQL 中是否存在
                                    if (guidesService.getById(guide.getId()) == null) {
                                        // Redis 有 MySQL 没有，增加到 MySQL
                                        guidesService.save(guide);
                                        log.info("同步攻略到 MySQL: guideId={}", guide.getId());
                                    }
                                } else if (work instanceof Scenics) {
                                    Scenics scenic = (Scenics) work;
                                    if (scenicsService.getById(scenic.getId()) == null) {
                                        // Redis 有 MySQL 没有，增加到 MySQL
                                        scenicsService.save(scenic);
                                    }
                                }
                            } catch (Exception e) {
                                log.error("处理单个作品时出错：work={}, error={}", work, e.getMessage());
                                // 继续处理下一个作品
                            }
                        }

                        syncCount++;
                    }
                } catch (NumberFormatException e) {
                    log.warn("无效的用户 ID 或状态格式：{}", key);
                    continue;
                }
            }

            log.info("Redis 作品数据同步完成，同步了 {} 个用户的数据", syncCount);
        } catch (Exception e) {
            log.error("同步 Redis 作品数据到 MySQL 失败：", e);
        }
    }

    /**
     * 清理 Redis 中不存在但 MySQL 中存在的数据
     * 每 10 分钟执行一次
     * 【修复】不再删除 MySQL 数据，仅记录差异日志
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10 分钟
    public void cleanUpWorkData() {
        try {
            log.info("开始检查 Redis 与 MySQL 作品数据一致性");

            // 先检查 Redis 中是否有作品数据
            Set<String> workKeys = redisTemplate.keys("user_works:*");
            if (workKeys == null || workKeys.isEmpty()) {
                log.info("Redis 中没有作品数据，跳过检查");
                return;
            }

            // 1. 从 MySQL 获取所有用户的作品
            List<Routes> routesList = routesService.list();
            List<Guides> guidesList = guidesService.list();

            int missingInRedisCount = 0;

            // 检查 Routes - 仅记录日志，不删除
            for (Routes route : routesList) {
                String redisKey = "user_works:" + route.getUserId() + ":" + route.getStatus();
                if (!redisTemplate.hasKey(redisKey)) {
                    missingInRedisCount++;
                    log.debug("Redis 中缺少路线缓存：routeId={}, userId={}, status={}", 
                        route.getId(), route.getUserId(), route.getStatus());
                }
            }

            // 检查 Guides - 仅记录日志，不删除
            for (Guides guide : guidesList) {
                String redisKey = "user_works:" + guide.getUserId() + ":" + guide.getStatus();
                if (!redisTemplate.hasKey(redisKey)) {
                    missingInRedisCount++;
                    log.debug("Redis 中缺少攻略缓存：guideId={}, userId={}, status={}", 
                        guide.getId(), guide.getUserId(), guide.getStatus());
                }
            }

            if (missingInRedisCount > 0) {
                log.info("检查完成：MySQL 中有 {} 条作品在 Redis 中缺少缓存（不删除，仅记录）", missingInRedisCount);
            } else {
                log.info("检查完成：Redis 与 MySQL 数据一致");
            }
        } catch (Exception e) {
            log.error("检查作品数据一致性失败：", e);
        }
    }
}
