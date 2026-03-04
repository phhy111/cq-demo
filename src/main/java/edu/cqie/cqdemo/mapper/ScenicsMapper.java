package edu.cqie.cqdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author
 * @description 针对表【scenics(景点表)】的数据库操作Mapper
 * @createDate 2026-01-30 19:29:27
 * @Entity generator.domain.Scenics
 */
@Mapper
public interface ScenicsMapper extends BaseMapper<Scenics> {
    /**
     * 获取主页轮播图信息
     * @return
     */
    public List<Scenics> getSlideShowInfo();

    /**
     * 获取对应区域的景点信息
     * @return
     */
    public List<Scenics> getScenicsInfoByRegionId(Integer regionId);

    /**
     * 获取不同等级的景点信息
     * @return
     */
    public List<Scenics> getScenicsInfoByLevel(String level);
    /**
     * 按评分高低获取景点信息
     * @return
     */
    public List<Scenics> getScenicsInfoByScore();
    /**
     * 按点赞量排序
     * @return
     */
    public List<Scenics> getScenicsInfoByLikeCount();
    /**
     * 增加浏览量
     * @return
     */
    public boolean addViewCount(Integer id);
    /**
     * 获取景点详情
     * @return
     */
    public List<ScenicsDTO> getScenicsDetailInfoById(Integer id);
    
    /**
     * 更新景点的点赞数、收藏数和评论数
     */
    public void updateLikeCountAndCollectCount();

}
