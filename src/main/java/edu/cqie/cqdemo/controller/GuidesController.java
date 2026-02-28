package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.LikesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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
}
