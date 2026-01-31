package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
}