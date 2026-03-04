package edu.cqie.cqdemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Guides;
import java.util.List;

public interface GuidesService extends IService<Guides> {
    /**
     * 根据路线 ID 获取相关攻略列表
     */
    List<Guides> getGuidesByRouteId(Integer routeId);

    /**
     * 查询所有状态的攻略数据
     */
    List<Guides> getAllGuidesWithAllStatus();
    
    /**
     * 更新攻略的点赞数、收藏数和评论数
     */
    void updateLikeCountAndCollectCount();
}
