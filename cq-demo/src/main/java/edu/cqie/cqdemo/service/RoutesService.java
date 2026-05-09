package edu.cqie.cqdemo.service;


import com.baomidou.mybatisplus.extension.service.IService;
import edu.cqie.cqdemo.entity.Routes;

import java.util.List;

/**
 * @author
 * @description 针对表【routes(路线表)】的数据库操作Service
 * @createDate 2026-01-31 11:47:59
 */
public interface RoutesService extends IService<Routes> {
    public List<Routes> getRoutesListInfo();

    //routes的所有status是1的升序
    List<Routes> getRoutesListInfoTimeS();
    //routes的所有status是1的降序
    List<Routes> getRoutesListInfoTimeJ();

    List<Routes> getAllHeatS();
    List<Routes> getAllHeatJ();

    // 更新点赞数和收藏数
    void updateLikeCountAndCollectCount();

    //查看特定id的路线信息
    Routes getRouteDetail(Integer id);

    //查询所有状态的路线数据
    List<Routes> getAllRoutesWithAllStatus();

    boolean deleteById(Integer id);
    List<Integer> selectPendingReviewCount();
}
