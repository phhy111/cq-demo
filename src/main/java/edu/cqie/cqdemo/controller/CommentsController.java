package edu.cqie.cqdemo.controller;

import edu.cqie.cqdemo.annotation.RedisLog;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.service.SensitiveWordService;
import edu.cqie.cqdemo.dto.ScenicsCommentsDTO;
import edu.cqie.cqdemo.dto.CommentsDTO;
import edu.cqie.cqdemo.entity.Comments;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.Users;
import edu.cqie.cqdemo.service.CommentsService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.UserService;
import edu.cqie.cqdemo.util.JwtUtil;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import edu.cqie.cqdemo.entity.LoginUser;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/comments")
public class CommentsController {
    @Autowired
    private CommentsService commentsService;

    @Autowired
    private JwtUtil jwtUtil;  // 注入 JWT 工具类

    @Autowired
    private LikesService likesService;

    @Autowired
    private OSSOperationUtil ossOperationUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SensitiveWordService sensitiveWordService;

    // 用于存储点赞状态查询的同步锁，防止缓存穿透
    private final ConcurrentHashMap<String, Object> likeLocks = new ConcurrentHashMap<>();

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
     * 获取景点评论信息
     * @param id 景点id
     * @return 景点评论信息
     */
    @GetMapping("/GetScenicsCommentsInfo")
    public Result<List<ScenicsCommentsDTO>> getScenicsCommentsInfo(Integer id) {
        List<ScenicsCommentsDTO> commentsInfo = commentsService.getScenicsCommentsInfo(id);
        if (commentsInfo != null){
            return Result.success(commentsInfo);
        }else {
            return Result.error("未查询到该景点的评论信息");
        }
    }

    /**
     * 添加评论信息
     * @param comments 评论信息
     * @return 添加结果
     */
    @RedisLog(type = "INFO", module = "COMMENT")
    @PostMapping("/AddCommentsInfo")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> addCommentsInfo(
            @RequestPart(required = false) MultipartFile file,
            @RequestPart Comments comments, // 前端传的comments中无userId
            HttpServletRequest request) {
        try {
            // 1. Token解析（原有逻辑）
            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("无效的Token");
            }
            Long userId = jwtUtil.getUserIdFromToken(token);

            // 2. 关键修复：将解析到的 userId 设置到 comments 实体中
            comments.setUserId(userId); // 之前漏掉了这一步！

            // 2.5 敏感词检查
            if (sensitiveWordService.containsSensitiveWord(comments.getContent())) {
                List<String> sensitiveWords = sensitiveWordService.findSensitiveWords(comments.getContent());
                return Result.error("评论包含敏感词：" + String.join(",", sensitiveWords));
            }

            // 3. 填充时间（原有逻辑）
            comments.setCreatedAt(new Date());
            comments.setUpdatedAt(new Date());

            // 4. 图片上传（原有逻辑）
            if (file != null && !file.isEmpty()) {
                String imageUrl = ossOperationUtil.upload(file);
                comments.setImages(imageUrl);
            } else {
                comments.setImages("");
            }

            // 5. 保存评论
            boolean success = commentsService.addCommentWithUserInfo(comments);
            if (success) {
                return Result.success(true);
            } else {
                return Result.error("评论添加失败");
            }
        } catch (Exception e) {
            log.error("添加评论失败", e);
            return Result.error("服务器内部错误：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "COMMENT")
    @PostMapping("/addRouteComment")
    public Result<?> addCommentsInfo(@RequestBody Comments comments,HttpServletRequest request) {
        try
        {

            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("无效的Token");
            }
            boolean success = jwtUtil.validateToken(token);
            if (success)
            {
                Long userId = jwtUtil.getUserIdFromToken(token);
                
                // 敏感词检查
                if (sensitiveWordService.containsSensitiveWord(comments.getContent())) {
                    List<String> sensitiveWords = sensitiveWordService.findSensitiveWords(comments.getContent());
                    return Result.error("评论包含敏感词");
                }
                
                comments.setUserId(userId);
                commentsService.addCommentWithUserInfo(comments);
                // 查询用户信息
                Users user = userService.getUserById(userId);
                // 构建响应对象
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("message", "评论添加成功");
                response.put("user", user);
                System.out.println("response:"+response);
                return Result.success(response);
            }else{
                return Result.success("用户未登录");
            }

        } catch (Exception e) {
            // 打印异常便于排查
            e.printStackTrace();
            return Result.error("评论添加失败：" + e.getMessage());
        }
    }

    @RedisLog(type = "INFO", module = "COMMENT")
    @PostMapping("/replyComment")
    public Result<?> replyComment(@RequestBody Comments comments, HttpServletRequest request) {
        try {
            String token = jwtUtil.getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("无效的Token");
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            
            // 敏感词检查
            if (sensitiveWordService.containsSensitiveWord(comments.getContent())) {
                List<String> sensitiveWords = sensitiveWordService.findSensitiveWords(comments.getContent());
                return Result.error("评论包含敏感词：" + String.join(",", sensitiveWords));
            }
            
            comments.setUserId(userId);
            commentsService.addCommentWithUserInfo(comments);
            // 查询用户信息
            Users user = userService.getUserById(userId);
            // 构建响应对象
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "回复添加成功");
            response.put("user", user);
            return Result.success(response);
        } catch (Exception e) {
            // 打印异常便于排查
            e.printStackTrace();
            return Result.error("回复添加失败：" + e.getMessage());
        }
    }

    @GetMapping("/getCommentReplies")
    public Result<List<CommentsDTO>> getCommentReplies(Integer commentId, Integer page, Integer size) {
        try {
            // 默认值设置
            if (page == null || page < 1) {
                page = 1;
            }
            if (size == null || size < 1) {
                size = 10;
            }
            List<CommentsDTO> replies = commentsService.getCommentReplies(commentId, page, size);
            System.out.println("replies:"+replies);
            return Result.success(replies);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取回复失败：" + e.getMessage());
        }
    }

    @GetMapping("/getCommentRepliesRecursive")
    public Result<List<CommentsDTO>> getCommentRepliesRecursive(Integer commentId) {
        try {
            List<CommentsDTO> replies = commentsService.getCommentRepliesRecursive(commentId);
            System.out.println("replies:"+replies);
            return Result.success(replies);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取回复失败：" + e.getMessage());
        }
    }

    @GetMapping("/getRoutesComments")
    public Result<List<CommentsDTO>> getRoutesComments(Integer targetId,Integer targetType)
    {
        try
        {
            List<CommentsDTO> listComments = commentsService.getRoutesComments(targetId,targetType);
            System.out.println("listComments:"+listComments);
            return Result.success(listComments);
        }catch (Exception e)
        {
            return Result.error("获取路线评论失败" + e.getMessage());
        }

    }

    @GetMapping("/getGuidesComments")
    public Result<List<CommentsDTO>> getGuidesComments(Integer targetId,Integer targetType)
    {
        try
        {
            List<CommentsDTO> listComments = commentsService.getRoutesComments(targetId,targetType);
            System.out.println("listComments:"+listComments);
            return Result.success(listComments);
        }catch (Exception e)
        {
            return Result.error("获取攻略评论失败" + e.getMessage());
        }

    }

    @PostMapping("/addLikeComments")
    public Result addLikeComments(@RequestBody Likes likes){
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户点赞");
            }

            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 2. 正确调用Redis Set的add方法：opsForSet()获取Set操作对象，再调用add
            Long isAdded = redisTemplate.opsForSet().add(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 3. 根据添加结果返回不同的响应
            if (isAdded != null && isAdded == 1) {
                log.info("用户 {} 点赞成功，目标类型：{}，目标ID：{}", likes.getUserId(), likes.getTargetType(), likes.getTargetId());
                return Result.success("点赞成功");
            } else {
                return Result.success("已点赞，无需重复操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 4. 异常处理，返回错误信息
            e.printStackTrace();
            return Result.error("点赞失败：" + e.getMessage());
        }
    }

    @PostMapping("/removeLikeComments")
    public Result removeLikeComments(@RequestBody Likes likes){
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户取消点赞");
            }

            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 2. 从Redis Set中移除userId
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, likes.getUserId());
            // 设置7天过期时间
            redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

            // 3. 根据移除结果返回不同的响应
            if (isRemoved != null && isRemoved == 1) {
                log.info("用户 {} 取消点赞成功，目标类型：{}，目标ID：{}", likes.getUserId(), likes.getTargetType(), likes.getTargetId());
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
     * 查询用户是否点赞了某个目标
     * 优先从Redis读取，提高性能
     */
    @PostMapping("/checkLikeStatus")
    public Result checkLikeStatus(@RequestBody Likes likes){
        try {
            // 获取当前登录用户信息
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            // 验证请求中的用户ID是否与当前登录用户一致
            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权查询其他用户的点赞状态");
            }

            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            // 2. 检查Redis中是否存在该用户的点赞记录
            Boolean isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());

            if (isLiked != null && isLiked) {
                return Result.success(true);
            } else if (isLiked != null && !isLiked) {
                return Result.success(false);
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
                    } else {
                        // 如果MySQL中不存在，也同步到Redis，设置为false，防止缓存穿透
                        // 注意：这里不能直接存储false，因为Redis Set只存储存在的元素
                        // 所以我们不做任何操作，让Redis自然过期
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
     * 获取某个目标的点赞数量
     * 优先从Redis读取，提高性能
     */
    @GetMapping("/getLikeCount")
    public Result getLikeCount(Integer targetType, Long targetId){
        try {
            // 1. 规范拼接Redis Key
            String redisKey = "likes:" + targetType + ":" + targetId;
            // 2. 从Redis中获取点赞数量
            Long likeCount = redisTemplate.opsForSet().size(redisKey);

            if (likeCount != null) {
                // 重置Redis Key的过期时间
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                return Result.success(likeCount);
            } else {
                // Redis中不存在，使用同步锁确保只有一个请求打到MySQL
                String lockKey = "lock:like:count:" + targetType + ":" + targetId;
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    // 再次检查Redis，防止并发情况下已经有其他请求更新了Redis
                    likeCount = redisTemplate.opsForSet().size(redisKey);
                    if (likeCount != null) {
                        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);
                        return Result.success(likeCount);
                    }

                    // Redis中确实不存在，从MySQL中查询
                    com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Likes> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                    queryWrapper.eq("target_id", targetId);
                    queryWrapper.eq("target_type", targetType);

                    // 查询具体的点赞记录
                    List<Likes> likesList = likesService.list(queryWrapper);
                    long mysqlLikeCount = likesList.size();

                    // 同步到Redis，将具体的点赞用户ID添加到集合中
                    for (Likes like : likesList) {
                        redisTemplate.opsForSet().add(redisKey, like.getUserId());
                    }
                    // 设置过期时间
                    redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

                    return Result.success(mysqlLikeCount);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询点赞数量失败：" + e.getMessage());
        }
    }

}
