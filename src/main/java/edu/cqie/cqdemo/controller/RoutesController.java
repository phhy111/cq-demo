package edu.cqie.cqdemo.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import edu.cqie.cqdemo.annotation.RedisLog;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.RoutesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/routes")
@EnableScheduling
@Slf4j
public class RoutesController {

    // 添加本地锁对象，用于防止缓存击穿时的并发问题
    private final Object syncLock = new Object();

    // 用于点赞状态查询的并发锁映射
    private final Map<String, Object> likeLocks = new ConcurrentHashMap<>();

    @Autowired
    private RoutesService routesService;
    @Autowired
    private LikesService likesService;
    @Autowired
    private CollectionsService collectionsService;
    @Autowired
    private GuidesService guidesService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private OSSOperationUtil ossOperationUtil;

    // 查询的前五
    @RequestMapping("/GetRoutesListInfo")
    public Result<List<Routes>> getRoutesListInfo() {
        List<Routes> routesList = routesService.getRoutesListInfo();
        // 从 Redis 更新点赞和收藏计数
        updateRoutesCountsFromRedis(routesList);
        if (routesList != null) {
            return Result.success(routesList);
        } else {
            return Result.error("查询失败");
        }
    }

    // 时间升序
    @GetMapping("/getRoutesMessageTimeS")
    public Result<List<Routes>> getRouesAllMessageS() {
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeS();
        updateRoutesCountsFromRedis(allRoutes);
        return Result.success(allRoutes);
    }

    // 时间降序
    @GetMapping("/getRoutesMessageTimeJ")
    public Result<List<Routes>> getRouesAllMessageJ() {
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeJ();
        updateRoutesCountsFromRedis(allRoutes);
        return Result.success(allRoutes);
    }

    @GetMapping("/getRoutesHeatS")
    public Result<List<Routes>> getRoutesHeatS() {
        List<Routes> allHeatS = routesService.getAllHeatS();
        updateRoutesCountsFromRedis(allHeatS);
        return Result.success(allHeatS);
    }

    @GetMapping("/getRoutesHeatJ")
    public Result<List<Routes>> getRoutesHeatJ() {
        List<Routes> allHeatJ = routesService.getAllHeatJ();
        updateRoutesCountsFromRedis(allHeatJ);
        return Result.success(allHeatJ);
    }

    /**
     * 从 Redis 更新路线的点赞和收藏计数
     * 当 Redis 读取失效时，从 MySQL 同步数据，防止缓存击穿
     */
    private void updateRoutesCountsFromRedis(List<Routes> routesList) {
        if (routesList != null && !routesList.isEmpty()) {
            try {
                for (Routes route : routesList) {
                    // 获取点赞数
                    String likeRedisKey = "likes:3:" + route.getId();
                    Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                    route.setLikeCount(likeCount != null ? likeCount.intValue() : 0);

                    // 获取收藏数
                    String collectRedisKey = "collections:3:" + route.getId();
                    Long collectCount = redisTemplate.opsForSet().size(collectRedisKey);
                    route.setCollectCount(collectCount != null ? collectCount.intValue() : 0);
                }
            } catch (Exception e) {
                log.warn("Redis 读取失败，尝试从 MySQL 同步数据: {}", e.getMessage());

                // 使用本地锁确保只有一个线程执行同步操作
                synchronized (syncLock) {
                    // 双重检查：再次尝试从 Redis 读取
                    boolean needSync = false;
                    try {
                        for (Routes route : routesList) {
                            String likeRedisKey = "likes:3:" + route.getId();
                            Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                            if (likeCount == null) {
                                needSync = true;
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        needSync = true;
                    }

                    // 如果仍然需要同步，执行同步操作
                    if (needSync) {
                        syncDataFromMysqlToRedis();
                    }
                }

                // 重新从 Redis 读取数据
                for (Routes route : routesList) {
                    try {
                        String likeRedisKey = "likes:3:" + route.getId();
                        Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                        route.setLikeCount(likeCount != null ? likeCount.intValue() : 0);

                        String collectRedisKey = "collections:3:" + route.getId();
                        Long collectCount = redisTemplate.opsForSet().size(collectRedisKey);
                        route.setCollectCount(collectCount != null ? collectCount.intValue() : 0);
                    } catch (Exception ex) {
                        log.error("Redis 二次读取仍然失败，保留数据库中的值: {}", ex.getMessage());
                        // 此时保留数据库中原本的值，不做修改
                    }
                }
            }
        }
    }

    /**
     * 从 MySQL 同步数据到 Redis (仅在 Redis 异常或初始化时使用)
     */
    private void syncDataFromMysqlToRedis() {
        try {
            log.info("开始从 MySQL 同步点赞和收藏数据到 Redis...");

            // 1. 同步点赞数据
            List<Likes> allLikes = likesService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes>().eq("target_type", 3));
            for (Likes like : allLikes) {
                String redisKey = "likes:3:" + like.getTargetId();
                redisTemplate.opsForSet().add(redisKey, like.getUserId());
                // 设置较长的过期时间，避免频繁同步
                redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
            }

            // 2. 同步收藏数据
            List<Collections> allCollections = collectionsService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>().eq("target_type", 3));
            for (Collections collection : allCollections) {
                String redisKey = "collections:3:" + collection.getTargetId();
                redisTemplate.opsForSet().add(redisKey, collection.getUserId());
                redisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
            }

            log.info("从 MySQL 同步数据到 Redis 成功，共同步点赞:{}条，收藏:{}条", allLikes.size(), allCollections.size());
        } catch (Exception e) {
            log.error("从 MySQL 同步数据到 Redis 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 【修复点】获取路线详情，确保点赞/收藏数与列表页一致（从 Redis 读取）
     */
    @GetMapping("/getRouteDetail")
    public Result getRouteDetail(Integer id) {
        try {
            Routes route = routesService.getRouteDetail(id);
            if (route == null) {
                return Result.error("路线不存在");
            }

            // 【关键修复】将单个对象放入列表，复用更新计数的逻辑，确保从 Redis 获取最新数值
            List<Routes> singleRouteList = new ArrayList<>();
            singleRouteList.add(route);
            updateRoutesCountsFromRedis(singleRouteList);

            return Result.success(route);
        } catch (Exception e) {
            log.error("详情查询失败: {}", e.getMessage(), e);
            return Result.error("详情查询失败:" + e.getMessage());
        }
    }

    /**
     * 获取路线相关的攻略列表
     */
    @GetMapping("/getRouteGuides")
    public Result getRouteGuides(Integer routeId) {
        try {
            String redisKey = "guides:route:" + routeId;
            List<Guides> guidesList = (List<Guides>) redisTemplate.opsForValue().get(redisKey);

            if (guidesList != null) {
                redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
                return Result.success(guidesList);
            } else {
                String lockKey = "lock:guides:route:" + routeId;
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    guidesList = (List<Guides>) redisTemplate.opsForValue().get(redisKey);
                    if (guidesList != null) {
                        redisTemplate.expire(redisKey, 1, TimeUnit.MINUTES);
                        return Result.success(guidesList);
                    }

                    guidesList = guidesService.getGuidesByRouteId(routeId);
                    if (guidesList != null) {
                        redisTemplate.opsForValue().set(redisKey, guidesList, 1, TimeUnit.MINUTES);
                    }
                    return Result.success(guidesList);
                }
            }
        } catch (Exception e) {
            log.error("查询路线攻略失败: {}", e.getMessage(), e);
            return Result.error("查询路线攻略失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "ROUTE")
    @PostMapping("/addLikeRoutes")
    public Result addLikeRoutes(@RequestBody Likes likes) {
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户点赞");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Long isAdded = redisTemplate.opsForSet().add(redisKey, likes.getUserId());
            redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);

            if (isAdded != null && isAdded == 1) {
                likes.setCreatedAt(new Date());
                likesService.save(likes);
                log.info("点赞成功：userId={}, targetId={}, targetType={}", likes.getUserId(), likes.getTargetId(), likes.getTargetType());
                return Result.success("点赞成功");
            } else {
                return Result.success("已点赞，无需重复操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("点赞失败: {}", e.getMessage(), e);
            return Result.error("点赞失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "ROUTE")
    @PostMapping("/removeLikeRoutes")
    public Result removeLikeRoutes(@RequestBody Likes likes) {
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户取消点赞");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, likes.getUserId());
            // 即使移除后集合为空，也刷新一下过期时间，或者让其自然过期
            if (redisTemplate.opsForSet().size(redisKey) > 0) {
                redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);
            }

            if (isRemoved != null && isRemoved == 1) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", likes.getUserId());
                queryWrapper.eq("target_id", likes.getTargetId());
                queryWrapper.eq("target_type", likes.getTargetType());

                boolean deleted = likesService.remove(queryWrapper);
                log.info("从 MySQL 删除点赞记录：{}", deleted ? "成功" : "失败");

                return Result.success("取消点赞成功");
            } else {
                return Result.success("未点赞，无需取消操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消点赞失败: {}", e.getMessage(), e);
            return Result.error("取消点赞失败：" + e.getMessage());
        }
    }

    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }

    @PostMapping("/checkLikeStatus")
    public Result checkLikeStatus(@RequestBody Likes likes) {
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权查询其他用户的点赞状态");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Boolean isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());

            if (isLiked != null) {
                return Result.success(isLiked);
            }

            // Redis 未命中，加锁查库
            String lockKey = "lock:like:" + likes.getTargetType() + ":" + likes.getTargetId() + ":" + likes.getUserId();
            Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

            synchronized (lock) {
                isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());
                if (isLiked != null) {
                    return Result.success(isLiked);
                }

                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", likes.getUserId());
                queryWrapper.eq("target_id", likes.getTargetId());
                queryWrapper.eq("target_type", likes.getTargetType());

                Likes existingLike = likesService.getOne(queryWrapper);
                boolean mysqlLiked = existingLike != null;

                if (mysqlLiked) {
                    redisTemplate.opsForSet().add(redisKey, likes.getUserId());
                    redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);
                }

                return Result.success(mysqlLiked);
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询点赞状态失败: {}", e.getMessage(), e);
            return Result.error("查询点赞状态失败：" + e.getMessage());
        }
    }

    @GetMapping("/getLikeCount")
    public Result getLikeCount(Integer targetType, Long targetId) {
        try {
            String redisKey = "likes:" + targetType + ":" + targetId;
            Long likeCount = redisTemplate.opsForSet().size(redisKey);

            if (likeCount != null) {
                redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);
                return Result.success(likeCount);
            }

            String lockKey = "lock:like:count:" + targetType + ":" + targetId;
            Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

            synchronized (lock) {
                likeCount = redisTemplate.opsForSet().size(redisKey);
                if (likeCount != null) {
                    redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);
                    return Result.success(likeCount);
                }

                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("target_id", targetId);
                queryWrapper.eq("target_type", targetType);

                List<Likes> likesList = likesService.list(queryWrapper);
                long mysqlLikeCount = likesList.size();

                for (Likes like : likesList) {
                    redisTemplate.opsForSet().add(redisKey, like.getUserId());
                }
                redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);

                return Result.success(mysqlLikeCount);
            }
        } catch (Exception e) {
            log.error("查询点赞数量失败: {}", e.getMessage(), e);
            return Result.error("查询点赞数量失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "ROUTE")
    @PostMapping("/addCollections")
    public Result addCollections(@RequestBody Collections collections) {
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户添加收藏");
            }

            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            Long isAdded = redisTemplate.opsForSet().add(redisKey, collections.getUserId());
            redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);

            if (isAdded != null && isAdded == 1) {
                collections.setCreatedAt(new Date());
                collectionsService.save(collections);
                return Result.success("收藏成功");
            } else {
                return Result.success("已收藏，无需重复操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("收藏失败: {}", e.getMessage(), e);
            return Result.error("收藏失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "ROUTE")
    @PostMapping("/removeCollections")
    public Result removeCollections(@RequestBody Collections collections) {
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户取消收藏");
            }

            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, collections.getUserId());

            if (redisTemplate.opsForSet().size(redisKey) > 0) {
                redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);
            }

            if (isRemoved != null && isRemoved == 1) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", collections.getUserId());
                queryWrapper.eq("target_id", collections.getTargetId());
                queryWrapper.eq("target_type", collections.getTargetType());

                boolean deleted = collectionsService.remove(queryWrapper);
                log.info("从 MySQL 删除收藏记录：{}", deleted ? "成功" : "失败");

                return Result.success("取消收藏成功");
            } else {
                return Result.success("未收藏，无需取消操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消收藏失败: {}", e.getMessage(), e);
            return Result.error("取消收藏失败：" + e.getMessage());
        }
    }

    /**
     * 定时任务：将 Redis 中的点赞数据同步到 MySQL
     * 【修复】移除了“反向删除”逻辑，防止 Redis 键过期导致 MySQL 数据丢失
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncLikesToMySQL() {
        try {
            Set<String> keys = redisTemplate.keys("likes:*");
            if (keys != null && !keys.isEmpty()) {
                int count = 0;
                for (String key : keys) {
                    String[] parts = key.split(":");
                    if (parts.length == 3) {
                        int targetType = Integer.parseInt(parts[1]);
                        Long targetId = Long.parseLong(parts[2]);

                        Set<Object> userIds = redisTemplate.opsForSet().members(key);
                        if (userIds != null && !userIds.isEmpty()) {
                            for (Object obj : userIds) {
                                Long userId = obj instanceof Integer ? Long.valueOf((Integer) obj) : Long.valueOf(obj.toString());

                                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                                queryWrapper.eq("user_id", userId);
                                queryWrapper.eq("target_id", targetId);
                                queryWrapper.eq("target_type", targetType);

                                Likes existingLike = likesService.getOne(queryWrapper);
                                if (existingLike == null) {
                                    Likes like = new Likes();
                                    like.setUserId(userId);
                                    like.setTargetId(targetId);
                                    like.setTargetType(targetType);
                                    like.setCreatedAt(new Date());
                                    likesService.save(like);
                                    count++;
                                }
                            }
                        }
                    }
                }
                if (count > 0) {
                    log.info("定时任务同步点赞数据：新增 {} 条记录", count);
                }
            }
            // 【已移除】不再执行“MySQL 有但 Redis 没有则删除”的逻辑，避免误删
        } catch (Exception e) {
            log.error("Redis 点赞数据同步到 MySQL 失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时任务：将 Redis 中的收藏数据同步到 MySQL
     * 【修复】移除了“反向删除”逻辑
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncCollectionsToMySQL() {
        try {
            Set<String> keys = redisTemplate.keys("collections:*");
            if (keys != null && !keys.isEmpty()) {
                int count = 0;
                for (String key : keys) {
                    String[] parts = key.split(":");
                    if (parts.length == 3) {
                        int targetType = Integer.parseInt(parts[1]);
                        Long targetId = Long.parseLong(parts[2]);

                        Set<Object> userIds = redisTemplate.opsForSet().members(key);
                        if (userIds != null && !userIds.isEmpty()) {
                            for (Object obj : userIds) {
                                Long userId = obj instanceof Integer ? Long.valueOf((Integer) obj) : Long.valueOf(obj.toString());

                                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                                queryWrapper.eq("user_id", userId);
                                queryWrapper.eq("target_id", targetId);
                                queryWrapper.eq("target_type", targetType);

                                Collections existingCollection = collectionsService.getOne(queryWrapper);
                                if (existingCollection == null) {
                                    Collections collection = new Collections();
                                    collection.setUserId(userId);
                                    collection.setTargetId(targetId);
                                    collection.setTargetType(targetType);
                                    collection.setCreatedAt(new Date());
                                    collectionsService.save(collection);
                                    count++;
                                }
                            }
                        }
                    }
                }
                if (count > 0) {
                    log.info("定时任务同步收藏数据：新增 {} 条记录", count);
                }
            }
            // 【已移除】不再执行“MySQL 有但 Redis 没有则删除”的逻辑
        } catch (Exception e) {
            log.error("Redis 收藏数据同步到 MySQL 失败: {}", e.getMessage(), e);
        }
    }

    @GetMapping("/GetRoutesInfo")
    public Result<PageInfo<Routes>> getRoutesInfo(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        PageHelper.startPage(pageNum, pageSize);
        List<Routes> routesList = routesService.getAllRoutesWithAllStatus();
        PageInfo<Routes> pageInfo = new PageInfo<>(routesList);

        if (pageInfo.getList() != null) {
            return Result.success(pageInfo);
        } else {
            return Result.error("查询失败");
        }
    }

    @PostMapping("/updateRouteStatus")
    public Result updateRouteStatus(@RequestParam Integer id) {
        try {
            Routes route = routesService.getById(id);
            if (route != null && route.getStatus() == 2) {
                Integer oldStatus = route.getStatus();
                route.setStatus(1);
                routesService.updateById(route);

                // 简化 Redis 更新逻辑，实际生产中建议使用更稳健的缓存更新策略或删除缓存
                String oldRedisKey = "user_works:" + route.getUserId() + ":" + oldStatus;
                String newRedisKey = "user_works:" + route.getUserId() + ":" + 1;

                // 注意：这里直接操作 List 可能存在并发问题，生产环境建议直接删除 Key 让下次查询重建，或使用 Lua 脚本
                // 此处保持原逻辑但增加空指针保护
                updateRedisUserWorks(route.getUserId(), oldStatus, newRedisKey, route, true);

                return Result.success("审核通过成功");
            } else {
                return Result.error("路线不存在或状态不是待审核");
            }
        } catch (Exception e) {
            log.error("审核通过失败: {}", e.getMessage(), e);
            return Result.error("审核通过失败：" + e.getMessage());
        }
    }

    @PostMapping("/noUpdateRouteStatus")
    public Result noUpdateRouteStatus(@RequestParam Integer id) {
        try {
            Routes route = routesService.getById(id);
            if (route != null && route.getStatus() == 2) {
                Integer oldStatus = route.getStatus();
                route.setStatus(3);
                routesService.updateById(route);

                updateRedisUserWorks(route.getUserId(), oldStatus, "user_works:" + route.getUserId() + ":" + 3, route, true);

                return Result.success("审核驳回成功");
            } else {
                return Result.error("路线不存在或状态不是待审核");
            }
        } catch (Exception e) {
            log.error("审核驳回失败: {}", e.getMessage(), e);
            return Result.error("审核驳回失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "ROUTE")
    @PostMapping("/saveRoute")
    public Result saveRoute(
            @RequestPart(required = false) MultipartFile file,
            @RequestPart Routes route) {
        try {
            if (route.getId() != null) {
                // 编辑路线
                Routes oldRoute = routesService.getById(route.getId());
                Integer oldStatus = oldRoute != null ? oldRoute.getStatus() : null;

                // 处理图片上传
                if (file != null && !file.isEmpty()) {
                    String imageUrl = ossOperationUtil.upload(file, "routes_img/");
                    route.setCoverImg(imageUrl);
                }

                route.setStatus(2);
                route.setUpdatedAt(new Date());
                routesService.updateById(route);

                if (oldStatus != null && !oldStatus.equals(route.getStatus())) {
                    updateRedisUserWorks(route.getUserId(), oldStatus, "user_works:" + route.getUserId() + ":" + route.getStatus(), route, true);
                }

                return Result.success("编辑成功");
            } else {
                // 新增路线
                route.setStatus(2);
                route.setCreatedAt(new Date());
                route.setUpdatedAt(new Date());

                // 处理图片上传
                if (file != null && !file.isEmpty()) {
                    String imageUrl = ossOperationUtil.upload(file, "routes_img/");
                    route.setCoverImg(imageUrl);
                }

                routesService.save(route);

                String redisKey = "user_works:" + route.getUserId() + ":" + route.getStatus();
                // 简单处理：直接删除该用户的缓存列表，强制下次重新查询，避免复杂的 List 操作并发问题
                redisTemplate.delete(redisKey);
                // 也可以删除所有状态的缓存
                redisTemplate.delete("user_works:" + route.getUserId() + ":1");
                redisTemplate.delete("user_works:" + route.getUserId() + ":3");

                return Result.success("新增成功");
            }
        } catch (Exception e) {
            log.error("保存失败: {}", e.getMessage(), e);
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    // 辅助方法：更新 Redis 中的用户作品列表
    private void updateRedisUserWorks(Long userId, Integer oldStatus, String newRedisKey, Routes route, boolean addToNew) {
        try {
            if (oldStatus != null) {
                String oldRedisKey = "user_works:" + userId + ":" + oldStatus;
                List<Object> oldWorks = (List<Object>) redisTemplate.opsForValue().get(oldRedisKey);
                if (oldWorks != null) {
                    oldWorks.removeIf(work -> work instanceof Routes && ((Routes) work).getId().equals(route.getId()));
                    redisTemplate.opsForValue().set(oldRedisKey, oldWorks);
                }
            }

            if (addToNew) {
                List<Object> newWorks = (List<Object>) redisTemplate.opsForValue().get(newRedisKey);
                if (newWorks == null) {
                    newWorks = new ArrayList<>();
                }
                boolean exists = false;
                for (Object work : newWorks) {
                    if (work instanceof Routes && ((Routes) work).getId().equals(route.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    newWorks.add(route);
                    redisTemplate.opsForValue().set(newRedisKey, newWorks);
                }
            }
        } catch (Exception e) {
            log.warn("更新 Redis 用户作品列表失败: {}", e.getMessage());
            // 缓存更新失败不影响主流程，记录日志即可
        }
    }

    @PostMapping("/updateLikeCountAndCollectCount")
    public Result updateLikeCountAndCollectCount() {
        try {
            routesService.updateLikeCountAndCollectCount();
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新统计失败：{}", e.getMessage(), e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 上传路线图片到阿里云 OSS
     * @param file 前端上传的文件
     * @return 阿里云返回的图片 URL
     */
    @PostMapping("/uploadRouteImage")
    public Result uploadRouteImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return Result.error("上传文件不能为空");
            }
            
            String url = ossOperationUtil.upload(file, "routes_img/");
            return Result.success(url);
        } catch (Exception e) {
            log.error("路线图片上传失败：{}", e.getMessage(), e);
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除路线
     * 【完整删除】同时删除 MySQL 和 Redis 中的所有相关数据
     */
    @RedisLog(type = "INFO", module = "ROUTE")
    @DeleteMapping("/deleteRoute")
    public Result deleteRoute(@RequestParam Integer id) {
        try {
            // 1. 校验 ID 非空
            if (id == null) {
                return Result.error("删除失败：路线 ID 不能为空");
            }
            // 2. 检查路线是否存在
            Routes routes = routesService.getById(id);
            if (routes == null) {
                return Result.error("删除失败：ID 为" + id + "的路线不存在");
            }
            // 3. 获取路线信息用于删除 Redis 数据
            Long userId = routes.getUserId();
            Integer status = routes.getStatus();
            
            // 4. 先删除 MySQL 数据（使用事务保证原子性）
            boolean isDeleted = routesService.deleteById(id);
            if (isDeleted) {
                // 5. 彻底清理 Redis 上的所有相关数据
                try {
                    // 5.1 删除点赞数据
                    String likeRedisKey = "likes:3:" + id;
                    redisTemplate.delete(likeRedisKey);
                    
                    // 5.2 删除收藏数据
                    String collectRedisKey = "collections:3:" + id;
                    redisTemplate.delete(collectRedisKey);
                    
                    // 5.3 删除攻略缓存
                    String guidesRedisKey = "guides:route:" + id;
                    redisTemplate.delete(guidesRedisKey);
                    
                    // 5.4 删除用户作品缓存（删除该用户所有状态的缓存，强制重新查询）
                    if (userId != null) {
                        redisTemplate.delete("user_works:" + userId + ":1");
                        redisTemplate.delete("user_works:" + userId + ":2");
                        redisTemplate.delete("user_works:" + userId + ":3");
                    }
                    
                    // 5.5 深度清理：从所有 Redis 作品列表中移除该路线（防止定时任务重新同步）
                    cleanRouteFromAllRedisWorks(id);
                    
                    // 5.6 添加删除标记到 Redis（10 分钟过期），防止同步任务误删
                    String deleteMarkKey = "deleted:route:" + id;
                    redisTemplate.opsForValue().set(deleteMarkKey, "deleted", 10, TimeUnit.MINUTES);
                    
                    log.info("删除路线成功，已同步清理 Redis 数据：路线 ID={}", id);
                } catch (Exception e) {
                    log.warn("删除 Redis 数据失败：{}", e.getMessage());
                    // Redis 删除失败不影响主流程，只记录警告日志
                }
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：执行删除操作无数据变更");
            }
        } catch (Exception e) {
            log.error("删除路线失败（ID={}）：{}", id, e.getMessage(), e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 从所有 Redis 作品列表中清理指定路线
     */
    private void cleanRouteFromAllRedisWorks(Integer routeId) {
        try {
            Set<String> keys = redisTemplate.keys("user_works:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    List<Object> works = (List<Object>) redisTemplate.opsForValue().get(key);
                    if (works != null && !works.isEmpty()) {
                        boolean removed = works.removeIf(work -> 
                            work instanceof Routes && ((Routes) work).getId().equals(routeId));
                        if (removed) {
                            redisTemplate.opsForValue().set(key, works);
                            log.info("从 Redis 作品列表 {} 中移除已删除路线 ID={}", key, routeId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理 Redis 作品列表中的路线失败：{}", e.getMessage());
        }
    }
}