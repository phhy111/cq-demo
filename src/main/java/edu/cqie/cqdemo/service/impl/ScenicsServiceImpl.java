package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.dto.ScenicsDTO;
import edu.cqie.cqdemo.entity.Scenics;
import edu.cqie.cqdemo.mapper.ScenicsMapper;
import edu.cqie.cqdemo.service.ScenicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ScenicsServiceImpl extends ServiceImpl<ScenicsMapper, Scenics> implements ScenicsService {
    /**
     * 获取主页轮播图信息
     * @return
     */
    @Autowired
    private ScenicsMapper scenicsMapper;
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
}