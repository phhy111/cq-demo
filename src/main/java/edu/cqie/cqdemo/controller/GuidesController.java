package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/guides")
@Slf4j
public class GuidesController {

    // 用于点赞状态查询的并发锁映射
    private final ConcurrentHashMap<String, Object> likeLocks = new ConcurrentHashMap<>();

    @Autowired
    private GuidesService guidesService;

    @Autowired
    private LikesService likesService;

    @Autowired
    private CollectionsService collectionsService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OSSOperationUtil ossOperationUtil;

    /**
     * 获取攻略详情
     */
    @GetMapping("/getGuideDetail")
    public Result getGuideDetail(Integer id) {
        try {
            Guides guide = guidesService.getById(id);
            if (guide != null) {
                return Result.success(guide);
            } else {
                return Result.error("未查询到攻略详情");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("详情查询失败:" + e.getMessage());
        }
    }

    /**
     * 检查用户是否点赞了攻略
     */
    @PostMapping("/checkLikeStatus")
    public Result checkLikeStatus(@RequestBody Likes likes) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权查询其他用户的点赞状态");
            }

            // 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 检查Redis中是否存在该用户的点赞记录
            Boolean isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());

            if (isLiked != null) {
                // 重置Redis Key的过期时间
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                return Result.success(isLiked);
            } else {
                // Redis中不存在，使用同步锁确保只有一个请求打到MySQL
                String lockKey = "lock:like:" + likes.getTargetType() + ":" + likes.getTargetId() + ":" + likes.getUserId();
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    // 再次检查Redis，防止并发情况下已经有其他请求更新了Redis
                    isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());
                    if (isLiked != null) {
                        return Result.success(isLiked);
                    }

                    // Redis中确实不存在，检查MySQL
                    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                    queryWrapper.eq("user_id", likes.getUserId());
                    queryWrapper.eq("target_id", likes.getTargetId());
                    queryWrapper.eq("target_type", likes.getTargetType());

                    Likes existingLike = likesService.getOne(queryWrapper);
                    boolean mysqlLiked = existingLike != null;

                    // 如果MySQL中存在，同步到Redis
                    if (mysqlLiked) {
                        redisTemplate.opsForSet().add(redisKey, likes.getUserId());
                        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                    }

                    return Result.success(mysqlLiked);
                }
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询点赞状态失败：" + e.getMessage());
        }
    }

    /**
     * 点赞攻略
     */
    @PostMapping("/addLikeGuides")
    public Result addLikeGuides(@RequestBody Likes likes) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户点赞");
            }

            // 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
            Long isAdded = redisTemplate.opsForSet().add(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 根据添加结果返回不同的响应
            if (isAdded != null && isAdded == 1) {
                return Result.success("点赞成功");
            } else {
                return Result.success("已点赞，无需重复操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 异常处理，返回错误信息
            e.printStackTrace();
            return Result.error("点赞失败：" + e.getMessage());
        }
    }

    /**
     * 取消点赞攻略
     */
    @PostMapping("/removeLikeGuides")
    public Result removeLikeGuides(@RequestBody Likes likes) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户取消点赞");
            }

            // 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 从Redis Set中移除userId
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 根据移除结果返回不同的响应
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
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("取消点赞失败：" + e.getMessage());
        }
    }

    /**
     * 收藏攻略
     */
    @PostMapping("/addCollections")
    public Result addCollections(@RequestBody Collections collections) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户添加收藏");
            }

            // 规范拼接Redis Key
            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            // 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
            Long isAdded = redisTemplate.opsForSet().add(redisKey, collections.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 根据添加结果返回不同的响应
            if (isAdded != null && isAdded == 1) {
                // 同时添加到MySQL
                collections.setCreatedAt(new Date());
                collectionsService.save(collections);
                return Result.success("收藏成功");
            } else {
                return Result.success("已收藏，无需重复操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 异常处理，返回错误信息
            e.printStackTrace();
            return Result.error("收藏失败：" + e.getMessage());
        }
    }

    /**
     * 取消收藏攻略
     */
    @PostMapping("/removeCollections")
    public Result removeCollections(@RequestBody Collections collections) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户取消收藏");
            }

            // 规范拼接Redis Key
            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            // 从Redis Set中移除userId
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, collections.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 根据移除结果返回不同的响应
            if (isRemoved != null && isRemoved == 1) {
                // 同时从MySQL中删除对应的记录
                // 使用MyBatis-Plus的条件查询和删除
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", collections.getUserId());
                queryWrapper.eq("target_id", collections.getTargetId());
                queryWrapper.eq("target_type", collections.getTargetType());

                // 执行删除操作
                boolean deleted = collectionsService.remove(queryWrapper);
                log.info("从MySQL删除收藏记录：" + (deleted ? "成功" : "失败"));

                return Result.success("取消收藏成功");
            } else {
                return Result.success("未收藏，无需取消操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("取消收藏失败：" + e.getMessage());
        }
    }

    /**
     * 增加攻略浏览量
     * 使用Redis实现，提高性能
     * 与MySQL同步，保证数据一致性
     * 使用锁保证Redis失效时只有一条请求打入MySQL
     */
    @PostMapping("/incrementViewCount")
    public Result incrementViewCount(@RequestParam Integer guideId) {
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "views:guides:" + guideId;

            // 2. 尝试从Redis增加浏览量
            Long viewCount = redisTemplate.opsForValue().increment(redisKey);

            if (viewCount != null) {
                // 设置Redis Key的过期时间为7天
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                return Result.success(viewCount);
            } else {
                // Redis中不存在或操作失败，使用同步锁确保只有一个请求打到MySQL
                String lockKey = "lock:views:guides:" + guideId;
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    // 再次尝试从Redis增加浏览量，防止并发情况下已经有其他请求更新了Redis
                    viewCount = redisTemplate.opsForValue().increment(redisKey);
                    if (viewCount != null) {
                        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                        return Result.success(viewCount);
                    }

                    // Redis中确实不存在，从MySQL中获取当前浏览量并加1
                    Guides guide = guidesService.getById(guideId);
                    if (guide != null) {
                        int currentViewCount = guide.getViewCount() != null ? guide.getViewCount() : 0;
                        int newViewCount = currentViewCount + 1;
                        guide.setViewCount(newViewCount);
                        guidesService.updateById(guide);

                        // 同步到Redis
                        redisTemplate.opsForValue().set(redisKey, newViewCount, 7, TimeUnit.DAYS);

                        return Result.success(newViewCount);
                    } else {
                        return Result.error("攻略不存在");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("增加浏览量失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     */
    private LoginUser getLoginUser() throws IllegalAccessException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof LoginUser)) {
            throw new IllegalAccessException("用户未登录或令牌无效");
        }
        return (LoginUser) principal;
    }

    /**
     * 查询所有状态的攻略数据
     */
    @GetMapping("/GetGuidesInfo")
    public Result<List<Guides>> getGuidesInfo() {
        List<Guides> guidesList = guidesService.getAllGuidesWithAllStatus();
        if (guidesList != null) {
            return Result.success(guidesList);
        } else {
            return Result.error("查询失败");
        }
    }
    
    /**
     * 更新攻略的点赞数、收藏数和评论数
     */
    @PostMapping("/updateLikeCountAndCollectCount")
    public Result updateLikeCountAndCollectCount() {
        try {
            guidesService.updateLikeCountAndCollectCount();
            return Result.success("更新成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 更新攻略状态，将待审核（2）状态更新为发布（1）状态
     */
    @PostMapping("/updateGuideStatus")
    public Result updateGuideStatus(@RequestParam Integer id) {
        try {
            Guides guide = guidesService.getById(id);
            if (guide != null && guide.getStatus() == 2) {
                guide.setStatus(1);
                guidesService.updateById(guide);
                return Result.success("审核通过成功");
            } else {
                return Result.error("攻略不存在或状态不是待审核");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("审核通过失败：" + e.getMessage());
        }
    }

    /**
     * 保存攻略数据（新增或编辑）
     */
    @PostMapping("/saveGuide")
    public Result saveGuide(
            @RequestPart(required = false) MultipartFile file,
            @RequestPart Guides guide) {
        try {
            // 图片上传到 OSS
            if (file != null && !file.isEmpty()) {
                String imageUrl = ossOperationUtil.upload(file, "guides_img/");
                guide.setCoverImg(imageUrl);
            }
            
            if (guide.getId() != null) {
                // 编辑攻略
                guidesService.updateById(guide);
                return Result.success("编辑成功");
            } else {
                // 新增攻略
                guide.setStatus(2); // 默认状态为待审核
                guide.setCreateTime(new Date());
                guide.setUpdateTime(new Date());
                guidesService.save(guide);
                return Result.success("新增成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 删除攻略
     * 【完整删除】同时删除 MySQL 和 Redis 中的所有相关数据
     */
    @DeleteMapping("/deleteGuide")
    public Result deleteGuide(@RequestParam(required = false) Integer id) {
        try {
            // 1. 校验 ID 非空
            if (id == null) {
                return Result.error("删除失败：攻略 ID 不能为空");
            }
            // 2. 检查攻略是否存在
            Guides guide = guidesService.getById(id);
            if (guide == null) {
                return Result.error("删除失败：ID 为" + id + "的攻略不存在");
            }
            // 3. 获取攻略信息用于删除 Redis 数据
            Long userId = guide.getUserId();
            Integer status = guide.getStatus();
            
            // 4. 先删除 MySQL 数据（使用事务保证原子性）
            boolean isDeleted = guidesService.removeById(id);
            if (isDeleted) {
                // 5. 彻底清理 Redis 上的所有相关数据
                try {
                    // 5.1 删除点赞数据
                    String likeRedisKey = "likes:2:" + id;
                    redisTemplate.delete(likeRedisKey);
                    
                    // 5.2 删除收藏数据
                    String collectRedisKey = "collections:2:" + id;
                    redisTemplate.delete(collectRedisKey);
                    
                    // 5.3 删除浏览量数据
                    String viewRedisKey = "views:guides:" + id;
                    redisTemplate.delete(viewRedisKey);
                    
                    // 5.4 删除用户作品缓存（删除该用户所有状态的缓存，强制重新查询）
                    if (userId != null) {
                        redisTemplate.delete("user_works:" + userId + ":1");
                        redisTemplate.delete("user_works:" + userId + ":2");
                        redisTemplate.delete("user_works:" + userId + ":3");
                    }
                    
                    // 5.5 深度清理：从所有 Redis 作品列表中移除该攻略（防止定时任务重新同步）
                    cleanGuideFromAllRedisWorks(id);
                    
                    // 5.6 添加删除标记到 Redis（10 分钟过期），防止同步任务误删
                    String deleteMarkKey = "deleted:guide:" + id;
                    redisTemplate.opsForValue().set(deleteMarkKey, "deleted", 10, TimeUnit.MINUTES);
                    
                    log.info("删除攻略成功，已同步清理 Redis 数据：攻略 ID={}", id);
                } catch (Exception e) {
                    log.warn("删除 Redis 数据失败：{}", e.getMessage());
                    // Redis 删除失败不影响主流程，只记录警告日志
                }
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：执行删除操作无数据变更");
            }
        } catch (Exception e) {
            log.error("删除攻略失败（ID={}）：{}", id, e.getMessage(), e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 从所有 Redis 作品列表中清理指定攻略
     */
    private void cleanGuideFromAllRedisWorks(Integer guideId) {
        try {
            List<String> keys = scanKeys("user_works:*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    List<Object> works = (List<Object>) redisTemplate.opsForValue().get(key);
                    if (works != null && !works.isEmpty()) {
                        boolean removed = works.removeIf(work -> 
                            work instanceof Guides && ((Guides) work).getId().equals(guideId));
                        if (removed) {
                            redisTemplate.opsForValue().set(key, works);
                            log.info("从 Redis 作品列表 {} 中移除已删除攻略 ID={}", key, guideId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("清理 Redis 作品列表中的攻略失败：{}", e.getMessage());
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
