package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.mapper.GuidesMapper;
import edu.cqie.cqdemo.service.GuidesService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GuidesServiceImpl extends ServiceImpl<GuidesMapper, Guides> implements GuidesService {
    @Override
    public List<Guides> getGuidesByRouteId(Integer routeId) {
        // 使用 MyBatis-Plus 的条件查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Guides> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("routes_id", routeId);
        queryWrapper.eq("status", 1); // 只查询已发布的攻略
        queryWrapper.orderByDesc("created_at");

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<Guides> getAllGuidesWithAllStatus() {
        // 使用 MyBatis-Plus 的条件查询，不限制状态
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Guides> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.orderByDesc("created_at");

        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateLikeCountAndCollectCount() {
        // 调用 Mapper 中的更新方法
        baseMapper.updateLikeCountAndCollectCount();
    }
}
