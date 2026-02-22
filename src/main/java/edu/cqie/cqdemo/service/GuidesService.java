package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Guides;
import java.util.List;

public interface GuidesService extends IService<Guides> {
    /**
     * 根据路线ID获取相关攻略列表
     */
    List<Guides> getGuidesByRouteId(Integer routeId);
}
