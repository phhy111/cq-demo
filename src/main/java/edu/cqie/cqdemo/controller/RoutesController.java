package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.RoutesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/routes")
@EnableScheduling
@Slf4j
public class RoutesController {
    /**
     * 查询路线信息
     * @return
     */

    // 添加本地锁对象，用于防止缓存击穿时的并发问题
    private final Object syncLock = new Object();

    @Autowired
    private RoutesService routesService;
    @Autowired
    private LikesService likesService;
    @Autowired
    private CollectionsService collectionsService;
    @Autowired
    private RedisTemplate redisTemplate;

    //查询的前五
    @RequestMapping("/GetRoutesListInfo")
    public Result<List<Routes>> getRoutesListInfo() {
        List<Routes> routesList = routesService.getRoutesListInfo();
        // 从Redis更新点赞和收藏计数
        updateRoutesCountsFromRedis(routesList);
        if (routesList != null){
            return Result.success(routesList);
        }else {
            return Result.error("查询失败");
        }
    }
    //时间升序
    @GetMapping("/getRoutesMessageTimeS")
    public Result<List<Routes>> getRouesAllMessageS(){
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeS();
        // 从Redis更新点赞和收藏计数
        updateRoutesCountsFromRedis(allRoutes);
        return Result.success(allRoutes);
    }
    //时间降序
    @GetMapping("/getRoutesMessageTimeJ")
    public Result<List<Routes>> getRouesAllMessageJ()
    {
        List<Routes> allRoutes = routesService.getRoutesListInfoTimeJ();
        // 从Redis更新点赞和收藏计数
        updateRoutesCountsFromRedis(allRoutes);
        return Result.success(allRoutes);
    }

    @GetMapping("/getRoutesHeatS")
    public Result<List<Routes>> getRoutesHeatS(){
        List<Routes> allHeatS =routesService.getAllHeatS();
        // 从Redis更新点赞和收藏计数
        updateRoutesCountsFromRedis(allHeatS);
        return Result.success(allHeatS);
    }

    @GetMapping("/getRoutesHeatJ")
    public Result<List<Routes>> getRoutesHeatJ(){
        List<Routes> allHeatJ =routesService.getAllHeatJ();
        // 从Redis更新点赞和收藏计数
        updateRoutesCountsFromRedis(allHeatJ);
        return Result.success(allHeatJ);
    }

    /**
     * 从Redis更新路线的点赞和收藏计数
     * 当Redis读取失效时，从MySQL同步数据，防止缓存击穿
     */
    private void updateRoutesCountsFromRedis(List<Routes> routesList) {
        if (routesList != null && !routesList.isEmpty()) {
            try {
                for (Routes route : routesList) {
                    // 获取点赞数：从Redis的likes:3:{routeId}集合中获取元素数量
                    String likeRedisKey = "likes:3:" + route.getId();
                    Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                    route.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
                    
                    // 获取收藏数：从Redis的collections:3:{routeId}集合中获取元素数量
                    String collectRedisKey = "collections:3:" + route.getId();
                    Long collectCount = redisTemplate.opsForSet().size(collectRedisKey);
                    route.setCollectCount(collectCount != null ? collectCount.intValue() : 0);
                }
            } catch (Exception e) {
                // Redis读取失败，从MySQL同步数据
                e.printStackTrace();
                System.out.println("Redis读取失败，从MySQL同步数据：" + e.getMessage());
                
                // 使用本地锁确保只有一个线程执行同步操作
                synchronized (syncLock) {
                    // 双重检查：再次尝试从Redis读取，避免重复同步
                    boolean needSync = false;
                    try {
                        for (Routes route : routesList) {
                            try {
                                String likeRedisKey = "likes:3:" + route.getId();
                                Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                                if (likeCount == null) {
                                    needSync = true;
                                    break;
                                }
                            } catch (Exception ex) {
                                needSync = true;
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        needSync = true;
                        ex.printStackTrace();
                    }
                    
                    // 如果仍然需要同步，执行同步操作
                    if (needSync) {
                        // 从MySQL同步数据到Redis
                        syncDataFromMysqlToRedis();
                    }
                }
                
                // 重新从Redis读取数据
                for (Routes route : routesList) {
                    try {
                        String likeRedisKey = "likes:3:" + route.getId();
                        Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
                        route.setLikeCount(likeCount != null ? likeCount.intValue() : 0);
                        
                        String collectRedisKey = "collections:3:" + route.getId();
                        Long collectCount = redisTemplate.opsForSet().size(collectRedisKey);
                        route.setCollectCount(collectCount != null ? collectCount.intValue() : 0);
                    } catch (Exception ex) {
                        // 如果仍然失败，使用数据库中的值
                        ex.printStackTrace();
                        System.out.println("Redis读取仍然失败，使用数据库中的值：" + ex.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 从MySQL同步数据到Redis
     */
    private void syncDataFromMysqlToRedis() {
        try {
            // 1. 同步点赞数据
            List<Likes> allLikes = likesService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes>().eq("target_type", 3));
            for (Likes like : allLikes) {
                String redisKey = "likes:3:" + like.getTargetId();
                redisTemplate.opsForSet().add(redisKey, like.getUserId());
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            }
            
            // 2. 同步收藏数据
            List<Collections> allCollections = collectionsService.list(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>().eq("target_type", 3));
            for (Collections collection : allCollections) {
                String redisKey = "collections:3:" + collection.getTargetId();
                redisTemplate.opsForSet().add(redisKey, collection.getUserId());
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            }
            
            System.out.println("从MySQL同步数据到Redis成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("从MySQL同步数据到Redis失败：" + e.getMessage());
        }
    }
    @GetMapping("/getRouteDetail")
    public Result getRouteDetail(Integer id) {
        try {
            Routes route = routesService.getRouteDetail(id);
            return  Result.success(route);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("详情查询失败:" + e.getMessage());
        }
    }


    @PostMapping("/addLikeRoutes")
    public Result addLikeRoutes(@RequestBody Likes likes){
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 2. 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
            Long isAdded = redisTemplate.opsForSet().add(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 3. 根据添加结果返回不同的响应
            if (isAdded != null && isAdded == 1) {
                return Result.success("点赞成功");
            } else {
                return Result.success("已点赞，无需重复操作");
            }
        } catch (Exception e) {
            // 4. 异常处理，返回错误信息
            e.printStackTrace();
            return Result.error("点赞失败：" + e.getMessage());
        }
    }

    @PostMapping("/removeLikeRoutes")
    public Result removeLikeRoutes(@RequestBody Likes likes){
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 2. 从Redis Set中移除userId
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            System.out.println(isRemoved);

            // 3. 根据移除结果返回不同的响应
            if (isRemoved != null && isRemoved == 1) {
                // 同时从MySQL中删除对应的记录
                // 使用MyBatis-Plus的条件查询和删除
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", likes.getUserId());
                queryWrapper.eq("target_id", likes.getTargetId());
                queryWrapper.eq("target_type", likes.getTargetType());
                
                // 执行删除操作
                boolean deleted = likesService.remove(queryWrapper);
                log.info("从MySQL删除点赞记录：" + (deleted ? "成功" : "失败"));
                
                return Result.success("取消点赞成功");
            } else {
                return Result.success("未点赞，无需取消操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("取消点赞失败：" + e.getMessage());
        }
    }

    @PostMapping("/addCollections")
    public Result addLikeRoutes(@RequestBody Collections collections){
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            // 2. 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
            Long isAdded = redisTemplate.opsForSet().add(redisKey, collections.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            
            // 3. 根据添加结果返回不同的响应
            if (isAdded != null && isAdded == 1) {
                return Result.success("收藏成功");
            } else {
                return Result.success("已收藏，无需重复操作");
            }
        } catch (Exception e) {
            // 4. 异常处理，返回错误信息
            e.printStackTrace();
            return Result.error("收藏失败：" + e.getMessage());
        }
    }

    @PostMapping("/removeCollections")
    public Result removeLikeRoutes(@RequestBody Collections collections){
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            // 2. 从Redis Set中移除userId
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, collections.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
            System.out.println(isRemoved);

            // 3. 根据移除结果返回不同的响应
            if (isRemoved != null && isRemoved == 1) {
                // 同时从MySQL中删除对应的记录
                // 使用MyBatis-Plus的条件查询和删除
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", collections.getUserId());
                queryWrapper.eq("target_id", collections.getTargetId());
                queryWrapper.eq("target_type", collections.getTargetType());

                // 执行删除操作
                boolean deleted = collectionsService.remove(queryWrapper);
                System.out.println("从MySQL删除收藏记录：" + (deleted ? "成功" : "失败"));

                return Result.success("取消收藏成功");
            } else {
                return Result.success("未收藏，无需取消操作");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("取消收藏失败：" + e.getMessage());
        }
    }



    /**
     * 定时任务：将Redis中的点赞数据同步到MySQL
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
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
                                // 使用MyBatis-Plus的条件查询
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
                                    System.out.println("新增点赞记录：userId=" + userId + ", targetId=" + targetId + ", targetType=" + targetType);
                                } else {
                                    System.out.println("点赞记录已存在，跳过插入：userId=" + userId + ", targetId=" + targetId + ", targetType=" + targetType);
                                }
                            }
                        }
                    }
                }
            }
            
            // 处理取消点赞的情况：删除MySQL中存在但Redis中不存在的点赞记录
            // 获取MySQL中所有点赞记录
            List<Likes> allLikesInMySQL = likesService.list();
            for (Likes like : allLikesInMySQL) {
                String redisKey = "likes:" + like.getTargetType() + ":" + like.getTargetId();
                // 检查Redis中是否存在该点赞
                Boolean existsInRedis = redisTemplate.opsForSet().isMember(redisKey, like.getUserId());
                if (existsInRedis == null || !existsInRedis) {
                    // Redis中不存在，从MySQL中删除
                    likesService.removeById(like.getId());
                    System.out.println("从MySQL删除点赞记录：userId=" + like.getUserId() + ", targetId=" + like.getTargetId() + ", targetType=" + like.getTargetType());
                }
            }
            
            System.out.println("Redis点赞数据同步到MySQL成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Redis点赞数据同步到MySQL失败：" + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void syncCollectionsToMySQL() {
        try {
            // 扫描Redis中所有以"collections:"开头的键
            Set<String> keys = redisTemplate.keys("collections:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    // 解析key，格式为"collections:targetType:targetId"
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
                                // 检查该收藏是否已经存在于MySQL中
                                // 使用MyBatis-Plus的条件查询
                                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                                queryWrapper.eq("user_id", userId);
                                queryWrapper.eq("target_id", targetId);
                                queryWrapper.eq("target_type", targetType);
                                
                                // 执行查询
                                Collections existingCollection = collectionsService.getOne(queryWrapper);
                                
                                // 如果不存在，则插入
                                if (existingCollection == null) {
                                    Collections collection = new Collections();
                                    collection.setUserId(userId);
                                    collection.setTargetId(targetId);
                                    collection.setTargetType(targetType);
                                    collection.setCreatedAt(new Date());
                                    
                                    // 插入到MySQL
                                    collectionsService.save(collection);
                                    System.out.println("新增收藏记录：userId=" + userId + ", targetId=" + targetId + ", targetType=" + targetType);
                                } else {
                                    System.out.println("收藏记录已存在，跳过插入：userId=" + userId + ", targetId=" + targetId + ", targetType=" + targetType);
                                }
                            }
                        }
                    }
                }
            }
            
            // 处理取消收藏的情况：删除MySQL中存在但Redis中不存在的收藏记录
            // 获取MySQL中所有收藏记录
            List<Collections> allCollectionsInMySQL = collectionsService.list();
            for (Collections collection : allCollectionsInMySQL) {
                String redisKey = "collections:" + collection.getTargetType() + ":" + collection.getTargetId();
                // 检查Redis中是否存在该收藏
                Boolean existsInRedis = redisTemplate.opsForSet().isMember(redisKey, collection.getUserId());
                if (existsInRedis == null || !existsInRedis) {
                    // Redis中不存在，从MySQL中删除
                    collectionsService.removeById(collection.getId());
                    System.out.println("从MySQL删除收藏记录：userId=" + collection.getUserId() + ", targetId=" + collection.getTargetId() + ", targetType=" + collection.getTargetType());
                }
            }
            
            System.out.println("Redis收藏数据同步到MySQL成功");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Redis收藏数据同步到MySQL失败：" + e.getMessage());
        }
    }

}
