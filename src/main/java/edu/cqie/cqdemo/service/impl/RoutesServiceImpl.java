package edu.cqie.cqdemo.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Routes;
import edu.cqie.cqdemo.mapper.RoutesMapper;
import edu.cqie.cqdemo.service.RoutesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author
 * @description 针对表【routes(路线表)】的数据库操作Service实现
 * @createDate 2026-01-31 11:47:59
 */
@Service
public class RoutesServiceImpl extends ServiceImpl<RoutesMapper, Routes> implements RoutesService {
    private static final String LIKE_KEY_PREFIX = "routes:like:";


    @Autowired
    private RoutesMapper routesMapper;

    /**
     * 查询路线榜单前5
     * @return
     */
    @Override
    public List<Routes> getRoutesListInfo() {
        return routesMapper.getRoutesListInfo();
    }
    @Override
    public List<Routes> getRoutesListInfoTimeS()
    {
        return routesMapper.getRoutesListInfoByName();
    }
    @Override
    public List<Routes> getRoutesListInfoTimeJ()
    {
        return routesMapper.getRoutesListInfoByCode();
    }
    @Override
    public List<Routes>getAllHeatS()
    {
        return routesMapper.getAllHeatS();
    }
    @Override
    public List<Routes>getAllHeatJ()
    {
        return routesMapper.getAllHeatJ();
    }

    @Override
    public void updateLikeCountAndCollectCount() {
        routesMapper.updateLikeCountAndCollectCount();
    }

    @Override
    public Routes getRouteDetail(Integer id) {
        return routesMapper.getRouteDetail(id);
    }

    @Override
    public List<Routes> getAllRoutesWithAllStatus() {
        return routesMapper.getAllRoutesWithAllStatus();
    }

    @Override
    public boolean deleteById(Integer id) {
        // 调用 MyBatis-Plus 的 deleteById(Serializable id) 方法
        int affectedRows = routesMapper.deleteById(id);
        // 受影响行数 > 0 表示删除成功
        return affectedRows > 0;
    }

    @Override
    public List<Integer> selectPendingReviewCount() {
        return routesMapper.selectPendingReviewCount();
    }


}




