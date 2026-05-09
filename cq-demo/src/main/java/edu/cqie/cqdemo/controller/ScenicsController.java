package edu.cqie.cqdemo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cqie.cqdemo.annotation.RedisLog;
import edu.cqie.cqdemo.common.Result;
import edu.cqie.cqdemo.dto.RegionScenicsCountDTO;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Collections;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.entity.LoginUser;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.service.CollectionsService;
import edu.cqie.cqdemo.service.LikesService;
import edu.cqie.cqdemo.service.ScenicsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import edu.cqie.cqdemo.util.OSSOperationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/scenics")
@Slf4j
public class ScenicsController {
    @Autowired
    private ScenicsService scenicsService;
    
    @Autowired
    private LikesService likesService;
    
    @Autowired
    private CollectionsService collectionsService;
    
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private edu.cqie.cqdemo.redis.util.RedisUtil redisUtil;

    private final Map<String, Object> likeLocks = new ConcurrentHashMap<>();
    /**
     * 获取所有景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfo")
    public Result<List<Scenics>> getScenicsInfo() {
       List<Scenics> scenicsList = scenicsService.list();
       if (scenicsList != null){
           return Result.success(scenicsList);
       }else {
           return Result.error("未查询到景点信息");
       }
    }
    /**
     * 获取轮播图信息
     * @return
     */
    @GetMapping("/GetSlideShowInfo")
    public Result<List<Scenics>> getSlideShowInfo() {
        List<Scenics> scenicsList = scenicsService.getSlideShowInfo();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到轮播图信息");
        }
    }

    /**
     * 获取对应区域的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByRegionId")
    public Result<List<Scenics>> getScenicsInfoByRegionId(Integer regionId) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByRegionId(regionId);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该区域下的景点信息");
        }
    }
    /**
     * 获取不同等级的景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByLevel")
    public Result<List<Scenics>> getScenicsInfoByLevel(String level) {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLevel(level);
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该等级的景点信息");
        }
    }
    /**
     * 按评分高低获取景点信息
     * @return
     */
    @GetMapping("/GetScenicsInfoByScore")
    public Result<List<Scenics>> getScenicsInfoByScore() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByScore();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该评分的景点信息");
        }
    }
    /**
     * 按点赞量排序
     * @return
     */
    @GetMapping("/GetScenicsInfoByLike")
    public Result<List<Scenics>> getScenicsInfoByLike() {
        List<Scenics> scenicsList = scenicsService.getScenicsInfoByLikeCount();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到该点赞量的景点信息");
        }
    }
    /**
     * 获取推荐景点信息
     * @return
     */
    @GetMapping("/GetRecommendedScenics")
    public Result<List<Scenics>> getRecommendedScenics() {
        List<Scenics> scenicsList = scenicsService.getRecommendedScenics();
        if (scenicsList != null){
            return Result.success(scenicsList);
        }else {
            return Result.error("未查询到推荐景点信息");
        }
    }
    /**
     * 添加景点浏览量
     * @return
     */
    @PostMapping("/AddViewCount")
    public Result<String> addViewCount(Integer id) {
        boolean result = scenicsService.addViewCount(id);
        if (result){
            return Result.success("添加成功");
        }else {
            return Result.error("添加失败");
        }
    }
    /**
     * 获取景点详情信息
     * @return
     */
    @GetMapping("/GetScenicsDetailInfoById")
    public Result<List<ScenicsDTO>> getScenicsDetailInfoById(Integer id) {
        List<ScenicsDTO> scenicsDTOList = scenicsService.getScenicsDetailInfoById(id);
        if (scenicsDTOList != null){
            return Result.success(scenicsDTOList);
        }else {
            return Result.error("未查询到该景点详情信息");
        }
    }
    
    /**
     * 更新景点的点赞数、收藏数和评论数
     */
    @RedisLog(type = "INFO", module = "SCENIC")
    @PostMapping("/updateLikeCountAndCollectCount")
    public Result updateLikeCountAndCollectCount(Integer id) {
        try {
            scenicsService.updateLikeCountAndCollectCount(id);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("更新失败：" + e.getMessage());
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
     * 增加景点点赞
     */
    @RedisLog(type = "INFO", module = "SCENIC")
    @PostMapping("/addLikeScenics")
    public Result addLikeScenics(@RequestBody Likes likes){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户点赞");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Long isAdded = redisTemplate.opsForSet().add(redisKey, likes.getUserId());
            redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

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
            log.error("操作失败", e);
            return Result.error("点赞失败：" + e.getMessage());
        }
    }

    /**
     * 取消景点点赞
     */
    @RedisLog(type = "INFO", module = "SCENIC")
    @PostMapping("/removeLikeScenics")
    public Result removeLikeScenics(@RequestBody Likes likes){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权为其他用户取消点赞");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, likes.getUserId());
            redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

            if (isRemoved != null && isRemoved == 1) {
                QueryWrapper<Likes> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", likes.getUserId());
                queryWrapper.eq("target_id", likes.getTargetId());
                queryWrapper.eq("target_type", likes.getTargetType());
                boolean deleted = likesService.remove(queryWrapper);
                log.info("从 MySQL 删除点赞记录：" + (deleted ? "成功" : "失败"));
                return Result.success("取消点赞成功");
            } else {
                return Result.success("未点赞，无需取消操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("取消点赞失败：" + e.getMessage());
        }
    }

    /**
     * 检查用户是否点赞了某个景点
     */
    @PostMapping("/checkLikeStatus")
    public Result checkLikeStatus(@RequestBody Likes likes){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(likes.getUserId())) {
                return Result.error("无权查询其他用户的点赞状态");
            }

            String redisKey = "likes:" + likes.getTargetType() + ":" + likes.getTargetId();
            Boolean isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());

            if (isLiked != null && isLiked) {
                return Result.success(true);
            } else if (isLiked != null && !isLiked) {
                return Result.success(false);
            } else {
                String lockKey = "lock:like:" + likes.getTargetType() + ":" + likes.getTargetId() + ":" + likes.getUserId();
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    isLiked = redisTemplate.opsForSet().isMember(redisKey, likes.getUserId());
                    if (isLiked != null) {
                        return Result.success(isLiked);
                    }

                    QueryWrapper<Likes> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("user_id", likes.getUserId());
                    queryWrapper.eq("target_id", likes.getTargetId());
                    queryWrapper.eq("target_type", likes.getTargetType());
                    Likes existingLike = likesService.getOne(queryWrapper);
                    boolean mysqlLiked = existingLike != null;

                    if (mysqlLiked) {
                        redisTemplate.opsForSet().add(redisKey, likes.getUserId());
                        redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                    }

                    return Result.success(mysqlLiked);
                }
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("查询点赞状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取景点的点赞数量
     */
    @GetMapping("/getLikeCount")
    public Result getLikeCount(Integer targetType, Long targetId){
        try {
            String redisKey = "likes:" + targetType + ":" + targetId;
            Long likeCount = redisTemplate.opsForSet().size(redisKey);

            if (likeCount != null) {
                redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                return Result.success(likeCount);
            } else {
                String lockKey = "lock:like:count:" + targetType + ":" + targetId;
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    likeCount = redisTemplate.opsForSet().size(redisKey);
                    if (likeCount != null) {
                        redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                        return Result.success(likeCount);
                    }

                    QueryWrapper<Likes> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("target_id", targetId);
                    queryWrapper.eq("target_type", targetType);
                    List<Likes> likesList = likesService.list(queryWrapper);
                    long mysqlLikeCount = likesList.size();

                    for (Likes like : likesList) {
                        redisTemplate.opsForSet().add(redisKey, like.getUserId());
                    }
                    redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

                    return Result.success(mysqlLikeCount);
                }
            }
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("查询点赞数量失败：" + e.getMessage());
        }
    }

    /**
     * 增加景点收藏
     */
    @RedisLog(type = "INFO", module = "SCENIC")
    @PostMapping("/addCollections")
    public Result addCollections(@RequestBody Collections collections){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户添加收藏");
            }

            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            Long isAdded = redisTemplate.opsForSet().add(redisKey, collections.getUserId());
            redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

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
            log.error("操作失败", e);
            return Result.error("收藏失败：" + e.getMessage());
        }
    }

    /**
     * 取消景点收藏
     */
    @RedisLog(type = "INFO", module = "SCENIC")
    @PostMapping("/removeCollections")
    public Result removeCollections(@RequestBody Collections collections){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权为其他用户取消收藏");
            }

            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            Long isRemoved = redisTemplate.opsForSet().remove(redisKey, collections.getUserId());
            redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

            if (isRemoved != null && isRemoved == 1) {
                QueryWrapper<Collections> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id", collections.getUserId());
                queryWrapper.eq("target_id", collections.getTargetId());
                queryWrapper.eq("target_type", collections.getTargetType());
                boolean deleted = collectionsService.remove(queryWrapper);
                return Result.success("取消收藏成功");
            } else {
                return Result.success("未收藏，无需取消操作");
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("取消收藏失败：" + e.getMessage());
        }
    }

    /**
     * 检查用户是否收藏了某个景点
     */
    @PostMapping("/checkCollectionStatus")
    public Result checkCollectionStatus(@RequestBody Collections collections){
        try {
            LoginUser loginUser = getLoginUser();
            Long currentUserId = loginUser.getId();

            if (!currentUserId.equals(collections.getUserId())) {
                return Result.error("无权查询其他用户的收藏状态");
            }

            String redisKey = "collections:" + collections.getTargetType() + ":" + collections.getTargetId();
            Boolean isCollected = redisTemplate.opsForSet().isMember(redisKey, collections.getUserId());

            if (isCollected != null && isCollected) {
                return Result.success(true);
            } else if (isCollected != null && !isCollected) {
                return Result.success(false);
            } else {
                String lockKey = "lock:collection:" + collections.getTargetType() + ":" + collections.getTargetId() + ":" + collections.getUserId();
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    isCollected = redisTemplate.opsForSet().isMember(redisKey, collections.getUserId());
                    if (isCollected != null) {
                        return Result.success(isCollected);
                    }

                    QueryWrapper<Collections> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("user_id", collections.getUserId());
                    queryWrapper.eq("target_id", collections.getTargetId());
                    queryWrapper.eq("target_type", collections.getTargetType());
                    Collections existingCollection = collectionsService.getOne(queryWrapper);
                    boolean mysqlCollected = existingCollection != null;

                    if (mysqlCollected) {
                        redisTemplate.opsForSet().add(redisKey, collections.getUserId());
                        redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                    }

                    return Result.success(mysqlCollected);
                }
            }
        } catch (IllegalAccessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("查询收藏状态失败：" + e.getMessage());
        }
    }

    /**
     * 获取景点的收藏数量
     */
    @GetMapping("/getCollectionCount")
    public Result getCollectionCount(Integer targetType, Long targetId){
        try {
            String redisKey = "collections:" + targetType + ":" + targetId;
            Long collectionCount = redisTemplate.opsForSet().size(redisKey);

            if (collectionCount != null) {
                redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                return Result.success(collectionCount);
            } else {
                String lockKey = "lock:collection:count:" + targetType + ":" + targetId;
                Object lock = likeLocks.computeIfAbsent(lockKey, k -> new Object());

                synchronized (lock) {
                    collectionCount = redisTemplate.opsForSet().size(redisKey);
                    if (collectionCount != null) {
                        redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);
                        return Result.success(collectionCount);
                    }

                    QueryWrapper<Collections> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("target_id", targetId);
                    queryWrapper.eq("target_type", targetType);
                    List<Collections> collectionsList = collectionsService.list(queryWrapper);
                    long mysqlCollectionCount = collectionsList.size();

                    for (Collections collection : collectionsList) {
                        redisTemplate.opsForSet().add(redisKey, collection.getUserId());
                    }
                    redisUtil.expireWithJitter(redisKey, 26, TimeUnit.DAYS);

                    return Result.success(mysqlCollectionCount);
                }
            }
        } catch (Exception e) {
            log.error("操作失败", e);
            return Result.error("查询收藏数量失败：" + e.getMessage());
        }
    }
    /**
     * 多表关联分页查询
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping("/selectScenicsAndRegionPage")
    public Result selectScenicsAndRegionPage(
            @RequestParam(defaultValue = "1") Integer pageNum, // 默认第1页
            @RequestParam(defaultValue = "5") Integer pageSize) // 默认每页5条
    {
        IPage<ScenicsAndRegionDTO> page = scenicsService.selectScenicsAndRegionPage(pageNum, pageSize);
        if (page != null){
            return Result.success(page);
        }else {
            return Result.error("多表关联分页查询失败");
        }
    }
    /**
     * 添加景点信息
     * @return
     */
    @Autowired
    private OSSOperationUtil ossOperationUtil;
    @PostMapping("/AddScenicsInfo")
    public Result addScenicsInfo(
            MultipartFile file,
            Scenics scenics) {
        if (file != null) {
            //获取生成的路径
            String imageUrl = ossOperationUtil.uploadScenicsCoverImg(file);
            scenics.setCoverImg(imageUrl);

            boolean result = scenicsService.addScenicsInfo(scenics);
            if (result) {
                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
        } else {
            log.warn("图片为空");
            return Result.error("图片为空");
        }
    }
    /**
     * 批量删除景点信息
     * @return
     */
    @PostMapping("/DeleteScenics")
    public Result deleteScenics(@RequestParam("id") List<Integer>  id){
        if (id != null){
            boolean result = scenicsService.removeByIds(id);
            if (result){
                return Result.success("删除成功");
            }else {
                return Result.error("删除失败");
            }
        }else {
            return Result.error("删除失败");
        }
    }
    /**
     * 修改景点信息
     * @return
     */
    @PostMapping("/UpdateScenicsInfo")
    public Result updateScenicsInfo(MultipartFile file, Scenics scenics){
        if (scenics != null && file != null){
            String imageUrl = ossOperationUtil.uploadScenicsCoverImg(file);
            scenics.setCoverImg(imageUrl);

            boolean result = scenicsService.updateScenicsInfo(scenics);
            if (result){
                return Result.success("修改成功");
            }else {
                return Result.error("修改失败");
            }
        }else {
            return Result.error("文件或景点数据信息为空");
        }
    }

    /**
     * 查询每个区域景点数量
     * @return
     */
    @GetMapping("/GetRegionScenicsCount")
    public Result<List<RegionScenicsCountDTO>> getRegionScenicsCount() {
        List<RegionScenicsCountDTO> list = scenicsService.getRegionScenicsCount();
        if (list != null) {
            return Result.success(list);
        } else {
            return Result.error("未查询到区域景点数量信息");
        }
    }
}
