package edu.cqie.cqdemo.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.cqie.cqdemo.entity.Routes;
import org.apache.ibatis.annotations.Mapper;

import javax.swing.*;
import java.util.List;

/**
 * @author
 * @description 针对表【routes(路线表)】的数据库操作Mapper
 * @createDate 2026-01-31 11:47:59
 * @Entity cq-demo.Routes
 */
@Mapper
public interface RoutesMapper extends BaseMapper<Routes> {
    public List<Routes> getRoutesListInfo();

    //所有static为1的时间升序
    List<Routes> getRoutesListInfoByName();
    //所有static为1的时间降序
    List<Routes> getRoutesListInfoByCode();

    List<Routes> getAllHeatS();

    List<Routes> getAllHeatJ();

    void updateLikeCountAndCollectCount();

    Routes getRouteDetail(Integer id);

    //查询所有状态的路线数据
    List<Routes> getAllRoutesWithAllStatus();

    List<Integer> selectPendingReviewCount();
}




