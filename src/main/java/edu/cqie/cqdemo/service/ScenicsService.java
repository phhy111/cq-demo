package edu.cqie.cqdemo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Scenics;

import java.util.List;

/**
 * @author
 * @description 针对表【scenics(景点表)】的数据库操作Service
 * @createDate 2026-01-30 19:29:27
 */
public interface ScenicsService extends IService<Scenics> {
    public List<Scenics> getSlideShowInfo();
}
