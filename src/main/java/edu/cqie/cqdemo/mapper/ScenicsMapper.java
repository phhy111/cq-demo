package edu.cqie.cqdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
     * 多表关联分页查询
     * @param page 分页对象（MP 会自动解析分页参数）
     * @return 分页结果
     */
    IPage<ScenicsAndRegionDTO> selectScenicsAndRegionPage(Page<ScenicsAndRegionDTO> page);
    /**
     * 新增景点
     * @return
     */
    boolean addScenics(Scenics scenics);
    /**
     * 批量删除景点
     * @return
     */
    boolean deleteScenics(List<Integer> id);
    /**
     * 修改景点信息
     * @return
     */
    boolean updateScenics(Scenics scenics);
}
