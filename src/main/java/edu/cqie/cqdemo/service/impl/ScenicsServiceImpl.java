package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.mapper.ScenicsMapper;
import edu.cqie.cqdemo.service.ScenicsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class ScenicsServiceImpl extends ServiceImpl<ScenicsMapper, Scenics> implements ScenicsService {
    /**
     * 获取主页轮播图信息
     * @return
     */
    @Autowired
    private ScenicsMapper scenicsMapper;
    
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public List<Scenics> getSlideShowInfo() {
        return scenicsMapper.getSlideShowInfo();
    }
    /**
     * 获取对应区域的景点信息
     * @return
     */
    @Override
    public List<Scenics> getScenicsInfoByRegionId(Integer regionId) {
        return scenicsMapper.getScenicsInfoByRegionId(regionId);
    }
    /**
     * 获取不同等级的景点信息
     * @return
     */
    @Override
    public List<Scenics> getScenicsInfoByLevel(String level) {
        return scenicsMapper.getScenicsInfoByLevel(level);
    }

    @Override
    public List<Scenics> getScenicsInfoByScore() {
        return scenicsMapper.getScenicsInfoByScore();
    }

    @Override
    public List<Scenics> getScenicsInfoByLikeCount() {
        return scenicsMapper.getScenicsInfoByLikeCount();
    }

    @Override
    public List<Scenics> getRecommendedScenics() {
        return scenicsMapper.getRecommendedScenics();
    }

    @Override
    public boolean addViewCount(Integer id) {
        return scenicsMapper.addViewCount(id);
    }

    /**
     * 获取景点详细信息
     * @return
     */
    @Override
    public List<ScenicsDTO> getScenicsDetailInfoById(Integer id) {
        return scenicsMapper.getScenicsDetailInfoById(id);
    }
    
    /**
     * 更新景点的点赞数、收藏数和评论数
     * @param id 景点 ID
     */
    @Override
    public void updateLikeCountAndCollectCount(Integer id) {
        // 调用 Mapper 中的更新方法，更新数据库
        scenicsMapper.updateLikeCountAndCollectCount(id);
        
        // 更新 Redis 中的点赞数
        String likeRedisKey = "likes:1:" + id;
        Long likeCount = redisTemplate.opsForSet().size(likeRedisKey);
        if (likeCount == null) {
            likeCount = 0L;
        }
        redisTemplate.expire(likeRedisKey, 26, TimeUnit.DAYS);
        
        // 更新 Redis 中的收藏数
        String collectRedisKey = "collections:1:" + id;
        Long collectCount = redisTemplate.opsForSet().size(collectRedisKey);
        if (collectCount == null) {
            collectCount = 0L;
        }
        redisTemplate.expire(collectRedisKey, 26, TimeUnit.DAYS);
        
        log.info("更新景点 ID={} 的点赞数={}、收藏数={}", id, likeCount, collectCount);
    }
    /**
     * 多表关联分页查询
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @Override
    public IPage<ScenicsAndRegionDTO> selectScenicsAndRegionPage(Integer pageNum,Integer pageSize){
        //1、创建分页对象
        Page<ScenicsAndRegionDTO> page = new Page<>(pageNum,pageSize);
        //2、执行分页查询
        return scenicsMapper.selectScenicsAndRegionPage( page);
    }

    @Override
    public boolean addScenicsInfo(Scenics scenics) {
        return scenicsMapper.addScenics(scenics);
    }

    @Override
    public boolean deleteScenicsInfo(List<Integer> id) {
        return scenicsMapper.deleteScenics( id);
    }

    @Override
    public boolean updateScenicsInfo(Scenics scenics) {
        return scenicsMapper.updateScenics( scenics);
    }
}