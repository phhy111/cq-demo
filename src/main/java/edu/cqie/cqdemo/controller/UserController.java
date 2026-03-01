package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.GuidesService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.RoutesService;
import edu.cqie.cqdemo.service.ScenicsService;
import edu.cqie.cqdemo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import edu.cqie.cqdemo.entity.LoginUser;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoutesService routesService;

    @Autowired
    private GuidesService guidesService;

    @Autowired
    private CollectionsService collectionsService;

    @Autowired
    private LikesService likesService;

    @Autowired
    private ScenicsService scenicsService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取路线或攻略的发布者信息
     */
    @GetMapping("/getPublisher")
    public Result<Users> getPublisher(@RequestParam Long userId) {
        try {
            Users user = userService.getById(userId);
            if (user != null) {
                return Result.success(user);
            } else {
                return Result.error("用户不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取发布者信息失败：" + e.getMessage());
        }
    }

    /**
     * 切换用户收藏状态（关注/取消关注）
     */
    @PostMapping("/toggleUserCollection")
    public Result toggleUserCollection(@RequestBody Collections collection) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(collection.getUserId())) {
                return Result.error("无权为其他用户操作");
            }

            // 确保target_type为7（用户）
            collection.setTargetType(7);

            // 1. 规范拼接Redis Key
            String redisKey = "collections:" + collection.getTargetType() + ":" + collection.getTargetId();
            // 2. 检查是否已收藏
            Boolean isCollected = redisTemplate.opsForSet().isMember(redisKey, collection.getUserId());

            if (isCollected != null && isCollected) {
                // 已收藏，取消收藏
                Long isRemoved = redisTemplate.opsForSet().remove(redisKey, collection.getUserId());
                // 设置26天过期时间
                redisTemplate.expire(redisKey, 26, TimeUnit.DAYS);

                if (isRemoved != null && isRemoved == 1) {
                    // 同时从MySQL中删除对应的记录
                    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                    queryWrapper.eq("user_id", collection.getUserId());
                    queryWrapper.eq("target_id", collection.getTargetId());
                    queryWrapper.eq("target_type", collection.getTargetType());

                    boolean deleted = collectionsService.remove(queryWrapper);
                    log.info("取消关注用户：" + (deleted ? "成功" : "失败"));

                    return Result.success("取消关注成功");
                } else {
                    return Result.success("未关注，无需取消操作");
                }
            } else {
                // 未收藏，添加收藏
                // 1. 规范拼接Redis Key
                String redisKeyAdd = "collections:" + collection.getTargetType() + ":" + collection.getTargetId();
                // 2. 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
                Long isAdded = redisTemplate.opsForSet().add(redisKeyAdd, collection.getUserId());
                // 设置7天过期时间
                redisTemplate.expire(redisKeyAdd, 7, TimeUnit.DAYS);

                // 3. 根据添加结果返回不同的响应
                if (isAdded != null && isAdded == 1) {
                    // 同时添加到MySQL
                    collection.setCreatedAt(new Date());
                    collectionsService.save(collection);
                    log.info("关注用户成功");
                    return Result.success("关注成功");
                } else {
                    return Result.success("已关注，无需重复操作");
                }
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("操作失败：" + e.getMessage());
        }
    }

    /**
     * 检查是否已收藏
     */
    @PostMapping("/checkUserCollection")
    public Result checkUserCollection(@RequestBody Collections collection) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(collection.getUserId())) {
                return Result.error("无权查询其他用户的收藏状态");
            }

            // 1. 规范拼接Redis Key
            String redisKey = "collections:" + collection.getTargetType() + ":" + collection.getTargetId();
            // 2. 检查Redis中是否存在该收藏记录
            Boolean isCollected = redisTemplate.opsForSet().isMember(redisKey, collection.getUserId());

            if (isCollected != null) {
                return Result.success(isCollected);
            } else {
                // Redis中不存在，检查MySQL
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                queryWrapper.eq("user_id", collection.getUserId());
                queryWrapper.eq("target_id", collection.getTargetId());
                queryWrapper.eq("target_type", collection.getTargetType());

                Collections existingCollection = collectionsService.getOne(queryWrapper);
                boolean mysqlCollected = existingCollection != null;

                // 如果MySQL中存在，同步到Redis
                if (mysqlCollected) {
                    redisTemplate.opsForSet().add(redisKey, collection.getUserId());
                    redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                }

                return Result.success(mysqlCollected);
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询收藏状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户详情页信息（仿照抖音作者详情页）
     */
    @GetMapping("/getUserDetail")
    public Result getUserDetail(@RequestParam Long userId) {
        try {
            // 获取用户基本信息
            Users user = userService.getById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 获取用户发布的路线
            List<Routes> userRoutes = routesService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Routes>()
                            .eq("user_id", userId)
                            .eq("status", 1)
                            .orderByDesc("created_at")
            );

            // 获取用户发布的攻略
            List<Guides> userGuides = guidesService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Guides>()
                            .eq("user_id", userId)
                            .eq("status", 1)
                            .orderByDesc("created_at")
            );

            // 获取关注数量（用户关注的人数）
            long followCount = collectionsService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>()
                            .eq("user_id", userId)
                            .eq("target_type", 7)
            );

            // 获取粉丝数量（关注用户的人数）
            long fanCount = collectionsService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>()
                            .eq("target_id", userId)
                            .eq("target_type", 7)
            );

            // 获取获赞数量（简化版，实际应该统计所有内容的点赞数）
            long likeCount = likesService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes>()
                            .inSql("target_id", "SELECT id FROM routes WHERE user_id = " + userId)
                            .eq("target_type", 3)
            ) + likesService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes>()
                            .inSql("target_id", "SELECT id FROM guides WHERE user_id = " + userId)
                            .eq("target_type", 4)
            );

            // 构建返回结果
            java.util.Map<String, Object> resultMap = new java.util.HashMap<>();
            resultMap.put("user", user);
            resultMap.put("routes", userRoutes);
            resultMap.put("guides", userGuides);
            resultMap.put("followCount", followCount);
            resultMap.put("fanCount", fanCount);
            resultMap.put("likeCount", likeCount);

            return Result.success(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户关注列表
     */
    @GetMapping("/getFollowList")
    public Result getFollowList(@RequestParam Long userId) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(userId)) {
                return Result.error("无权查询其他用户的关注列表");
            }

            // 查询用户关注的人
            List<Collections> follows = collectionsService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>()
                            .eq("user_id", userId)
                            .eq("target_type", 7)
                            .orderByDesc("created_at")
            );

            // 获取关注用户的详细信息
            List<Users> followUsers = new java.util.ArrayList<>();
            for (Collections follow : follows) {
                Users user = userService.getById(follow.getTargetId());
                if (user != null) {
                    followUsers.add(user);
                }
            }

            return Result.success(followUsers);
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取关注列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户粉丝列表
     */
    @GetMapping("/getFanList")
    public Result getFanList(@RequestParam Long userId) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(userId)) {
                return Result.error("无权查询其他用户的粉丝列表");
            }

            // 查询关注用户的人
            List<Collections> fans = collectionsService.list(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections>()
                            .eq("target_id", userId)
                            .eq("target_type", 7)
                            .orderByDesc("created_at")
            );

            // 获取粉丝用户的详细信息
            List<Users> fanUsers = new java.util.ArrayList<>();
            for (Collections fan : fans) {
                Users user = userService.getById(fan.getUserId());
                if (user != null) {
                    fanUsers.add(user);
                }
            }

            return Result.success(fanUsers);
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取粉丝列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户收藏列表
     */
    @PostMapping("/getUserCollections")
    public Result getUserCollections(@RequestBody Collections collection) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(collection.getUserId())) {
                return Result.error("无权查询其他用户的收藏列表");
            }

            // 从MySQL中查询用户的收藏记录
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Collections> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("user_id", collection.getUserId());
            if (collection.getTargetType() != null) {
                queryWrapper.eq("target_type", collection.getTargetType());
            }
            queryWrapper.orderByDesc("created_at");

            List<Collections> collections = collectionsService.list(queryWrapper);
            return Result.success(collections);
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取收藏列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户喜欢列表
     */
    @PostMapping("/getUserLikes")
    public Result getUserLikes(@RequestBody Likes likes) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权查询其他用户的喜欢列表");
            }

            // 从MySQL中查询用户的喜欢记录
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            queryWrapper.eq("user_id", likes.getUserId());
            if (likes.getTargetType() != null) {
                queryWrapper.eq("target_type", likes.getTargetType());
            }
            queryWrapper.orderByDesc("created_at");

            List<Likes> likesList = likesService.list(queryWrapper);
            return Result.success(likesList);
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取喜欢列表失败：" + e.getMessage());
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
     * 获取用户作品列表
     */
    @GetMapping("/getUserWorks")
    public Result getUserWorks(@RequestParam Long userId, @RequestParam Integer status, @RequestParam(required = false) String type) {
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(userId)) {
                return Result.error("无权查询其他用户的作品列表");
            }

            // 构建Redis键
            String redisKey = "user_works:" + userId + ":" + status;
            if (type != null && !type.isEmpty()) {
                redisKey += ":" + type;
            }

            // 尝试从Redis获取数据
            List<Object> works = (List<Object>) redisTemplate.opsForValue().get(redisKey);
            if (works != null) {
                return Result.success(works);
            }

            // Redis中没有数据，使用同步锁确保只有一个请求进入MySQL
            String lockKey = "lock:user_works:" + userId + ":" + status;
            if (type != null && !type.isEmpty()) {
                lockKey += ":" + type;
            }

            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 30, TimeUnit.SECONDS);
            if (acquired != null && acquired) {
                try {
                    // 从MySQL获取数据
                    List<Object> result = new java.util.ArrayList<>();

                    // 根据类型获取不同的数据
                    switch (type) {
                        case "routes":
                            List<Routes> routes = routesService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Routes>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(routes);
                            break;
                        case "guides":
                            List<Guides> guides = guidesService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Guides>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(guides);
                            break;
                        case "scenics":
                            List<Scenics> scenics = scenicsService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Scenics>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(scenics);
                            break;
                        case "food_categories":
                            // 这里需要添加美食分类的查询逻辑
                            break;
                        case "foods":
                            // 这里需要添加店铺的查询逻辑
                            break;
                        default:
                            // 获取所有类型的数据
                            List<Routes> allRoutes = routesService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Routes>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(allRoutes);

                            List<Guides> allGuides = guidesService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Guides>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(allGuides);

                            List<Scenics> allScenics = scenicsService.list(
                                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Scenics>()
                                            .eq("user_id", userId)
                                            .eq("status", status)
                                            .orderByDesc("created_at")
                            );
                            result.addAll(allScenics);
                            break;
                    }

                    // 同步到Redis
                    redisTemplate.opsForValue().set(redisKey, result, 1, TimeUnit.HOURS);

                    return Result.success(result);
                } finally {
                    // 释放锁
                    redisTemplate.delete(lockKey);
                }
            } else {
                // 其他请求已经在处理，等待一段时间后重试
                Thread.sleep(100);
                // 再次尝试从Redis获取数据
                works = (List<Object>) redisTemplate.opsForValue().get(redisKey);
                if (works != null) {
                    return Result.success(works);
                } else {
                    return Result.error("获取作品列表失败，请稍后重试");
                }
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取作品列表失败：" + e.getMessage());
        }
    }

}