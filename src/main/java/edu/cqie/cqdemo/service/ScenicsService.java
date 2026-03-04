package edu.cqie.cqdemo.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.dto.ScenicsAndRegionDTO;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * @author
 * @description 针对表【scenics(景点表)】的数据库操作Service
 * @createDate 2026-01-30 19:29:27
 */
public interface ScenicsService extends IService<Scenics> {
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
     * 添加景点浏览量
     * @param id
     */
    public boolean addViewCount(Integer id);
    /**
     * 获取景点详细信息
     * @param id
     */
    public List<ScenicsDTO> getScenicsDetailInfoById(Integer id);
    /**
     * 多表关联分页查询
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public IPage<ScenicsAndRegionDTO> selectScenicsAndRegionPage(Integer pageNum, Integer pageSize);
    /**
     * 新增景点信息
     * @param scenics
     */
    public boolean addScenicsInfo(Scenics scenics);
    /**
     * 批量删除景点信息
     * @param id
     */
    public boolean deleteScenicsInfo(List<Integer> id);
    /**
     * 修改景点信息
     * @param scenics
     */
    public boolean updateScenicsInfo(Scenics scenics);
}
